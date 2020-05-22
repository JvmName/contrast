package dev.jvmname.contrast

import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import dev.jvmname.contrast.ListTarget.DiffOp

fun <T> List<T>.diff(
    other: List<T>,
    shouldDetectMoves: Boolean,
    areItemsSame: (a: T, b: T) -> Boolean,
    areContentsEqual: (a: T, b: T) -> Boolean
): List<DiffOp> = this.diff(other, shouldDetectMoves, areItemsSame, areContentsEqual, null)

fun <T> List<T>.diff(
    other: List<T>,
    shouldDetectMoves: Boolean,
    areItemsSame: (a: T, b: T) -> Boolean,
    areContentsEqual: (a: T, b: T) -> Boolean,
    generatePayload: ((a: T, b: T) -> Any?)? = null
): List<DiffOp> {
    val callback =
        ListContrastCallback(this, other, areItemsSame, areContentsEqual, generatePayload)

    val diff = calculateDiff(callback, shouldDetectMoves)
    ListTarget()
    diff.dispatchUpdatesTo()
}

class RVAdapterTarget(private val adapter: RecyclerView.Adapter<*>) : ContrastTarget,
                                                                      ListUpdateCallback {
    override fun onInserted(position: Int, count: Int) {
        adapter.notifyItemRangeInserted(position, count);
    }

    override fun onRemoved(position: Int, count: Int) {
        adapter.notifyItemRangeRemoved(position, count);
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        adapter.notifyItemMoved(fromPosition, toPosition);
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        adapter.notifyItemRangeChanged(position, count, payload);
    }
}

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