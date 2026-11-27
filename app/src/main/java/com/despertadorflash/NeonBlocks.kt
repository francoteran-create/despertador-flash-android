package com.despertadorflash

import kotlin.random.Random

internal data class BlockCell(val x: Int, val y: Int)

internal enum class BlockShape(val cells: List<BlockCell>) {
    ARC(listOf(BlockCell(-1, 0), BlockCell(0, 0), BlockCell(0, 1), BlockCell(1, 1))),
    BEAM(listOf(BlockCell(-1, 0), BlockCell(0, 0), BlockCell(1, 0), BlockCell(2, 0))),
    STEP(listOf(BlockCell(-1, 0), BlockCell(0, 0), BlockCell(1, 0), BlockCell(1, 1))),
    LANTERN(listOf(BlockCell(-1, 0), BlockCell(0, 0), BlockCell(1, 0), BlockCell(0, 1))),
    CORNER(listOf(BlockCell(0, 0), BlockCell(1, 0), BlockCell(0, 1))),
    HALO(listOf(BlockCell(0, -1), BlockCell(-1, 0), BlockCell(0, 0), BlockCell(1, 0), BlockCell(0, 1))),
    GEM(listOf(BlockCell(0, 0), BlockCell(1, 0), BlockCell(0, 1), BlockCell(1, 1)));

    val ink: Int get() = ordinal + 1
}

internal data class FallingBlock(val shape: BlockShape, val x: Int, val y: Int, val angle: Int = 0) {
    fun positions(): List<BlockCell> = shape.cells.map { cell ->
        val rotated = if (shape == BlockShape.GEM) cell else when (angle % 4) {
            1 -> BlockCell(-cell.y, cell.x)
            2 -> BlockCell(-cell.x, -cell.y)
            3 -> BlockCell(cell.y, -cell.x)
            else -> cell
        }
        BlockCell(x + rotated.x, y + rotated.y)
    }
}

internal class NeonBlocks(private val random: Random = Random.Default) {
    companion object {
        const val WIDTH = 9
        const val HEIGHT = 18
        const val GRAVITY_MS = 620L
        const val CLEAR_MS = 340L
    }

    private val bag = ArrayDeque<BlockShape>()
    var board: List<List<Int>> = emptyBoard()
        private set
    var next = take()
        private set
    var active: FallingBlock? = spawn()
        private set
    var pending: Set<Int> = emptySet()
        private set
    var score = 0
        private set
    var ended = false
        private set

    private fun take(): BlockShape {
        if (bag.isEmpty()) bag.addAll(BlockShape.entries.shuffled(random))
        return bag.removeFirst()
    }

    private fun spawn(): FallingBlock {
        val piece = FallingBlock(next, WIDTH / 2, 1)
        next = take()
        return piece
    }

    fun reset() {
        board = emptyBoard()
        pending = emptySet()
        score = 0
        ended = false
        bag.clear()
        next = take()
        active = spawn()
    }

    private fun fits(piece: FallingBlock): Boolean = piece.positions().all {
        it.x in 0 until WIDTH && it.y in 0 until HEIGHT && board[it.y][it.x] == 0
    }

    fun shift(delta: Int) {
        val piece = active ?: return
        val moved = piece.copy(x = piece.x + delta)
        if (fits(moved)) active = moved
    }

    fun turn() {
        val piece = active ?: return
        for (offset in listOf(0, -1, 1, -2, 2)) {
            val turned = piece.copy(x = piece.x + offset, angle = (piece.angle + 1) % 4)
            if (fits(turned)) {
                active = turned
                return
            }
        }
    }

    fun lower() {
        val piece = active ?: return
        val moved = piece.copy(y = piece.y + 1)
        if (fits(moved)) {
            active = moved
            return
        }
        val settled = board.map { it.toMutableList() }
        piece.positions().forEach { settled[it.y][it.x] = piece.shape.ink }
        board = settled
        pending = completeRows(board)
        if (pending.isEmpty()) nextPiece() else active = null
    }

    fun removeLines() {
        if (pending.isEmpty()) return
        val count = pending.size
        board = collapseRows(board, pending)
        score += pointsForRows(count)
        pending = emptySet()
        nextPiece()
    }

    private fun nextPiece() {
        val piece = spawn()
        ended = !fits(piece)
        active = piece.takeUnless { ended }
    }
}

internal fun emptyBoard() = List(NeonBlocks.HEIGHT) { List(NeonBlocks.WIDTH) { 0 } }
internal fun completeRows(board: List<List<Int>>): Set<Int> =
    board.indices.filterTo(linkedSetOf()) { index -> board[index].all { it != 0 } }

internal fun collapseRows(board: List<List<Int>>, rows: Set<Int>): List<List<Int>> =
    List(rows.size) { List(NeonBlocks.WIDTH) { 0 } } + board.filterIndexed { index, _ -> index !in rows }

internal fun pointsForRows(count: Int) = 100 * count * count
