package com.despertadorflash

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class NeonBlocksTest {
    @Test fun pieceMovesAndStopsAtWall() {
        val game = NeonBlocks(Random(12))
        repeat(30) { game.shift(-1) }
        val left = requireNotNull(game.active)
        assertEquals(0, left.positions().minOf { it.x })
        game.shift(-1)
        assertEquals(left, game.active)
        game.turn()
        assertTrue(requireNotNull(game.active).positions().all { it.x in 0 until NeonBlocks.WIDTH })
    }

    @Test fun whenPieceSettlesPreviouslyDisplayedNextShapeIsSpawned() {
        val game = NeonBlocks(Random(2))
        val first = requireNotNull(game.active)
        val next = game.next
        val shadow = requireNotNull(game.landing())
        game.hardDrop(40)
        assertTrue(shadow.positions().all { game.board[it.y][it.x] == first.shape.ink })
        assertEquals(first.shape.cells.size, game.board.flatten().count { it != 0 })
        assertEquals(next, game.active?.shape)
        assertEquals(620L, NeonBlocks.GRAVITY_MS)
        game.reset()
        assertEquals(0, game.score)
        assertFalse(game.ended)
        assertEquals(emptyBoard(), game.board)
    }

    @Test fun lateralMovesDoNotDelayGravityAndNaturalLandingAllows450ms() {
        val game = NeonBlocks(Random(3))
        game.shift(1, 100)
        assertEquals(620L, game.gravityAt)
        while (game.lockAt == null) game.tick(game.nextEvent())
        val lock = requireNotNull(game.lockAt)
        assertTrue(game.board.flatten().all { it == 0 })
        assertEquals(450L, NeonBlocks.LOCK_MS)
        game.tick(lock - 1)
        assertTrue(game.board.flatten().all { it == 0 })
        game.tick(lock)
        assertTrue(game.board.flatten().any { it != 0 })
    }

    @Test fun shiftingAtTheFloorCannotExtendLockForever() {
        val game = NeonBlocks(Random(8))
        while (game.lockAt == null) game.tick(game.nextEvent())
        val first = requireNotNull(game.lockAt)
        repeat(NeonBlocks.MAX_RESETS + 5) { index ->
            val now = requireNotNull(game.lockAt) - 20
            game.shift(if (index % 2 == 0) -1 else 1, now)
        }
        val last = requireNotNull(game.lockAt)
        assertTrue(last - first <= NeonBlocks.LOCK_MS * NeonBlocks.MAX_RESETS)
        game.tick(last)
        assertTrue(game.board.flatten().any { it != 0 })
    }

    @Test fun rowsCollapseWithoutChangingTheirSurvivingOrder() {
        val filled = List(NeonBlocks.WIDTH) { 3 }
        val marked = List(NeonBlocks.WIDTH) { if (it == 0) 2 else 0 }
        val board = emptyBoard().dropLast(3) + listOf(filled, marked, filled)
        val lines = completeRows(board)
        assertEquals(setOf(15, 17), lines)
        assertEquals(340L, NeonBlocks.CLEAR_MS)
        assertEquals(marked, collapseRows(board, lines).last())
        assertEquals(400, pointsForRows(lines.size))
    }

    @Test fun birthdayAndPrivacyLabelsUseExistingProfileFields() {
        assertTrue(birthdayToday("29/02/1988", 29, 2))
        assertFalse(birthdayToday("29/02/1988", 28, 2))
        assertFalse(birthdayToday("", 29, 2))
        assertTrue(eligibleBirthday("29/02/1988", 29, 2, 2028, setOf("2024")))
        assertFalse(eligibleBirthday("29/02/1988", 29, 2, 2028, setOf("2028")))
        assertTrue(eligibleBirthday("01/01/1988", 1, 1, 2029, setOf("2028")))
        assertEquals("BIENVENIDO", openingSalutation(NeonGender.MAN))
        assertEquals("BIENVENIDA", openingSalutation(NeonGender.WOMAN))
        assertEquals("TE DAMOS LA BIENVENIDA", openingSalutation(NeonGender.PRIVATE))
    }
}
