import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import dev.jvmname.contrast.BatchingContrastTarget
import dev.jvmname.contrast.ContrastTarget
import dev.jvmname.contrast.ListContrastCallback
import dev.jvmname.contrast.calculateDiff

fun <T> List<T>.diff(
    other: List<T>,
    shouldDetectMoves: Boolean,
    areItemsSame: (a: T, b: T) -> Boolean,
    areContentsEqual: (a: T, b: T) -> Boolean,
    target: ContrastTarget
) {

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

