package dev.jvmname.contrast

import kotlin.math.min

/**
 * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
 * add or remove operation. See the Myers' paper for details.
 * @property startX Start position in the old list
 * @property startY  Start position in the new list
 * @property endX End position in the old list
 * @property endY  End position in the new list
 * @property reverse True if this snake was created in the reverse search, false otherwise.
 */
internal class Snake(
    var startX: Int = 0,
    var startY: Int = 0,
    var endX: Int = 0,
    var endY: Int = 0,
    val reverse: Boolean = false
) {
    internal val hasAdditionOrRemoval: Boolean get() = endY - startY != endX - startX

    internal val isAddition: Boolean get() = endY - startY > endX - startX

    internal val diagonalSize: Int get() = min(endX - startX, endY - startY)
}

internal fun Snake.toDiagonal(): Diagonal {
    return if (hasAdditionOrRemoval) {
        when {
            reverse -> Diagonal(startX, startY, diagonalSize) // snake edge it at the end
            isAddition -> Diagonal(startX, startY + 1, diagonalSize) // snake edge it at the beginning
            else -> Diagonal(startX + 1, startY, diagonalSize)
        }
    } else {
        Diagonal(startX, startY, endX - startX)
    }
}

/**
 * A diagonal is a match in the graph.
 * Rather than snakes, we only record the diagonals in the path.
 */
internal class Diagonal(val x: Int, val y: Int, val size: Int) {

    val endX: Int get() = x + size

    val endY: Int get() = y + size
}