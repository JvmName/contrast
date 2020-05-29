package dev.jvmname.contrast

class ContrastResult private constructor(
    private val diagonals: List<Diagonal>,
    private val oldItemStatuses: IntArray,
    private val newItemStatuses: IntArray,
    private val callback: ContrastCallback,
    private val oldListSize: Int,
    private val newListSize: Int,
    private val shouldDetectMoves: Boolean
) {

    companion object {

        /**Item is not present in the list */
        const val NO_POSITION = -1

        /**
         * While reading the flags below, keep in mind that when multiple items move in a list,
         * Myers's may pick any of them as the anchor item and consider that one NOT_CHANGED while
         * picking others as additions and removals. This is completely fine as we later detect
         * all moves.
         * <p>
         * Below, when an item is mentioned to stay in the same "location", it means we won't
         * dispatch a move/add/remove for it, it DOES NOT mean the item is still in the same
         * position.
         */

        private const val FLAG_NOT_CHANGED = 1 // item stayed the same.
        private const val FLAG_CHANGED =
            FLAG_NOT_CHANGED shl 1 // item stayed in the same location but changed.
        private const val FLAG_MOVED_CHANGED =
            FLAG_CHANGED shl 1 // Item has moved and also changed.
        private const val FLAG_MOVED_NOT_CHANGED =
            FLAG_MOVED_CHANGED shl 1 // Item has moved but did not change.
        private const val FLAG_MOVED = FLAG_MOVED_CHANGED or FLAG_MOVED_NOT_CHANGED //item moved

        // since we are re-using the int arrays that were created in the Myers' step, we mask change flags
        private const val FLAG_OFFSET = 4

        private const val FLAG_MASK = (1 shl FLAG_OFFSET) - 1

    }

    internal constructor(
        diagonals: MutableList<Diagonal>,
        oldItemStatuses: IntArray,
        newItemStatuses: IntArray,
        callback: ContrastCallback,
        shouldDetectMoves: Boolean
    ) : this(
        diagonals,
        oldItemStatuses,
        newItemStatuses,
        callback,
        callback.oldListSize,
        callback.newListSize,
        shouldDetectMoves
    ) {
        this.oldItemStatuses.fill(0)
        this.newItemStatuses.fill(0)

        val root = addEdgeDiagonal(diagonals, callback.oldListSize, callback.newListSize)
        if (root != null) diagonals.add(0, root)
        else diagonals += Diagonal(x = oldListSize, y = newListSize, size = 0)

        findMatchingItems()
    }

    private fun addEdgeDiagonal(
        diagonals: List<Diagonal>,
        oldListSize: Int,
        newListSize: Int
    ): Diagonal? {
        val first = diagonals.getOrNull(0)
        return if (first == null || first.x != 0 || first.y != 0) Diagonal(x = 0, y = 0, size = 0)
        else null
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

        val postponedUpdates = ArrayDeque<PostponedUpdate>()
        // track up to date current list size for moves
        // when a move is found, we record its position from the end of the list (which is
        // less likely to change since we iterate in reverse).
        // Later when we find the match of that move, we dispatch the update
        var curSize: Int = oldListSize
        // posX and posY are exclusive
        var posX = oldListSize
        var posY = newListSize
        for (idx in diagonals.size downTo 0) {
            val diag = diagonals[idx]
            val endX = diag.endX
            val endY = diag.endY
            // dispatch removals and additions until we reach to that diagonal
            // first remove then add so that it can go into its place and we don't need to offset values

            while (posX > endX) {
                posX--  // REMOVAL
                val status = oldItemStatuses[posX]
                if (status and FLAG_MOVED != 0) {
                    val newPos = status shr FLAG_OFFSET
                    val postponed = popPostponed(postponedUpdates, newPos, false)
                    if (postponed != null) {
                        val updatedPos = curSize - postponed.currentPosition
                        batchedTarget.onMoved(posX, updatedPos - 1)
                        if (status and FLAG_MOVED_CHANGED != 0) {
                            val payload = callback.getChangePayload(posX, newPos)
                            batchedTarget.onChanged(updatedPos - 1, 1, payload)
                        }
                    } else {
                        // first time we are seeing this, we'll see a matching addition
                        postponedUpdates += PostponedUpdate(
                            ownerListPosition = posX,
                            currentPosition = curSize - posX - 1,
                            removal = true
                        )
                    }
                } else {
                    batchedTarget.onRemoved(posX, 1)
                    curSize--
                }
            }

            while (posY > endY) {
                posY--  // ADDITION
                val status = newItemStatuses[posY]
                if (status and FLAG_MOVED != 0) {
                    //move, not addition; see if it's postponed
                    val oldPos = status shr FLAG_OFFSET
                    //get postponed removal
                    val postponed = popPostponed(postponedUpdates, oldPos, true)
                    if (postponed == null) {
                        // punt until we see the removal
                        postponedUpdates += PostponedUpdate(
                            ownerListPosition = posY,
                            currentPosition = curSize - posX,
                            removal = false
                        )
                    } else {
                        // oldPosFromEnd = foundListSize - posX
                        // we can find posX if we swap the list sizes
                        // posX = listSize - oldPosFromEnd
                        val updatedPos = curSize - postponed.currentPosition - 1
                        batchedTarget.onMoved(updatedPos, posX)
                        if (status and FLAG_MOVED_CHANGED != 0) {
                            val payload = callback.getChangePayload(oldPos, posY)
                            batchedTarget.onChanged(posX, 1, payload)
                        }
                    }
                } else {
                    batchedTarget.onInserted(posX, 1) //a simple insertion
                    curSize++
                }
            }

            //now dispatch updates for the diagonal
            posX = diag.x
            posY = diag.y
            for (i in diag.size - 1 downTo 0) {
                if ((oldItemStatuses[posX] and FLAG_MASK) == FLAG_CHANGED) { //dispatch
                    val payload = callback.getChangePayload(posX, posY)
                    batchedTarget.onChanged(posX, 1, payload)
                }
                posX++
                posY++
            }
            //snap back for next diagonal
            posX = diag.x
            posY = diag.y
        }
        batchedTarget.dispatchEvent()
        batchedTarget.notifyCompleted()
    }

    /**
     * Find position mapping from old list to new list.
     * If moves are requested, we'll also try to do an n^2 search between additions and
     * removals to find moves.
     */
    private fun findMatchingItems() {
        for (diag in diagonals) {
            for (offset in 0..diag.size) {
                val posX = diag.x + offset
                val posY = diag.y + offset
                val changeFlag =
                    if (callback.areContentsTheSame(posX, posY)) FLAG_NOT_CHANGED else FLAG_CHANGED
                oldItemStatuses[posX] = (posY shl FLAG_OFFSET) or changeFlag
                newItemStatuses[posY] = (posX shl FLAG_OFFSET) or changeFlag
            }
        }
        // now all matches are marked, lets look for moves
        // traverse each addition / removal from the end of the list, find matching addition removal from before
        if (shouldDetectMoves) findMoveMatches()
    }

    private fun findMoveMatches() {
        var posX = 0
        for (diagonal in diagonals) {
            while (posX < diagonal.x) {
                if (oldItemStatuses[posX] == 0) {
                    //if there's a removal, find the addition from the rest that matches
                    findMatchingAddition(posX)
                }
                posX++
            }
            // snap back for the next diagonal
            posX = diagonal.endX
        }
    }

    private fun findMatchingAddition(posX: Int) {
        var posY = 0
        for (diagonal in diagonals) {
            while (posY < diagonal.y) {
                //found some additions
                if (newItemStatuses[posY] == 0) { //not eval'd yet
                    if (callback.areItemsTheSame(posX, posY)) {
                        //found it! set values
                        val changeFlag =
                            if (callback.areContentsTheSame(posX, posY)) FLAG_MOVED_NOT_CHANGED
                            else FLAG_MOVED_CHANGED
                        // once we process one of these, it will mark the other one as ignored.
                        oldItemStatuses[posX] = (posY shl FLAG_OFFSET) or changeFlag
                        newItemStatuses[posY] = (posX shl FLAG_OFFSET) or changeFlag
                    }
                }
                posY++
            }
            posY = diagonal.endY
        }
    }

    private fun popPostponed(
        updates: MutableList<PostponedUpdate>,
        index: Int,
        remove: Boolean
    ): PostponedUpdate? {
        val iter = updates.iterator()
        var needle: PostponedUpdate? = null
        for (update in iter) {
            if (update.ownerListPosition == index && update.removal == remove) {
                needle = update
                break
            }
        }
        for (update in iter) { // re-offset all others
            update.currentPosition += if (remove) 1 else -1
        }

        return needle
    }
}

private class PostponedUpdate(
    val ownerListPosition: Int,
    var currentPosition: Int,
    val removal: Boolean
)