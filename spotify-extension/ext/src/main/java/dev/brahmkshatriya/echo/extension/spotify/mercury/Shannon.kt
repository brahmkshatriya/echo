package dev.brahmkshatriya.echo.extension.spotify.mercury

class Shannon {
    private val R: IntArray /* Working storage for the shift register. */
    private val CRC: IntArray /* Working storage for CRC accumulation. */
    private val initR: IntArray /* Saved register contents. */
    private var konst = 0 /* Key dependant semi-constant. */
    private var sbuf = 0 /* Encryption buffer. */
    private var mbuf = 0 /* Partial word MAC buffer. */
    private var nbuf = 0 /* Number of part-word stream bits buffered. */

    /**
     * Create a new instance of the Shannon stream-cipher.
     */
    init {
        /* Registers with length N. */
        this.R = IntArray(N)
        this.CRC = IntArray(N)
        this.initR = IntArray(N)
    }

    /* Nonlinear transform (sbox) of a word. There are two slightly different combinations. */
    private fun sbox(i: Int): Int {
        var i = i
        i = i xor (Integer.rotateLeft(i, 5) or Integer.rotateLeft(i, 7))
        i = i xor (Integer.rotateLeft(i, 19) or Integer.rotateLeft(i, 22))

        return i
    }

    private fun sbox2(i: Int): Int {
        var i = i
        i = i xor (Integer.rotateLeft(i, 7) or Integer.rotateLeft(i, 22))
        i = i xor (Integer.rotateLeft(i, 5) or Integer.rotateLeft(i, 19))

        return i
    }

    /* Cycle the contents of the register and calculate output word in sbuf. */
    private fun cycle() {
        /* Nonlinear feedback function. */
        /* Temporary variable. */
        var t = R[12] xor R[13] xor this.konst
        t = this.sbox(t) xor Integer.rotateLeft(R[0], 1)

        /* Shift register. */
        for (i in 1..<N) {
            R[i - 1] = R[i]
        }

        R[N - 1] = t

        t = sbox2(R[2] xor R[15])
        R[0] = R[0] xor t
        this.sbuf = t xor R[8] xor R[12]
    }

    /*
     * The Shannon MAC function is modelled after the concepts of Phelix and SHA.
     * Basically, words to be accumulated in the MAC are incorporated in two
     * different ways:
     * 1. They are incorporated into the stream cipher register at a place
     *    where they will immediately have a nonlinear effect on the state.
     * 2. They are incorporated into bit-parallel CRC-16 registers; the
     *    contents of these registers will be used in MAC finalization.
     */
    /*
     * Accumulate a CRC of input words, later to be fed into MAC.
     * This is actually 32 parallel CRC-16s, using the IBM CRC-16
     * polynomian x^16 + x^15 + x^2 + 1
     */
    private fun crcFunc(i: Int) {
        /* Accumulate CRC of input. */
        /* Temporary variable. */
        val t = CRC[0] xor CRC[2] xor CRC[15] xor i

        for (j in 1..<N) {
            CRC[j - 1] = CRC[j]
        }

        CRC[N - 1] = t
    }

    /* Normal MAC word processing: do both stream register and CRC. */
    private fun macFunc(i: Int) {
        this.crcFunc(i)

        R[KEYP] =
            R[KEYP] xor i
    }

    /* Initialize to known state. */
    private fun initState() {
        /* Register initialized to Fibonacci numbers. */
        R[0] = 1
        R[1] = 1

        for (i in 2..<N) {
            R[i] = R[i - 1] + R[i - 2]
        }

        /* Initialization constant. */
        this.konst = INITKONST
    }

    /* Save the current register state. */
    private fun saveState() {
        for (i in 0..<N) {
            initR[i] = R[i]
        }
    }

    /* Inisialize to previously saved register state. */
    private fun reloadState() {
        for (i in 0..<N) {
            R[i] = initR[i]
        }
    }

    /* Initialize 'konst'. */
    private fun genKonst() {
        this.konst = R[0]
    }

    /* Load key material into the register. */
    private fun addKey(k: Int) {
        R[KEYP] =
            R[KEYP] xor k
    }

    /* Extra nonlinear diffusion of register for key and MAC. */
    private fun diffuse() {
        for (i in 0..<FOLD) {
            this.cycle()
        }
    }

    /*
     * Common actions for loading key material.
     * Allow non-word-multiple key and nonce material.
     * Note: Also initializes the CRC register as a side effect.
     */
    private fun loadKey(key: ByteArray) {
        val extra = ByteArray(4)
        var j: Int
        var t: Int

        /* Start folding key. */
        var i = 0
        while (i < (key.size and 0x03.inv())) {
            /* Shift 4 bytes into one word. */
            t = ((key[i + 3].toInt() and 0xFF) shl 24) or
                    ((key[i + 2].toInt() and 0xFF) shl 16) or
                    ((key[i + 1].toInt() and 0xFF) shl 8) or
                    ((key[i].toInt() and 0xFF))

            /* Insert key word at index 13. */
            this.addKey(t)

            /* Cycle register. */
            this.cycle()
            i += 4
        }

        /* If there were any extra bytes, zero pad to a word. */
        if (i < key.size) {
            /* i remains unchanged at start of loop. */
            j = 0
            while (i < key.size) {
                extra[j++] = key[i]
                i++
            }

            /* j remains unchanged at start of loop. */
            while (j < 4) {
                extra[j] = 0
                j++
            }

            /* Shift 4 extra bytes into one word. */
            t = ((extra[3].toInt() and 0xFF) shl 24) or
                    ((extra[2].toInt() and 0xFF) shl 16) or
                    ((extra[1].toInt() and 0xFF) shl 8) or
                    ((extra[0].toInt() and 0xFF))

            /* Insert key word at index 13. */
            this.addKey(t)

            /* Cycle register. */
            this.cycle()
        }

        /* Also fold in the length of the key. */
        this.addKey(key.size)

        /* Cycle register. */
        this.cycle()

        /* Save a copy of the register. */
        i = 0
        while (i < N) {
            CRC[i] = R[i]
            i++
        }

        /* Now diffuse. */
        this.diffuse()

        /* Now XOR the copy back -- makes key loading irreversible. */
        i = 0
        while (i < N) {
            R[i] = R[i] xor CRC[i]
            i++
        }
    }

    /* Set key */
    fun key(key: ByteArray) {
        /* Initializet known state. */
        this.initState()

        /* Load key material. */
        this.loadKey(key)

        /* In case we proceed to stream generation. */
        this.genKonst()

        /* Save register state. */
        this.saveState()

        /* Set 'nbuf' value to zero. */
        this.nbuf = 0
    }

    /* Set IV */
    fun nonce(nonce: ByteArray) {
        /* Reload register state. */
        this.reloadState()

        /* Set initialization constant. */
        this.konst = INITKONST

        /* Load "IV" material. */
        this.loadKey(nonce)

        /* Set 'konst'. */
        this.genKonst()

        /* Set 'nbuf' value to zero. */
        this.nbuf = 0
    }

    /*
     * XOR pseudo-random bytes into buffer.
     * Note: doesn't play well with MAC functions.
     */
    fun stream(buffer: ByteArray) {
        var i = 0
        val j: Int
        var n = buffer.size

        /* Handle any previously buffered bytes. */
        while (this.nbuf != 0 && n != 0) {
            buffer[i++] = (buffer[i++].toInt() xor (this.sbuf and 0xFF)).toByte()

            this.sbuf = this.sbuf shr 8
            this.nbuf -= 8

            n--
        }

        /* Handle whole words. */
        j = n and 0x03.inv()

        while (i < j) {
            /* Cycle register. */
            this.cycle()

            /* XOR word. */
            buffer[i + 3] = (buffer[i + 3].toInt() xor ((this.sbuf shr 24) and 0xFF)).toByte()
            buffer[i + 2] = (buffer[i + 2].toInt() xor ((this.sbuf shr 16) and 0xFF)).toByte()
            buffer[i + 1] = (buffer[i + 1].toInt() xor ((this.sbuf shr 8) and 0xFF)).toByte()
            buffer[i] = (buffer[i].toInt() xor ((this.sbuf) and 0xFF)).toByte()

            i += 4
        }

        /* Handle any trailing bytes. */
        n = n and 0x03

        if (n != 0) {
            /* Cycle register. */
            this.cycle()

            this.nbuf = 32

            while (this.nbuf != 0 && n != 0) {
                buffer[i++] = (buffer[i++].toInt() xor (this.sbuf and 0xFF)).toByte()

                this.sbuf = this.sbuf shr 8
                this.nbuf -= 8

                n--
            }
        }
    }

    /*
     * Accumulate words into MAC without encryption.
     * Note that plaintext is accumulated for MAC.
     */
    fun macOnly(buffer: ByteArray) {
        var i = 0
        val j: Int
        var n = buffer.size
        var t: Int

        /* Handle any previously buffered bytes. */
        if (this.nbuf != 0) {
            while (this.nbuf != 0 && n != 0) {
                this.mbuf = this.mbuf xor (buffer[i++].toInt() shl (32 - this.nbuf))
                this.nbuf -= 8

                n--
            }

            /* Not a whole word yet. */
            if (this.nbuf != 0) {
                return
            }

            /* LFSR already cycled. */
            this.macFunc(this.mbuf)
        }

        /* Handle whole words. */
        j = n and 0x03.inv()

        while (i < j) {
            /* Cycle register. */
            this.cycle()

            /* Shift 4 bytes into one word. */
            t = ((buffer[i + 3].toInt() and 0xFF) shl 24) or
                    ((buffer[i + 2].toInt() and 0xFF) shl 16) or
                    ((buffer[i + 1].toInt() and 0xFF) shl 8) or
                    ((buffer[i].toInt() and 0xFF))

            this.macFunc(t)

            i += 4
        }

        /* Handle any trailing bytes. */
        n = n and 0x03

        if (n != 0) {
            /* Cycle register. */
            this.cycle()

            this.mbuf = 0
            this.nbuf = 32

            while (this.nbuf != 0 && n != 0) {
                this.mbuf = this.mbuf xor (buffer[i++].toInt() shl (32 - this.nbuf))
                this.nbuf -= 8

                n--
            }
        }
    }

    /*
     * Combined MAC and encryption.
     * Note that plaintext is accumulated for MAC.
     */
    /*
     * Combined MAC and encryption.
     * Note that plaintext is accumulated for MAC.
     */
    @JvmOverloads
    fun encrypt(buffer: ByteArray, n: Int = buffer.size) {
        var n = n
        var i = 0
        var t: Int

        /* Handle any previously buffered bytes. */
        if (this.nbuf != 0) {
            while (this.nbuf != 0 && n != 0) {
                this.mbuf = this.mbuf xor ((buffer[i].toInt() and 0xFF) shl (32 - this.nbuf))
                buffer[i] =
                    (buffer[i].toInt() xor ((this.sbuf shr (32 - this.nbuf)) and 0xFF)).toByte()

                i++

                this.nbuf -= 8

                n--
            }

            /* Not a whole word yet. */
            if (this.nbuf != 0) {
                return
            }

            /* LFSR already cycled. */
            this.macFunc(this.mbuf)
        }

        /* Handle whole words. */
        val j = n and 0x03.inv()

        while (i < j) {
            /* Cycle register. */
            this.cycle()

            /* Shift 4 bytes into one word. */
            t = ((buffer[i + 3].toInt() and 0xFF) shl 24) or
                    ((buffer[i + 2].toInt() and 0xFF) shl 16) or
                    ((buffer[i + 1].toInt() and 0xFF) shl 8) or
                    ((buffer[i].toInt() and 0xFF))

            this.macFunc(t)

            t = t xor this.sbuf

            /* Put word into byte buffer. */
            buffer[i + 3] = ((t shr 24) and 0xFF).toByte()
            buffer[i + 2] = ((t shr 16) and 0xFF).toByte()
            buffer[i + 1] = ((t shr 8) and 0xFF).toByte()
            buffer[i] = ((t) and 0xFF).toByte()

            i += 4
        }

        /* Handle any trailing bytes. */
        n = n and 0x03

        if (n != 0) {
            /* Cycle register. */
            this.cycle()

            this.mbuf = 0
            this.nbuf = 32

            while (this.nbuf != 0 && n != 0) {
                this.mbuf = this.mbuf xor ((buffer[i].toInt() and 0xFF) shl (32 - this.nbuf))
                buffer[i] =
                    (buffer[i].toInt() xor ((this.sbuf shr (32 - this.nbuf)) and 0xFF)).toByte()

                i++

                this.nbuf -= 8

                n--
            }
        }
    }

    /*
     * Combined MAC and decryption.
     * Note that plaintext is accumulated for MAC.
     */
    /*
     * Combined MAC and decryption.
     * Note that plaintext is accumulated for MAC.
     */
    @JvmOverloads
    fun decrypt(buffer: ByteArray, n: Int = buffer.size) {
        var n = n
        var i = 0
        var t: Int

        /* Handle any previously buffered bytes. */
        if (this.nbuf != 0) {
            while (this.nbuf != 0 && n != 0) {
                buffer[i] =
                    (buffer[i].toInt() xor ((this.sbuf shr (32 - this.nbuf)) and 0xFF)).toByte()
                this.mbuf = this.mbuf xor ((buffer[i].toInt() and 0xFF) shl (32 - this.nbuf))

                i++

                this.nbuf -= 8

                n--
            }

            /* Not a whole word yet. */
            if (this.nbuf != 0) {
                return
            }

            /* LFSR already cycled. */
            this.macFunc(this.mbuf)
        }

        /* Handle whole words. */
        val j = n and 0x03.inv()

        while (i < j) {
            /* Cycle register. */
            this.cycle()

            /* Shift 4 bytes into one word. */
            t = ((buffer[i + 3].toInt() and 0xFF) shl 24) or
                    ((buffer[i + 2].toInt() and 0xFF) shl 16) or
                    ((buffer[i + 1].toInt() and 0xFF) shl 8) or
                    ((buffer[i].toInt() and 0xFF))

            t = t xor this.sbuf

            this.macFunc(t)

            /* Put word into byte buffer. */
            buffer[i + 3] = ((t shr 24) and 0xFF).toByte()
            buffer[i + 2] = ((t shr 16) and 0xFF).toByte()
            buffer[i + 1] = ((t shr 8) and 0xFF).toByte()
            buffer[i] = ((t) and 0xFF).toByte()

            i += 4
        }

        /* Handle any trailing bytes. */
        n = n and 0x03

        if (n != 0) {
            /* Cycle register. */
            this.cycle()

            this.mbuf = 0
            this.nbuf = 32

            while (this.nbuf != 0 && n != 0) {
                buffer[i] =
                    (buffer[i].toInt() xor ((this.sbuf shr (32 - this.nbuf)) and 0xFF)).toByte()
                this.mbuf = this.mbuf xor ((buffer[i].toInt() and 0xFF) shl (32 - this.nbuf))

                i++

                this.nbuf -= 8

                n--
            }
        }
    }

    /*
     * Having accumulated a MAC, finish processing and return it.
     * Note that any unprocessed bytes are treated as if they were
     * encrypted zero bytes, so plaintext (zero) is accumulated.
     */
    /*
     * Having accumulated a MAC, finish processing and return it.
     * Note that any unprocessed bytes are treated as if they were
     * encrypted zero bytes, so plaintext (zero) is accumulated.
     */
    @JvmOverloads
    fun finish(buffer: ByteArray, n: Int = buffer.size) {
        var n = n
        var i = 0
        var j: Int

        /* Handle any previously buffered bytes. */
        if (this.nbuf != 0) {
            /* LFSR already cycled. */
            this.macFunc(this.mbuf)
        }

        /*
         * Perturb the MAC to mark end of input.
         * Note that only the stream register is updated, not the CRC.
         * This is an action that can't be duplicated by passing in plaintext,
         * hence defeating any kind of extension attack.
         */
        this.cycle()
        this.addKey(INITKONST xor (this.nbuf shl 3))

        this.nbuf = 0

        /* Now add the CRC to the stream register and diffuse it. */
        j = 0
        while (j < N) {
            R[j] = R[j] xor CRC[j]
            j++
        }

        this.diffuse()

        /* Produce output from the stream buffer. */
        while (n > 0) {
            this.cycle()

            if (n >= 4) {
                /* Put word into byte buffer. */
                buffer[i + 3] = ((this.sbuf shr 24) and 0xFF).toByte()
                buffer[i + 2] = ((this.sbuf shr 16) and 0xFF).toByte()
                buffer[i + 1] = ((this.sbuf shr 8) and 0xFF).toByte()
                buffer[i] = ((this.sbuf) and 0xFF).toByte()

                n -= 4
                i += 4
            } else {
                j = 0
                while (j < n) {
                    buffer[i + j] = ((this.sbuf shr (i * 8)) and 0xFF).toByte()
                    j++
                }

                break
            }
        }
    }

    companion object {
        /*
     * Fold is how many register cycles need to be performed after combining the
     * last byte of key and non-linear feedback, before every byte depends on every
     * byte of the key. This depends on the feedback and nonlinear functions, and
     * on where they are combined into the register. Making it same as the register
     * length is a safe and conservative choice.
     */
        private const val N = 16
        private const val FOLD = N /* How many iterations of folding to do. */
        private const val INITKONST = 0x6996c53a /* Value of konst to use during key loading. */
        private const val KEYP = 13 /* Where to insert key/MAC/counter words. */
    }
}
