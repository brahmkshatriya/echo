package dev.brahmkshatriya.echo.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import dev.brahmkshatriya.echo.playback.FFTAudioProcessor
import java.lang.System.arraycopy
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The visualizer is a view which listens to the FFT changes and forwards it to the band view.
 * https://github.com/dzolnai/ExoVisualizer
 */
class ExoVisualizer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), FFTAudioProcessor.FFTListener {

    var processor: FFTAudioProcessor? = null
    private var currentWaveform: FloatArray? = null

    private val bandView = FFTBandView(context, attrs)

    init {
        addView(bandView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun updateProcessorListenerState(enable: Boolean) {
        if (enable) {
            processor?.addListener(this)
        } else {
            processor?.removeListener(this)
            currentWaveform = null
        }
    }

    fun setColors(color: Int) {
        bandView.setColor(color)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateProcessorListenerState(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updateProcessorListenerState(false)
    }

    override fun onFFTReady(sampleRateHz: Int, channelCount: Int, fft: FloatArray) {
        currentWaveform = fft
        bandView.onFFT(fft)
    }

    class FFTBandView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        companion object {
            // Taken from: https://en.wikipedia.org/wiki/Preferred_number#Audio_frequencies
//            private val FREQUENCY_BAND_LIMITS = arrayOf(
//                20, 25, 32, 40, 50, 63, 80, 100,
//                125, 160, 200, 250, 315, 400, 500, 630, 800, 1000,
//                1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
//                12500, 16000, 20000
//            )
            private val FREQUENCY_BAND_LIMITS = (0..31).map{
                (20 * 1.25.pow(it)).roundToInt().coerceAtMost(20000)
            }
        }

        private val bands = FREQUENCY_BAND_LIMITS.size
        private val size = FFTAudioProcessor.SAMPLE_SIZE / 2
        private val maxConst = 100000 // Reference max value for accum magnitude

        private val fft: FloatArray = FloatArray(size)
        private val paintPaths = listOf<Paint>(
//            Paint()
//            Paint(), Paint(), Paint()
        )
        private val paintBandsFill = Paint()

        // We average out the values over 3 occurences (plus the current one), so big jumps are smoothed out
        private val smoothingFactor = 8
        private val previousValues = FloatArray(bands * smoothingFactor)

        private val fftPath = paintPaths.map { Path() }

        private var startedAt: Long = 0

        init {
            paintBandsFill.style = Paint.Style.FILL
            paintPaths.forEach { paint ->
                paint.strokeWidth = 8f
                paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
            }
        }

        fun setColor(color: Int) {
            paintBandsFill.color = color
            paintPaths.forEachIndexed { index, paint ->
                paint.color = color
                paint.alpha = 250 * (index + 1) / paintPaths.size
            }
        }

        private fun drawAudio(canvas: Canvas) {
            // Clear the previous drawing on the screen
            canvas.drawColor(Color.TRANSPARENT)

            // Set up counters and widgets
            var currentFftPosition = 0
            var currentFrequencyBandLimitIndex = 0
            fftPath.forEach { it.reset() }
            fftPath.forEach { it.moveTo(0f, height.toFloat()) }

            // Iterate over the entire FFT result array
            while (currentFftPosition < size) {
                var accum = 0f

                // We divide the bands by frequency.
                // Check until which index we need to stop for the current band
                val nextLimitAtPosition =
                    floor(FREQUENCY_BAND_LIMITS[currentFrequencyBandLimitIndex] / 20_000.toFloat() * size).toInt()

                synchronized(fft) {
                    // Here we iterate within this single band
                    for (j in 0 until (nextLimitAtPosition - currentFftPosition) step 2) {
                        // Convert real and imaginary part to get energy
                        val raw = (fft[currentFftPosition + j].toDouble().pow(2.0) +
                                fft.getOrElse(currentFftPosition + j + 1) {
                                    fft.last()
                                }.toDouble().pow(2.0)).toFloat()

                        // Hamming window (by frequency band instead of frequency, otherwise it would prefer 10kHz, which is too high)
                        // The window mutes down the very high and the very low frequencies, usually not hearable by the human ear
                        val m = bands / 2
                        val windowed =
                            raw * (0.54f - 0.46f * cos(2 * Math.PI * currentFrequencyBandLimitIndex / (m + 1))).toFloat()
                        accum += windowed
                    }
                }
                // A window might be empty which would result in a 0 division
                if (nextLimitAtPosition - currentFftPosition != 0) {
                    accum /= (nextLimitAtPosition - currentFftPosition)
                } else {
                    accum = 0.0f
                }
                currentFftPosition = nextLimitAtPosition

                // Here we do the smoothing
                // If you increase the smoothing factor, the high shoots will be toned down, but the
                // 'movement' in general will decrease too
                var smoothedAccum = accum
                for (i in 0 until smoothingFactor) {
                    smoothedAccum += previousValues[i * bands + currentFrequencyBandLimitIndex]
                    if (i != smoothingFactor - 1) {
                        previousValues[i * bands + currentFrequencyBandLimitIndex] =
                            previousValues[(i + 1) * bands + currentFrequencyBandLimitIndex]
                    } else {
                        previousValues[i * bands + currentFrequencyBandLimitIndex] = accum
                    }
                }
                smoothedAccum /= (smoothingFactor + 1) // +1 because it also includes the current value

                val leftX = width * (currentFrequencyBandLimitIndex / bands.toFloat())
                val rightX = leftX + width / bands.toFloat()

                val fillBarHeight =
                    (height * (smoothedAccum / maxConst).coerceAtMost(1f))
                val fillTop = height - fillBarHeight
                canvas.drawRect(
                    leftX,
                    fillTop,
                    rightX,
                    height.toFloat(),
                    paintBandsFill
                )
                fftPath.forEachIndexed { index, path ->
                    val barHeight =
                        (height * (smoothedAccum / maxConst).coerceAtMost(1f))
                    val top = height - (barHeight * (index + 1) / paintPaths.size.toFloat())
                    if (top > 1) path.lineTo((leftX + rightX) / 2, top)
                }

                currentFrequencyBandLimitIndex++
            }

            paintPaths.forEachIndexed { index, paintPath ->
                canvas.drawPath(fftPath[index], paintPath)
            }
        }

        fun onFFT(fft: FloatArray) {
            synchronized(this.fft) {
                if (startedAt == 0L) {
                    startedAt = System.currentTimeMillis()
                }
                // The resulting graph is mirrored, because we are using real numbers instead of imaginary
                // Explanations: https://www.mathworks.com/matlabcentral/answers/338408-why-are-fft-diagrams-mirrored
                // https://dsp.stackexchange.com/questions/4825/why-is-the-fft-mirrored/4827#4827
                // So what we do here, we only check the left part of the graph.
                arraycopy(fft, 2, this.fft, 0, size)
                // By calling invalidate, we request a redraw.
                invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawAudio(canvas)
            // By calling invalidate, we request a redraw. See https://github.com/dzolnai/ExoVisualizer/issues/2
            invalidate()
        }
    }
}