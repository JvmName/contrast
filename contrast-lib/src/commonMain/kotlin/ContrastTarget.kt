package dev.jvmname.contrast

import dev.jvmname.contrast.BatchingContrastTarget.Op.ADD
import dev.jvmname.contrast.BatchingContrastTarget.Op.CHANGE
import dev.jvmname.contrast.BatchingContrastTarget.Op.NONE
import dev.jvmname.contrast.BatchingContrastTarget.Op.REMOVE
import kotlin.math.max
import kotlin.math.min

interface ContrastTarget {
    fun onInserted(position: Int, count: Int)
    fun onRemoved(position: Int, count: Int)
    fun onMoved(fromPosition: Int, toPosition: Int)
    fun onChanged(position: Int, count: Int, payload: Any?)
}

open class BatchingContrastTarget(private val delegate: ContrastTarget) : ContrastTarget {
    private var lastEvent: Op =
        NONE
    private var lastPosition = -1
    private var lastCount = -1
    private var lastPayload: Any? = null

    override fun onInserted(position: Int, count: Int) {
        if (lastEvent == ADD
            && position >= lastPosition
            && position <= lastPosition + lastCount
        ) {
            lastPosition = min(position, lastPosition)
            lastCount += count
            return
        }
        setStateAndDispatch(position, count, ADD)
    }

    override fun onRemoved(position: Int, count: Int) {
        if (lastEvent == REMOVE
            && lastPosition >= position
            && lastPosition <= position + count
        ) {
            lastPosition = position
            lastCount += count
            return
        }
        setStateAndDispatch(position, count, REMOVE)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        dispatchEvent() //don't merge moves
        delegate.onMoved(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        if (lastEvent == CHANGE
            && !(position > lastPosition + lastCount
                    || position + count < lastPosition
                    || lastPayload != payload)
        ) {
            val prevEnd = lastPosition + lastCount
            lastPosition = min(position, lastPosition)
            lastCount = max(prevEnd, position + count) - lastPosition
            return
        }
        lastPayload = payload
        setStateAndDispatch(position, count, CHANGE)
    }

    fun dispatchEvent() {
        when (lastEvent) {
            NONE -> return
            ADD -> delegate.onInserted(lastPosition, lastCount)
            REMOVE -> delegate.onRemoved(lastPosition, lastCount)
            CHANGE -> delegate.onChanged(lastPosition, lastCount, lastPayload)
        }
        lastPayload = null
        lastEvent = NONE
    }

    internal open fun notifyCompleted() {

    }

    private fun setStateAndDispatch(position: Int, count: Int, op: Op) {
        dispatchEvent()
        lastPosition = position
        lastCount = count
        lastEvent = op
    }

    enum class Op { NONE, ADD, REMOVE, CHANGE }
}