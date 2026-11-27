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
        const val LOCK_MS = 450L
        const val MAX_RESETS = 8
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

    var gravityAt = GRAVITY_MS
        private set
    var lockAt: Long? = null
        private set
    private var lockLimit: Long? = null
    private var resets = 0

    fun nextEvent() = minOf(gravityAt, lockAt ?: Long.MAX_VALUE)

    fun tick(now: Long) {
        val piece = active ?: return
        if (lockAt?.let { now >= it } == true && supported(piece)) {
            settle(now)
            return
        }
        if (now >= gravityAt) {
            gravityAt += GRAVITY_MS
            val down = piece.copy(y = piece.y + 1)
            if (fits(down)) active = down
            updateSupport(now, false)
        }
    }

    fun landing(): FallingBlock? {
        var piece = active ?: return null
        while (fits(piece.copy(y = piece.y + 1))) piece = piece.copy(y = piece.y + 1)
        return piece
    }

    fun hardDrop(now: Long) {
        active = landing() ?: return
        settle(now)
    }

    private fun supported(piece: FallingBlock) = !fits(piece.copy(y = piece.y + 1))

    private fun updateSupport(now: Long, movedOnGround: Boolean) {
        val piece = active ?: return
        if (!supported(piece)) {
            lockAt = null
            return
        }
        if (lockLimit == null) lockLimit = now + LOCK_MS * (MAX_RESETS + 1)
        if (lockAt == null || movedOnGround && resets < MAX_RESETS) {
            if (movedOnGround && lockAt != null) resets++
            lockAt = minOf(now + LOCK_MS, requireNotNull(lockLimit))
        }
    }

    private fun take(): BlockShape {
        if (bag.isEmpty()) bag.addAll(BlockShape.entries.shuffled(random))
        return bag.removeFirst()
    }

    private fun spawn(): FallingBlock {
        val piece = FallingBlock(next, WIDTH / 2, 1)
        next = take()
        return piece
    }

    fun reset(now: Long = 0) {
        board = emptyBoard()
        pending = emptySet()
        score = 0
        ended = false
        bag.clear()
        next = take()
        active = spawn()
        restartClocks(now)
    }

    private fun fits(piece: FallingBlock): Boolean = piece.positions().all {
        it.x in 0 until WIDTH && it.y in 0 until HEIGHT && board[it.y][it.x] == 0
    }

    fun shift(delta: Int, now: Long = 0) {
        val piece = active ?: return
        val moved = piece.copy(x = piece.x + delta)
        if (fits(moved)) {
            active = moved
            updateSupport(now, supported(piece))
        }
    }

    fun turn(now: Long = 0) {
        val piece = active ?: return
        for (offset in listOf(0, -1, 1, -2, 2)) {
            val turned = piece.copy(x = piece.x + offset, angle = (piece.angle + 1) % 4)
            if (fits(turned)) {
                if (turned.positions() == piece.positions()) return
                active = turned
                updateSupport(now, supported(piece))
                return
            }
        }
    }

    private fun settle(now: Long) {
        val piece = active ?: return
        val settled = board.map { it.toMutableList() }
        piece.positions().forEach { settled[it.y][it.x] = piece.shape.ink }
        board = settled
        pending = completeRows(board)
        if (pending.isEmpty()) nextPiece(now) else active = null
    }

    fun removeLines(now: Long = 0) {
        if (pending.isEmpty()) return
        val count = pending.size
        board = collapseRows(board, pending)
        score += pointsForRows(count)
        pending = emptySet()
        nextPiece(now)
    }

    private fun restartClocks(now: Long) {
        gravityAt = now + GRAVITY_MS
        lockAt = null
        lockLimit = null
        resets = 0
    }

    private fun nextPiece(now: Long) {
        restartClocks(now)
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
