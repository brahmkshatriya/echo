package dev.brahmkshatriya.echo.utils

import java.util.concurrent.Semaphore
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class AdjustableSemaphore {
    /**
     * semaphore starts at 0 capacity; must be set by setMaxPermits before use
     */
    private val semaphore = ResizeableSemaphore()

    /**
     * how many permits are allowed as governed by this semaphore.
     * Access must be synchronized on this object.
     */
    private var maxPermits = 0

    /**
     * Set the max number of permits. Must be greater than zero.
     *
     * Note that if there are more than the new max number of permits currently
     * outstanding, any currently blocking threads or any new threads that start
     * to block after the call will wait until enough permits have been released to
     * have the number of outstanding permits fall below the new maximum. In
     * other words, it does what you probably think it should.
     *
     * @param newMax
     */
    @Synchronized
    fun setMaxPermits(newMax: Int) {
        require(newMax >= 1) {
            ("Semaphore size must be at least 1, was $newMax")
        }

        var delta = newMax - this.maxPermits
        if (delta == 0) return

        if (delta > 0) {
            // new max is higher, so release that many permits
            semaphore.release(delta)
        } else {
            delta *= -1
            // delta < 0.
            // reducePermits needs a positive #, though.
            semaphore.reducePermits(delta)
        }

        this.maxPermits = newMax
    }

    /**
     * Release a permit back to the semaphore. Make sure not to double-release.
     *
     */
    fun release() {
        semaphore.release()
    }

    /**
     * Get a permit, blocking if necessary.
     *
     * @throws InterruptedException
     * if interrupted while waiting for a permit
     */
    @Throws(InterruptedException::class)
    fun acquire() {
        semaphore.acquire()
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> withPermit(block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        acquire()
        return try {
            block()
        } finally {
            release()
        }
    }

    /**
     * A trivial subclass of `Semaphore` that exposes the reducePermits
     * call to the parent class. Doug Lea says it's ok...
     * http://osdir.com/ml/java.jsr.166-concurrency/2003-10/msg00042.html
     */
    private class ResizeableSemaphore : Semaphore(0) {
        public override fun reducePermits(reduction: Int) {
            super.reducePermits(reduction)
        }
    }
}