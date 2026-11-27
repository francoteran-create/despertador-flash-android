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
        repeat(NeonBlocks.HEIGHT - 2 - first.positions().maxOf { it.y } + 1) { game.lower() }
        assertTrue(game.board.flatten().all { it == 0 })
        game.lower()
        assertEquals(first.shape.cells.size, game.board.flatten().count { it != 0 })
        assertEquals(next, game.active?.shape)
        assertEquals(620L, NeonBlocks.GRAVITY_MS)
        game.reset()
        assertEquals(0, game.score)
        assertFalse(game.ended)
        assertEquals(emptyBoard(), game.board)
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
        assertEquals("BIENVENIDO", openingSalutation(NeonGender.MAN))
        assertEquals("BIENVENIDA", openingSalutation(NeonGender.WOMAN))
        assertEquals("TE DAMOS LA BIENVENIDA", openingSalutation(NeonGender.PRIVATE))
    }
}
