package dev.jvmname.contrast

import dev.jvmname.contrast.BatchingContrastTarget
import dev.jvmname.contrast.ContrastTarget

class ListTarget(
    private val onCompleted: (List<DiffOp>) -> Unit,
    private val listener: InnerListener = InnerListener()
) : BatchingContrastTarget(listener) {

    sealed class DiffOp {
        class Insert(val position: Int, val count: Int) : DiffOp()
        class Remove(val position: Int, val count: Int) : DiffOp()
        class Move(val from: Int, val to: Int) : DiffOp()
        class Change(val position: Int, val count: Int, val payload: Any?) : DiffOp()
    }

    class InnerListener : ContrastTarget {
        val updates = mutableListOf<DiffOp>()

        override fun onInserted(position: Int, count: Int) {
            updates += DiffOp.Insert(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            updates += DiffOp.Remove(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            updates += DiffOp.Move(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            updates += DiffOp.Change(position, count, payload)
        }
    }

    override fun notifyCompleted() = onCompleted(listener.updates)
}