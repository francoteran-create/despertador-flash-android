package com.despertadorflash

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TrucoRulesTest {
    @Test fun authenticOrderAndSpanishDeck() {
        val s = Suit.SWORDS
        val b = Suit.CLUBS
        val o = Suit.COINS
        val c = Suit.CUPS
        val ranking = listOf(Card(s,1), Card(b,1), Card(s,7), Card(o,7), Card(c,3), Card(c,2), Card(o,1),
            Card(s,12), Card(b,11), Card(s,10), Card(c,7), Card(s,6), Card(b,5), Card(o,4))
        assertEquals((14 downTo 1).toList(), ranking.map { it.strength })
        assertEquals(40, spanishDeck.toSet().size)
        assertTrue(spanishDeck.none { it.number in 8..9 })
        assertEquals(Card(c,7).strength, Card(b,7).strength)
    }

    @Test fun envidoFlushAndAllTieOrders() {
        val hand = listOf(Card(Suit.CLUBS,7), Card(Suit.CLUBS,6), Card(Suit.CLUBS,1))
        assertEquals(33, envido(hand))
        assertEquals(34, flor(hand))
        assertEquals(7, envido(listOf(Card(Suit.CUPS,7), Card(Suit.CLUBS,10), Card(Suit.SWORDS,1))))
        val h = Seat.HUMAN
        val c = Seat.CPU
        assertEquals(h, winnerOfHand(listOf(null,null,null), h))
        assertEquals(c, winnerOfHand(listOf(null,null,null), c))
        assertEquals(c, winnerOfHand(listOf(null,c), h))
        assertEquals(h, winnerOfHand(listOf(h,null), c))
        assertEquals(h, winnerOfHand(listOf(h,c,null), c))
        assertEquals(c, winnerOfHand(listOf(h,c,c), h))
        assertNull(winnerOfHand(listOf(h,c), h))
        assertNull(winnerOfHand(listOf(null,null), h))
    }

    @Test fun raisesRefuseAtThePreviousStakeAndAlternateMano() {
        val game = TrucoMatch(15, false, Random(9))
        assertFalse(game.call(Seat.CPU, Call.TRUCO))
        assertTrue(game.call(Seat.HUMAN, Call.TRUCO))
        assertTrue(game.call(Seat.CPU, Call.RETRUCO))
        assertTrue(game.call(Seat.HUMAN, Call.VALE4))
        assertTrue(game.call(Seat.CPU, Call.REFUSE))
        assertEquals(3, game.score(Seat.HUMAN))
        assertEquals(Seat.HUMAN, game.handWinner)
        assertTrue(game.nextHand())
        assertEquals(Seat.CPU, game.mano)
    }

    @Test fun envidoCanInterruptTrucoAndAccumulatesAcceptedRaises() {
        val game = TrucoMatch(30, false, Random(18))
        assertTrue(game.call(Seat.HUMAN, Call.TRUCO))
        assertTrue(game.call(Seat.CPU, Call.ENVIDO))
        assertTrue(game.call(Seat.HUMAN, Call.ENVIDO))
        assertTrue(game.call(Seat.CPU, Call.REAL))
        assertEquals(7, game.view(Seat.HUMAN).bid?.stake)
        assertTrue(game.call(Seat.HUMAN, Call.REFUSE))
        assertEquals(4, game.score(Seat.CPU))
        assertEquals(Call.TRUCO, game.view(Seat.CPU).bid?.call)
        assertTrue(game.call(Seat.CPU, Call.WANT))
        assertEquals(Seat.HUMAN, game.turn)
        assertEquals(2, game.view(Seat.HUMAN).handStake)
    }

    @Test fun equalEnvidoGoesToManoAndFaltaCanFinishImmediately() {
        val equal = (1..300).asSequence().map { TrucoMatch(15, false, Random(it)) }.first {
            envido(it.view(Seat.HUMAN).original) == envido(it.view(Seat.CPU).original)
        }
        assertTrue(equal.call(Seat.HUMAN, Call.FALTA))
        assertTrue(equal.call(Seat.CPU, Call.WANT))
        assertEquals(Seat.HUMAN, equal.matchWinner)
        assertEquals(15, equal.score(Seat.HUMAN))
        assertFalse(equal.nextHand())
        assertEquals(15, faltaPoints(30, 0))
        assertEquals(1, faltaPoints(30, 14))
        assertEquals(15, faltaPoints(30, 15))
        assertEquals(2, faltaPoints(30, 28))
    }

    @Test fun optionalFlorTakesPrecedenceOverEnvidoAndCannotBeSilentlyPlayed() {
        val game = (1..500).asSequence().map { TrucoMatch(15, true, Random(it)) }.first {
            flor(it.view(Seat.HUMAN).original) == null && flor(it.view(Seat.CPU).original) != null
        }
        assertTrue(game.call(Seat.HUMAN, Call.ENVIDO))
        val cpu = game.view(Seat.CPU)
        assertEquals(setOf(Call.FLOR), cpu.calls)
        assertFalse(game.play(Seat.CPU, cpu.own.first()))
        assertTrue(game.call(Seat.CPU, Call.FLOR))
        assertEquals(3, game.score(Seat.CPU))
        assertEquals(Seat.HUMAN, game.turn)
    }

    @Test fun strategySeesOnlyOwnAndPublicCardsAndCompletesMatchesAtAllLevels() {
        for (level in TrucoLevel.entries) for (seed in 1..24) {
            val match = TrucoMatch(if (seed % 2 == 0) 30 else 15, seed % 4 == 0, Random(seed))
            val choices = mapOf(Seat.HUMAN to TrucoStrategy(level, Random(seed + 1)),
                Seat.CPU to TrucoStrategy(level, Random(seed + 3)))
            repeat(850) {
                if (match.matchWinner != null) return@repeat
                if (match.handWinner != null) assertTrue(match.nextHand())
                val who = match.turn
                val view = match.view(who)
                assertTrue(view.original.none { it in match.view(who.other()).original })
                assertTrue(view.publicCards.none { it.card in match.view(who.other()).own })
                when (val action = choices.getValue(who).choose(view).action) {
                    is TrucoAction.Say -> assertTrue("Oferta inválida $action", match.call(who, action.call))
                    is TrucoAction.Use -> assertTrue("Carta inválida $action", match.play(who, action.card))
                }
            }
            assertNotNull("Sin término: $level $seed", match.matchWinner)
        }
    }
}
