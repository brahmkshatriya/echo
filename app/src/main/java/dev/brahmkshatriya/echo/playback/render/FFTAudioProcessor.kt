package dev.brahmkshatriya.echo.playback.render

import android.media.AudioTrack
import android.media.AudioTrack.ERROR_BAD_VALUE
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.paramsen.noise.Noise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * An audio processor which forwards the input to the output,
 * but also takes the input and executes a Fast-Fourier Transformation (FFT) on it.
 * The results of this transformation is a 'list' of frequencies with their amplitudes,
 * which will be forwarded to the listener
 *
 * https://github.com/dzolnai/ExoVisualizer
 */

@OptIn(UnstableApi::class)
class FFTAudioProcessor : AudioProcessor {

    companion object {
        const val SAMPLE_SIZE = 8192

        // From DefaultAudioSink.java:160 'MIN_BUFFER_DURATION_US'
        private const val EXO_MIN_BUFFER_DURATION_US: Long = 250000

        // From DefaultAudioSink.java:164 'MAX_BUFFER_DURATION_US'
        private const val EXO_MAX_BUFFER_DURATION_US: Long = 750000

        // From DefaultAudioSink.java:173 'BUFFER_MULTIPLICATION_FACTOR'
        private const val EXO_BUFFER_MULTIPLICATION_FACTOR = 4

        // Extra size next in addition to the AudioTrack buffer size
        private const val BUFFER_EXTRA_SIZE = SAMPLE_SIZE * 8
    }

    private var noise: Noise? = null

    private var isActive: Boolean = false

    private var processBuffer: ByteBuffer
    private var fftBuffer: ByteBuffer
    private var outputBuffer: ByteBuffer

    private val listeners = mutableListOf<FFTListener>()
    private var inputEnded: Boolean = false

    private lateinit var srcBuffer: ByteBuffer
    private var srcBufferPosition = 0
    private val tempByteArray = ByteArray(SAMPLE_SIZE * 2)

    private var audioTrackBufferSize = 0

    private val src = FloatArray(SAMPLE_SIZE)
    private val dst = FloatArray(SAMPLE_SIZE + 2)

    interface FFTListener {
        fun onFFTReady(sampleRateHz: Int, channelCount: Int, fft: FloatArray)
    }

    fun addListener(listener: FFTListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: FFTListener) {
        listeners.remove(listener)
    }

    init {
        processBuffer = AudioProcessor.EMPTY_BUFFER
        fftBuffer = AudioProcessor.EMPTY_BUFFER
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    /**
     * The following method matches the implementation of getDefaultBufferSize in DefaultAudioSink
     * of ExoPlayer.
     * Because there is an AudioTrack buffer between the processor and the sound output, the processor receives everything early.
     * By putting the audio data to process in a buffer which has the same size as the audiotrack buffer,
     * we will delay ourselves to match the audio output.
     */
    private fun getDefaultBufferSizeInBytes(audioFormat: AudioProcessor.AudioFormat): Int {
        val outputPcmFrameSize =
            Util.getPcmFrameSize(audioFormat.encoding, audioFormat.channelCount)
        val minBufferSize =
            AudioTrack.getMinBufferSize(
                audioFormat.sampleRate,
                Util.getAudioTrackChannelConfig(audioFormat.channelCount),
                audioFormat.encoding
            )
        Assertions.checkState(minBufferSize != ERROR_BAD_VALUE)
        val multipliedBufferSize = minBufferSize * EXO_BUFFER_MULTIPLICATION_FACTOR
        val minAppBufferSize =
            durationUsToFrames(EXO_MIN_BUFFER_DURATION_US).toInt() * outputPcmFrameSize
        val maxAppBufferSize = max(
            minBufferSize.toLong(),
            durationUsToFrames(EXO_MAX_BUFFER_DURATION_US) * outputPcmFrameSize
        ).toInt()
        val bufferSizeInFrames = Util.constrainValue(
            multipliedBufferSize,
            minAppBufferSize,
            maxAppBufferSize
        ) / outputPcmFrameSize
        return bufferSizeInFrames * outputPcmFrameSize
    }

    private fun durationUsToFrames(durationUs: Long): Long {
        return durationUs * inputAudioFormat.sampleRate / C.MICROS_PER_SECOND
    }

    override fun isActive(): Boolean {
        return isActive
    }

    private lateinit var inputAudioFormat: AudioProcessor.AudioFormat

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(
                inputAudioFormat
            )
        }
        this.inputAudioFormat = inputAudioFormat
        isActive = true

        noise = Noise.real(SAMPLE_SIZE)

        audioTrackBufferSize = getDefaultBufferSizeInBytes(inputAudioFormat)

        srcBuffer = ByteBuffer.allocate(audioTrackBufferSize + BUFFER_EXTRA_SIZE)

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        var position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val frameCount = (limit - position) / (2 * inputAudioFormat.channelCount)
        val singleChannelOutputSize = frameCount * 2
        val outputSize = frameCount * inputAudioFormat.channelCount * 2


        if (processBuffer.capacity() < outputSize) {
            processBuffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
        } else {
            processBuffer.clear()
        }

        if (fftBuffer.capacity() < singleChannelOutputSize) {
            fftBuffer =
                ByteBuffer.allocateDirect(singleChannelOutputSize).order(ByteOrder.nativeOrder())
        } else {
            fftBuffer.clear()
        }

        while (position < limit) {
            var summedUp = 0
            for (channelIndex in 0 until inputAudioFormat.channelCount) {
                val current = inputBuffer.getShort(position + 2 * channelIndex)
                processBuffer.putShort(current)
                summedUp += current
            }
            // For the FFT, we use an currentAverage of all the channels
            fftBuffer.putShort((summedUp / inputAudioFormat.channelCount).toShort())
            position += inputAudioFormat.channelCount * 2
        }

        inputBuffer.position(limit)

        processFFT(this.fftBuffer)

        processBuffer.flip()
        outputBuffer = this.processBuffer
    }

    private fun processFFT(buffer: ByteBuffer) {
        scope.launch {
            //Need to additional delay because exoplayer sends data before silence skipping
            //if this processor is added after silence skipping,
            //it gives weird artifacts in the audio
            delay(5000)
            if (listeners.isEmpty()) return@launch

            srcBuffer.put(buffer.array())
            srcBufferPosition += buffer.array().size
            // Since this is PCM 16 bit, each sample will be 2 bytes.
            // So to get the sample size in the end, we need to take twice as many bytes off the buffer
            val bytesToProcess = SAMPLE_SIZE * 2
            var currentByte: Byte? = null
            while (srcBufferPosition > audioTrackBufferSize) {
                srcBuffer.position(0)
                srcBuffer.get(tempByteArray, 0, bytesToProcess)

                tempByteArray.forEachIndexed { index, byte ->
                    if (currentByte == null) {
                        currentByte = byte
                    } else {
                        src[index / 2] =
                            (currentByte!!.toFloat() * Byte.MAX_VALUE + byte) / (Byte.MAX_VALUE * Byte.MAX_VALUE)
                        dst[index / 2] = 0f
                        currentByte = null
                    }

                }
                srcBuffer.position(bytesToProcess)
                srcBuffer.compact()
                srcBufferPosition -= bytesToProcess
                runCatching {
                    srcBuffer.position(srcBufferPosition)
                    val fft = noise?.fft(src, dst)!!

                    listeners.forEach {
                        it.onFFTReady(
                            inputAudioFormat.sampleRate,
                            inputAudioFormat.channelCount,
                            fft
                        )
                    }
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun queueEndOfStream() {
        inputEnded = true
        processBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun getOutput(): ByteBuffer {
        val outputBuffer = this.outputBuffer
        this.outputBuffer = AudioProcessor.EMPTY_BUFFER
        return outputBuffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && processBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        // A new stream is incoming.
    }

    override fun reset() {
        flush()
        processBuffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat =
            AudioProcessor.AudioFormat(Format.NO_VALUE, Format.NO_VALUE, Format.NO_VALUE)
    }
}