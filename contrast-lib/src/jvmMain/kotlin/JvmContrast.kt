package dev.jvmname.contrast

import dev.jvmname.contrast.ListTarget.DiffOp

fun <T> List<T>.diff(
    other: List<T>,
    shouldDetectMoves: Boolean,
    areItemsSame: (a: T, b: T) -> Boolean,
    areContentsEqual: (a: T, b: T) -> Boolean
): List<DiffOp> = this.diff(other, shouldDetectMoves, areItemsSame, areContentsEqual)

fun <T> List<T>.diff(
    other: List<T>,
    shouldDetectMoves: Boolean,
    areItemsSame: (a: T, b: T) -> Boolean,
    areContentsEqual: (a: T, b: T) -> Boolean,
    generatePayload: ((a: T, b: T) -> Any?)? = null
): List<DiffOp> {
    val callback =
        ListContrastCallback(
            this,
            other,
            areItemsSame,
            areContentsEqual,
            generatePayload
        )

    val diff = calculateDiff(callback, shouldDetectMoves)
    ListTarget()
    diff.dispatchUpdatesTo()
}