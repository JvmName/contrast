package dev.jvmname.contrast

internal class ContrastRange(
    var oldListStart: Int,
    var oldListEnd: Int,
    var newListStart: Int,
    var newListEnd: Int
) {
    constructor() : this(0, 0, 0, 0)
}