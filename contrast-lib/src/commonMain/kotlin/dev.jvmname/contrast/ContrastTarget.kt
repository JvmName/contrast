package dev.jvmname.contrast

import kotlin.math.max
import kotlin.math.min

interface ContrastTarget {
    fun onInserted(position: Int, count: Int)
    fun onRemoved(position: Int, count: Int)
    fun onMoved(fromPosition: Int, toPosition: Int)
    fun onChanged(position: Int, count: Int, payload: Any?)
}

open class BatchingContrastTarget(private val delegate: ContrastTarget) : ContrastTarget {
    private var lastEvent: Op = Op.NONE
    private var lastPosition = -1
    private var lastCount = -1
    private var lastPayload: Any? = null

    override fun onInserted(position: Int, count: Int) {
        if (lastEvent == Op.ADD
            && position >= lastPosition
            && position <= lastPosition + lastCount
        ) {
            lastPosition = min(position, lastPosition)
            lastCount += count
            return
        }
        setStateAndDispatch(position, count, Op.ADD)
    }

    override fun onRemoved(position: Int, count: Int) {
        if (lastEvent == Op.REMOVE
            && lastPosition >= position
            && lastPosition <= position + count
        ) {
            lastPosition = position
            lastCount += count
            return
        }
        setStateAndDispatch(position, count, Op.REMOVE)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        dispatchEvent() //don't merge moves
        delegate.onMoved(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        if (lastEvent == Op.CHANGE
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
        setStateAndDispatch(position, count, Op.CHANGE)
    }

    fun dispatchEvent() {
        when (lastEvent) {
            Op.NONE -> return
            Op.ADD -> delegate.onInserted(lastPosition, lastCount)
            Op.REMOVE -> delegate.onRemoved(lastPosition, lastCount)
            Op.CHANGE -> delegate.onChanged(lastPosition, lastCount, lastPayload)
        }
        lastPayload = null
        lastEvent = Op.NONE
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