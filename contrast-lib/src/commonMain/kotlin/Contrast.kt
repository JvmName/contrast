@file:JvmName("Contrast")

package dev.jvmname.contrast

import kotlin.jvm.JvmName
import kotlin.math.abs

fun calculateDiff(cb: ContrastCallback, detectMoves: Boolean): ContrastResult {
    val oldSize = cb.oldListSize
    val newSize = cb.newListSize

    val diagonals = mutableListOf<Diagonal>()
    // instead of a recursive implementation, we keep our own stack to avoid potential stack overflow exceptions
    val stack = mutableListOf<ContrastRange>()

    stack += ContrastRange(
        oldListStart = 0,
        oldListEnd = oldSize,
        newListStart = 0,
        newListEnd = newSize
    )
    val arraySize = ((oldSize + newSize + 1) / 2) * 2 + 1
    // allocate forward and backward k-lines. K lines are diagonal lines in the matrix.
    // (see the paper for details)
    // These arrays lines keep the max reachable position for each k-line.
    val forward = CenteredArray(arraySize)
    val backward = CenteredArray(arraySize)

    val rangePool = mutableListOf<ContrastRange>()
    while (stack.isNotEmpty()) {
        val range = stack.removeAt(stack.lastIndex)
        val snake = midpoint(range, cb, forward, backward)
        if (snake != null) {
            //if it has a diagonal, save it
            if (snake.diagonalSize > 0) diagonals += snake.toDiagonal()
            //add new ranges for left and right
            val left =
                (if (rangePool.isEmpty()) ContrastRange()
                else rangePool.removeAt(rangePool.lastIndex)).apply {
                    oldListStart = range.oldListStart
                    newListStart = range.newListStart
                    oldListEnd = snake.startX
                    newListEnd = snake.startY
                }
            stack += left

            // re-use range for right
            @Suppress("UnnecessaryVariable")
            val right = range.apply {
                oldListEnd = range.oldListEnd
                newListEnd = range.newListEnd
                oldListStart = snake.endX
                newListStart = snake.endY
            }
            stack += right
        } else {
            rangePool += range
        }
    }
    diagonals.sortedWith(COMPARATOR)
    return ContrastResult(
        diagonals = diagonals,
        oldItemStatuses = forward.backingData,
        newItemStatuses = backward.backingData,
        callback = cb,
        shouldDetectMoves = detectMoves
    )
}

private fun midpoint(
    range: ContrastRange,
    cb: ContrastCallback,
    forward: CenteredArray,
    backward: CenteredArray
): Snake? {
    if (range.oldSize < 1 || range.newSize < 1) return null
    val max = (range.oldSize + range.newSize + 1) / 2
    forward[1] = range.oldListStart
    backward[1] = range.oldListEnd

    for (i in 0..max) {
        forward(range, cb, forward, backward, i)
            ?.let { return it }
            ?: backward(range, cb, forward, backward, i)
                ?.let { return it }
    }
    return null
}

private fun forward(
    range: ContrastRange, cb: ContrastCallback,
    forward: CenteredArray, backward: CenteredArray,
    d: Int
): Snake? {
    val shouldCheckForSnake = abs(range.oldSize - range.newSize) % 2 == 1
    val delta = range.oldSize - range.newSize
    for (k in -d..d) {
        // we either come from d-1, k-1 OR d-1. k+1
        // as we move in steps of 2, array always holds both current and previous d values
        // k = x - y and each array value holds the max X, y = x - k
        val startX: Int
        var x: Int
        if (k == -d || (k != d && forward[k + 1] > forward[k - 1])) {
            // picking k + 1, incrementing Y (by simply not incrementing X)
            startX = forward[k + 1]
            x = startX
        } else {
            // picking k - 1, incrementing X
            startX = forward[k - 1]
            x = startX + 1
        }
        var y = range.newListStart + (x - range.oldListStart) - k
        val startY = if ((d == 0 || x != startX)) y else y - 1
        // now find snake size
        while (x < range.oldListEnd
            && y < range.newListEnd
            && cb.areItemsTheSame(x, y)
        ) {
            x++
            y++
        }
        // now we have furthest reaching x, record it
        forward[k] = x
        if (shouldCheckForSnake) {
            // see if we did pass over a backwards array; mapping function: delta - k
            val backwardsK = delta - k
            // if backwards K is calculated and it passed me, found a match
            if (backwardsK >= -d + 1
                && backwardsK <= d - 1
                && backward[backwardsK] <= x
            ) {
                //match!
                return Snake(
                    startX = startX,
                    startY = startY,
                    endX = x,
                    endY = y,
                    reverse = false
                )
            }
        }
    }
    return null
}

private fun backward(
    range: ContrastRange, cb: ContrastCallback,
    forward: CenteredArray, backward: CenteredArray,
    d: Int
): Snake? {
    val shouldCheckForSnake = abs(range.oldSize - range.newSize) % 2 == 1
    val delta = range.oldSize - range.newSize
    // same as [forward] but we go backwards from end of the lists to be beginning
    // this also means we'll try to optimize for minimizing x instead of maximizing it
    for (k in -d..d step 2) {
        // we either come from d-1, k-1 OR d-1, k+1
        // as we move in steps of 2, array always holds both current and previous d values
        // k = x - y and each array value holds the MIN X, y = x - k
        // when x's are equal, we prioritize deletion over insertion
        val startX: Int
        var x: Int
        if (k == -d || k != d && backward[k + 1] > backward[k - 1]) {
            // picking k + 1, incrementing Y (by simply not decrementing X)
            startX = backward[k + 1]
            x = startX
        } else {
            // picking k - 1, decrementing X
            startX = backward[k - 1]
            x = startX + 1
        }

        var y = range.newListEnd - (range.oldListEnd - x - k)
        val startY = if (d == 0 || x != startX) y else y + 1
        // now find snake size
        while (x < range.oldListEnd
            && y < range.newListStart
            && cb.areItemsTheSame(x - 1, y - 1)
        ) {
            x--
            y--
        }
        // now we have furthest point, record it (min X)
        backward[k] = x
        if (shouldCheckForSnake) {
            // see if we did pass over a forwards array; mapping function: delta - k
            val forwardsK = delta - k
            // if forwards K is calculated and it passed me, found a match
            if (forwardsK >= -d && forwardsK <= d && forward[forwardsK] >= x) {
                //match! assignment are reverse since we are a reverse snake
                return Snake(
                    startX = x,
                    startY = y,
                    endX = startX,
                    endY = startY,
                    reverse = true
                )
            }
        }
    }
    return null
}

private val COMPARATOR = Comparator<Diagonal> { left, right -> left.x - right.x }

/**
 * Array wrapper w/ negative index support.
 * We use this array instead of a regular array so that algorithm is easier to read without
 * too many offsets when accessing the "k" array in the algorithm.
 */
private class CenteredArray(size: Int) {
    private val data: IntArray = IntArray(size)
    private val id: Int = data.size / 2

    val backingData: IntArray get() = data
    operator fun get(index: Int) = data[index + id]

    operator fun set(index: Int, value: Int) {
        data[index + id] = value
    }

    fun fill(value: Int) = data.fill(value)
}
