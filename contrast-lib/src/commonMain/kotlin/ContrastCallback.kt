package dev.jvmname.contrast

interface ContrastCallback {
    val oldListSize: Int

    val newListSize: Int

    fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

    fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean

    fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? = null
}

class ListContrastCallback<T>(
    private val oldList: List<T>,
    private val newList: List<T>,
    private val areItemsSame: (a: T, b: T) -> Boolean,
    private val areContentsEqual: (a: T, b: T) -> Boolean,
    private val generatePayload: ((a: T, b: T) -> Any?)?
) : ContrastCallback {
    override val oldListSize = oldList.size
    override val newListSize = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return areItemsSame(oldList[oldItemPosition], newList[newItemPosition])
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return areContentsEqual(oldList[oldItemPosition], newList[newItemPosition])
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return generatePayload?.let { it(oldList[oldItemPosition], newList[newItemPosition]) }
            ?: super.getChangePayload(oldItemPosition, newItemPosition)
    }
}
