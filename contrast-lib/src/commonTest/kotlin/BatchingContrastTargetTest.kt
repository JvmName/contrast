import dev.jvmname.contrast.BatchingContrastTarget
import dev.jvmname.contrast.ContrastTarget
import io.mockk.mockk
import io.mockk.verifyAll
import kotlin.test.BeforeTest
import kotlin.test.Test

class BatchingContrastTargetTest {
    private lateinit var mCallback: ContrastTarget
    private lateinit var mBatching: BatchingContrastTarget

    @BeforeTest
    fun before() {
        mCallback = mockk()
        mBatching = BatchingContrastTarget(mCallback)
    }

    @Test
    fun addSimple() {
        mBatching.onInserted(3, 2)
        dispatchLast()
        verifyAll { mCallback.onInserted(3, 2) }
    }

    @Test
    fun addToSamePos() {
        mBatching.onInserted(3, 2)
        mBatching.onInserted(3, 1)
        dispatchLast()
        verifyAll { mCallback.onInserted(3, 3) }
    }

    @Test
    fun addInsidePrevious() {
        mBatching.onInserted(3, 5)
        mBatching.onInserted(5, 1)
        dispatchLast()
        verifyAll { mCallback.onInserted(3, 6) }
    }

    @Test
    fun addBefore() {
        mBatching.onInserted(3, 5)
        mBatching.onInserted(2, 1)
        dispatchLast()
        verifyAll {
            mCallback.onInserted(3, 5)
            mCallback.onInserted(2, 1)
        }
    }

    @Test
    fun removeSimple() {
        mBatching.onRemoved(3, 2)
        dispatchLast()
        verifyAll { mCallback.onRemoved(3, 2) }
    }

    @Test
    fun removeSamePosition() {
        mBatching.onRemoved(3, 2)
        mBatching.onRemoved(3, 1)
        dispatchLast()
        verifyAll { mCallback.onRemoved(3, 3) }
    }

    @Test
    fun removeInside() {
        mBatching.onRemoved(3, 5)
        mBatching.onRemoved(4, 2)
        dispatchLast()
        verifyAll {
            mCallback.onRemoved(3, 5)
            mCallback.onRemoved(4, 2)
        }
    }

    @Test
    fun removeBefore() {
        mBatching.onRemoved(3, 2)
        mBatching.onRemoved(2, 1)
        dispatchLast()
        verifyAll { mCallback.onRemoved(2, 3) }
    }

    @Test
    fun removeBefore2() {
        mBatching.onRemoved(3, 2)
        mBatching.onRemoved(2, 4)
        dispatchLast()
        verifyAll { mCallback.onRemoved(2, 6) }
    }

    @Test
    fun removeBefore3() {
        mBatching.onRemoved(3, 2)
        mBatching.onRemoved(1, 1)
        dispatchLast()
        verifyAll {
            mCallback.onRemoved(3, 2)
            mCallback.onRemoved(1, 1)
        }
    }

    @Test
    fun moveSimple() {
        mBatching.onMoved(3, 2)
        dispatchLast()
        verifyAll { mCallback.onMoved(3, 2) }
    }

    @Test
    fun moveTwice() {
        mBatching.onMoved(3, 2)
        mBatching.onMoved(5, 6)
        dispatchLast()
        verifyAll {
            mCallback.onMoved(3, 2)
            mCallback.onMoved(5, 6)
        }
    }

    @Test
    fun changeSimple() {
        mBatching.onChanged(3, 2, null)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 2, null) }
    }

    @Test
    fun changeConsecutive() {
        mBatching.onChanged(3, 2, null)
        mBatching.onChanged(5, 2, null)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 4, null) }
    }

    @Test
    fun changeTheSame() {
        mBatching.onChanged(3, 2, null)
        mBatching.onChanged(4, 2, null)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 3, null) }
    }

    @Test
    fun changeTheSame2() {
        mBatching.onChanged(3, 2, null)
        mBatching.onChanged(3, 2, null)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 2, null) }
    }

    @Test
    fun changeBefore() {
        mBatching.onChanged(3, 2, null)
        mBatching.onChanged(2, 1, null)
        dispatchLast()
        verifyAll { mCallback.onChanged(2, 3, null) }
    }

    @Test
    fun changeBeforeOverlap() {
        mBatching.onChanged(3, 2, null)
        mBatching.onChanged(2, 2, null)
        dispatchLast()
        verifyAll { mCallback.onChanged(2, 3, null) }
    }

    @Test
    fun changeSimpleWithPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 2, payload) }
    }

    @Test
    fun changeConsecutiveWithPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        mBatching.onChanged(5, 2, payload)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 4, payload) }
    }

    @Test
    fun changeTheSameWithPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        mBatching.onChanged(4, 2, payload)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 3, payload) }
    }

    @Test
    fun changeTheSame2WithPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        mBatching.onChanged(3, 2, payload)
        dispatchLast()
        verifyAll { mCallback.onChanged(3, 2, payload) }
    }

    @Test
    fun changeBeforeWithPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        mBatching.onChanged(2, 1, payload)
        dispatchLast()
        verifyAll { mCallback.onChanged(2, 3, payload) }
    }

    @Test
    fun changeBeforeOverlapWithPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        mBatching.onChanged(2, 2, payload)
        dispatchLast()
        verifyAll { mCallback.onChanged(2, 3, payload) }
    }

    @Test
    fun changeWithNewPayload() {
        val payload1 = Any()
        val payload2 = Any()
        mBatching.onChanged(3, 2, payload1)
        mBatching.onChanged(2, 2, payload2)
        dispatchLast()
        verifyAll {
            mCallback.onChanged(3, 2, payload1)
            mCallback.onChanged(2, 2, payload2)
        }
    }

    @Test
    fun changeWithEmptyPayload() {
        val payload = Any()
        mBatching.onChanged(3, 2, payload)
        mBatching.onChanged(2, 2, null)
        dispatchLast()
        verifyAll {
            mCallback.onChanged(3, 2, payload)
            mCallback.onChanged(2, 2, null)
        }
    }

    @Test
    fun changeWithEmptyPayload2() {
        val payload = Any()
        mBatching.onChanged(3, 2, null)
        mBatching.onChanged(2, 2, payload)
        dispatchLast()
        verifyAll {
            mCallback.onChanged(3, 2, null)
            mCallback.onChanged(2, 2, payload)
        }
    }

    private fun dispatchLast() {
        mBatching.dispatchEvent()
        mBatching.notifyCompleted()
    }
}