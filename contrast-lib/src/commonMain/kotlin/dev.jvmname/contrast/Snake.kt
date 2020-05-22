package dev.jvmname.contrast

/**
 * Snakes represent a match between two lists. It is optionally prefixed or postfixed with an
 * add or remove operation. See the Myers' paper for details.
 * @property x Position in the old list
 * @property y  Position in the new list
 * @property size Number of matches. Might be 0.
 * @property removal If true, this is a removal from the original list followed by [size] matches. If false, this is an addition from the new list followed by [size] matches.
 * @property reverse If true, the addition or removal is at the end of the snake. If false, the addition or removal is at the beginning of the snake.
 */
internal class Snake(
    var x: Int = 0,
    var y: Int = 0,
    val size: Int = 0,
    val removal: Boolean = false,
    val reverse: Boolean = false
)