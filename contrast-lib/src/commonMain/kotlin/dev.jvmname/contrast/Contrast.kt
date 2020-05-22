@file:JvmName("Contrast")

package dev.jvmname.contrast

import kotlin.jvm.JvmName
import kotlin.math.abs

fun calculateDiff(cb: ContrastCallback, detectMoves: Boolean): ContrastResult {
    val oldSize = cb.oldListSize
    val newSize = cb.newListSize

    val snakes = mutableListOf<Snake>()
    // instead of a recursive implementation, we keep our own stack to avoid potential stack overflow exceptions
    val stack = mutableListOf<ContrastRange>()

    stack += ContrastRange(
        oldListStart = 0,
        oldListEnd = oldSize,
        newListStart = 0,
        newListEnd = newSize
    )

    val max = oldSize + newSize + abs(oldSize - newSize)
    val forward = IntArray(max * 2)
    val backward = IntArray(max * 2)

    val rangePool = mutableListOf<ContrastRange>()
    while (stack.isNotEmpty()) {
        val range = stack.removeAt(stack.lastIndex)
        val snake = diffPartial(
            cb,
            range.oldListStart, range.oldListEnd,
            range.newListStart, range.newListEnd,
            forward, backward, max
        )
        if (snake != null) {
            if (snake.size > 0) snakes += snake
            // offset the snake to convert its coordinates from the Range's area to global
            snake.x += range.oldListStart
            snake.y += range.newListStart

            val left =
                (if (rangePool.isEmpty()) ContrastRange()
                else rangePool.removeAt(rangePool.lastIndex)).apply {
                    oldListStart = range.oldListStart
                    newListStart = range.newListStart
                }

            if (snake.reverse) {
                left.oldListEnd = snake.x
                left.newListEnd = snake.y
            } else {
                if (snake.removal) {
                    left.oldListEnd = snake.x - 1
                    left.newListEnd = snake.y
                } else {
                    left.oldListEnd = snake.x
                    left.newListEnd = snake.y - 1
                }
            }
            stack += left
            // re-use range for right
            @Suppress("UnnecessaryVariable")
            val right = range
            if (snake.reverse) {
                if (snake.removal) {
                    right.oldListStart = snake.x + snake.size + 1
                    right.newListStart = snake.y + snake.size
                } else {
                    right.oldListStart = snake.x + snake.size
                    right.newListStart = snake.y + snake.size + 1
                }
            } else {
                right.oldListStart = snake.x + snake.size
                right.newListStart = snake.y + snake.size
            }
            stack += right
        } else {
            rangePool += range
        }
    }
    snakes.sortedWith(COMPARATOR)
    return ContrastResult(
        snakes = snakes,
        oldItemStatuses = forward,
        newItemStatuses = backward,
        callback = cb,
        shouldDetectMoves = detectMoves
    )
}

private fun diffPartial(
    cb: ContrastCallback,
    startOld: Int,
    endOld: Int,
    startNew: Int,
    endNew: Int,
    forward: IntArray,
    backward: IntArray,
    kOffset: Int
): Snake? {
    val oldSize = endOld - startOld
    val newSize = endNew - startNew

    if (endOld - startOld < 1 || endNew - startNew < 1) return null
    val delta = oldSize - newSize
    val deltaLimit = (oldSize + newSize + 1) / 2

    forward.fill(0, kOffset - deltaLimit - 1, kOffset + deltaLimit + 1)
    backward.fill(oldSize, kOffset - deltaLimit - 1 + delta, kOffset + deltaLimit + 1 + delta)

    val checkInForward = delta % 2 != 0
    for (d in 0..deltaLimit) {
        for (k in -d..d step 2) {
            // find forward path
            // we can reach k from k - 1 or k + 1. Check which one is further in the graph
            var x: Int
            val removal: Boolean
            if (k == -d || (k != d && forward[kOffset + k - 1] < forward[kOffset + k + 1])) {
                x = forward[kOffset + k + 1]
                removal = false
            } else {
                x = forward[kOffset + k - 1] + 1
                removal = true
            }
            // set y based on x
            var y = x - k
            // move diagonal as long as items match
            while (x < oldSize
                && y < newSize
                && cb.areItemsTheSame(startOld + x, startNew + y)
            ) {
                x++
                y++
            }

            forward[kOffset + k] = x
            if (checkInForward
                && k >= delta - d + 1
                && k <= delta + d - 1
                && forward[kOffset + k] >= backward[kOffset + k]
            ) {
                val xParam = backward[kOffset + k]
                return Snake(
                    x = xParam,
                    y = xParam - k,
                    size = forward[kOffset + k] - xParam,
                    removal = removal,
                    reverse = false
                )
            }
        }
        for (k in -d..d step 2) {
            // find reverse path at k + delta, in reverse
            val backK = k + delta
            var x: Int
            val removal: Boolean
            if (backK == d + delta
                || (backK != -d + delta && backward[kOffset + backK - 1] < backward[kOffset + backK + 1])
            ) {
                x = backward[kOffset + backK - 1]
                removal = false
            } else {
                x = backward[kOffset + backK + 1] - 1
                removal = true
            }

            // set y based on x
            var y = x - backK
            while (x > 0
                && y > 0
                && cb.areItemsTheSame(startOld + x - 1, startNew + y - 1)
            ) {
                x--
                y--
            }

            backward[kOffset + backK] = x

            if (!checkInForward
                && k + delta >= -d
                && k + delta <= d
                && forward[kOffset + backK] >= backward[kOffset + backK]
            ) {
                val xParam = backward[kOffset + backK]
                return Snake(
                    x = xParam,
                    y = xParam - backK,
                    size = forward[kOffset + backK] - backward[kOffset + backK],
                    removal = removal,
                    reverse = true
                )
            }
        }
    }
    throw IllegalStateException(
        """DiffUtil hit an unexpected case while trying to calculate
            |the optimal path! Your data should not be changing during the diff calculation. """.trimMargin()
    )
}

private val COMPARATOR = Comparator<Snake> { left, right ->
    (left.x - right.x).takeIf { it != 0 } ?: (left.y - right.y)
}

