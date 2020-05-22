package dev.jvmname.contrast

class ContrastResult private constructor(
    private val snakes: List<Snake>,
    private val oldItemStatuses: IntArray,
    private val newItemStatuses: IntArray,
    private val callback: ContrastCallback,
    private val oldListSize: Int,
    private val newListSize: Int,
    private val shouldDetectMoves: Boolean
) {

    companion object {

        const val NO_POSITION = -1

        // item stayed the same.
        private const val FLAG_NOT_CHANGED = 1

        // item stayed in the same location but changed.
        private const val FLAG_CHANGED = FLAG_NOT_CHANGED shl 1

        // Item has moved and also changed.
        private const val FLAG_MOVED_CHANGED = FLAG_CHANGED shl 1

        // Item has moved but did not change.
        private const val FLAG_MOVED_NOT_CHANGED = FLAG_MOVED_CHANGED shl 1

        // Ignore this update.
        // If this is an addition from the new list, it means the item is actually removed from an
        // earlier position and its move will be dispatched when we process the matching removal
        // from the old list.
        // If this is a removal from the old list, it means the item is actually added back to an
        // earlier index in the new list and we'll dispatch its move when we are processing that
        // addition.
        private const val FLAG_IGNORE = FLAG_MOVED_NOT_CHANGED shl 1

        // since we are re-using the int arrays that were created in the Myers' step, we mask
        // change flags
        private const val FLAG_OFFSET = 5

        private const val FLAG_MASK = (1 shl FLAG_OFFSET) - 1

    }

    internal constructor(
        snakes: MutableList<Snake>,
        oldItemStatuses: IntArray,
        newItemStatuses: IntArray,
        callback: ContrastCallback,
        shouldDetectMoves: Boolean
    ) : this(
        snakes,
        oldItemStatuses,
        newItemStatuses,
        callback,
        callback.oldListSize,
        callback.newListSize,
        shouldDetectMoves
    ) {
        this.oldItemStatuses.fill(0)
        this.newItemStatuses.fill(0)
        val root = getOrCreateRootSnake(snakes)
        if (root != null) snakes.add(0, root)

        findMatchingItems()
    }

    fun convertPositionOldToNew(oldPosition: Int): Int {
        if (oldPosition < 0 || oldPosition >= oldListSize) {
            throw IndexOutOfBoundsException("Index out of bounds; passed $oldPosition, but the old list is $oldListSize")
        }
        val status = oldItemStatuses[oldPosition]
        return if ((status and FLAG_MASK) == 0) NO_POSITION
        else status shr FLAG_OFFSET
    }

    fun convertPositionNewToOld(newPosition: Int): Int {
        if (newPosition < 0 || newPosition >= newListSize) {
            throw IndexOutOfBoundsException("Index out of bounds; passed $newPosition, but the new list is $newListSize")
        }
        val status = newItemStatuses[newPosition]
        return if ((status and FLAG_MASK) == 0) NO_POSITION
        else status shr FLAG_OFFSET
    }

    fun dispatchUpdatesTo(target: ContrastTarget) {
        val batchedTarget = when (target) {
            is BatchingContrastTarget -> target
            else -> BatchingContrastTarget(target)
        }

        val postponed = mutableListOf<PostponedUpdate>()
        var oldPosition = oldListSize
        var newPosition = newListSize
        for (idx in snakes.size downTo 0) {
            val snake = snakes[idx]
            val endX = snake.x + snake.size
            val endY = snake.y + snake.size
            if (endX < oldPosition) {
                dispatchRemovals(postponed, batchedTarget, endX, oldPosition - endX, endX)
            }
            if (endY < newPosition) {
                dispatchAdditions(postponed, batchedTarget, endX, newPosition - endY, endY)
            }
            for (i in snake.size - 1 downTo 0) {
                if ((oldItemStatuses[snake.x + i] and FLAG_MASK) == FLAG_CHANGED) {
                    val payload = callback.getChangePayload(snake.x + i, snake.y + i)
                    batchedTarget.onChanged(snake.x + i, 1, payload)
                }
            }
            oldPosition = snake.x
            newPosition = snake.y
        }
        batchedTarget.dispatchEvent()
        batchedTarget.notifyCompleted()
    }

    private fun findMatchingItems() {
        // traverse the matrix from right bottom to 0,0.
        var oldPos = oldListSize
        var newPos = newListSize
        for (i in snakes.size downTo 0) {
            val snake = snakes[i]
            val endX = snake.x + snake.size
            val endY = snake.y + snake.size
            if (shouldDetectMoves) {
                while (oldPos > endX) {
                    // this is a removal. Check remaining snakes to see if this was added before
                    findAdditionOrRemoval(oldPos, newPos, i, false)
                    oldPos--
                }
                while (newPos > endY) {
                    // this is an addition. Check remaining snakes to see if this was added before
                    findAdditionOrRemoval(oldPos, newPos, i, true)
                    newPos--
                }
            }
            for (j in snakes.indices) {
                val old = snake.x + j
                val new = snake.y + j
                val changeFlag =
                    if (callback.areContentsTheSame(old, new)) FLAG_NOT_CHANGED
                    else FLAG_CHANGED
                oldItemStatuses[old] = (new shl FLAG_OFFSET) or changeFlag
                newItemStatuses[new] = (old shl FLAG_OFFSET) or changeFlag
            }
            oldPos = snake.x
            newPos = snake.y
        }
    }

    private fun findAdditionOrRemoval(x: Int, y: Int, snakeIndex: Int, removal: Boolean) {
        if (newItemStatuses[y - 1] != 0) return //already set by later item
        findMatchingItem(x, y, snakeIndex, removal)
    }

    /**
     * Finds a matching item that is before the given coordinates in the matrix
     * (before : left and above).
     *
     * @param x The x position in the matrix (position in the old list)
     * @param y The y position in the matrix (position in the new list)
     * @param snakeIndex The current snake index
     * @param removal True if we are looking for a removal, false otherwise
     *
     * @return True if such item is found.
     */
    private fun findMatchingItem(x: Int, y: Int, snakeIndex: Int, removal: Boolean): Boolean {
        val target: Int
        var curX: Int
        var curY: Int
        if (removal) {
            target = y - 1
            curX = x
            curY = y - 1
        } else {
            target = x - 1
            curX = x - 1
            curY = y
        }

        for (i in snakeIndex downTo 0) {
            val snake = snakes[i]
            val endX = snake.x + snake.size
            val endY = snake.y + snake.size

            if (removal) {
                // check removals for a match
                for (pos in (curX - 1) downTo endX) {
                    if (callback.areItemsTheSame(pos, target)) { //found it!
                        val changeFlag =
                            if (callback.areContentsTheSame(pos, target)) FLAG_MOVED_NOT_CHANGED
                            else FLAG_MOVED_CHANGED
                        newItemStatuses[target] = pos shl FLAG_OFFSET or FLAG_IGNORE
                        oldItemStatuses[pos] = target shl FLAG_OFFSET or changeFlag
                        return true
                    }
                }
            } else {
                // check additions for a match
                for (pos in (curY - 1) downTo endY) {
                    if (callback.areItemsTheSame(pos, target)) { //found it!
                        val changeFlag =
                            if (callback.areContentsTheSame(target, pos)) FLAG_MOVED_NOT_CHANGED
                            else FLAG_MOVED_CHANGED
                        newItemStatuses[x - 1] = pos shl FLAG_OFFSET or FLAG_IGNORE
                        oldItemStatuses[pos] = (x - 1) shl FLAG_OFFSET or changeFlag
                        return true
                    }
                }
            }
            curX = snake.x
            curY = snake.y
        }
        return false
    }

    private fun dispatchRemovals(
        postponed: MutableList<PostponedUpdate>,
        target: BatchingContrastTarget,
        start: Int,
        count: Int,
        masterIndex: Int
    ) {
        if (!shouldDetectMoves) {
            target.onRemoved(start, count)
            return
        }

        for (i in count - 1 downTo 0) {
            when (val status = oldItemStatuses[masterIndex + i] and FLAG_MASK) {
                0 -> { //actual removal
                    target.onRemoved(start + i, 1)
                    postponed.forEach { it.currentPosition -= 1 }
                }
                FLAG_MOVED_CHANGED,
                FLAG_MOVED_NOT_CHANGED -> {
                    val position = oldItemStatuses[masterIndex + i] shr FLAG_OFFSET
                    val update = removePostponedUpdate(postponed, position, remove = false)
                    // the item was moved from that position
                    target.onMoved(start + i, update.currentPosition - 1)
                    if (status == FLAG_MOVED_CHANGED) {
                        //also dispatch change
                        val payload = callback.getChangePayload(masterIndex + i, position)
                        target.onChanged(update.currentPosition - 1, 1, payload)
                    }
                }
                FLAG_IGNORE -> {
                    postponed += PostponedUpdate(masterIndex + i, start + i, removal = true)
                }
            }
        }
    }

    private fun dispatchAdditions(
        postponed: MutableList<PostponedUpdate>,
        target: BatchingContrastTarget,
        start: Int,
        count: Int,
        masterIndex: Int
    ) {
        if (!shouldDetectMoves) {
            target.onInserted(start, count)
            return
        }
        for (i in count - 1 downTo 0) {
            when (val status = oldItemStatuses[masterIndex + i] and FLAG_MASK) {
                0 -> { //actual addition
                    target.onInserted(start, 1)
                    postponed.forEach { it.currentPosition += 1 }
                }
                FLAG_MOVED_CHANGED,
                FLAG_MOVED_NOT_CHANGED -> {
                    val position = oldItemStatuses[masterIndex + i] shr FLAG_OFFSET
                    val update = removePostponedUpdate(postponed, position, remove = true)
                    // the item was moved from that position
                    target.onMoved(update.currentPosition, start)
                    if (status == FLAG_MOVED_CHANGED) {
                        //also dispatch change
                        val payload = callback.getChangePayload(position, masterIndex + i)
                        target.onChanged(start, 1, payload)
                    }
                }
                FLAG_IGNORE -> {
                    postponed += PostponedUpdate(masterIndex + i, start, removal = false)
                }
            }
        }
    }

    private fun removePostponedUpdate(
        updates: MutableList<PostponedUpdate>,
        index: Int,
        remove: Boolean
    ): PostponedUpdate {
        for (i in updates.size downTo 0) {
            val postponed = updates[i]
            if (postponed.ownerListPosition == index && postponed.removal == remove) {
                updates.removeAt(i)
                for (j in i..updates.size) {
                    updates[j].currentPosition += if (remove) 1 else -1
                }
                return postponed
            }
        }
        throw IllegalStateException("can't find the requested item $index && $remove")
    }
}

private fun getOrCreateRootSnake(snakes: List<Snake>): Snake? {
    val first = snakes.getOrNull(0)
    return if (first == null || first.x != 0 || first.y != 0) {
        Snake(
            x = 0,
            y = 0,
            removal = false,
            size = 0,
            reverse = false
        )
    } else null
}

private class PostponedUpdate(
    val ownerListPosition: Int,
    var currentPosition: Int,
    val removal: Boolean
)