package com.despertadorflash

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TrucoDialogueTest {
    private val engine = TrucoMatch(30, false, Random(17))
    private val view = engine.view(Seat.CPU)
    private val card = view.own.first()
    private val plain = RivalMove(TrucoAction.Use(card), RivalMood.CALM)

    @Test fun reactionsAreBasedOnPublicEventsAndOwnIntent() {
        assertEquals(RivalMoment.ENVIDO, rivalMoment(view, RivalMove(TrucoAction.Say(Call.REAL), RivalMood.CALM), null))
        assertEquals(RivalMoment.TRUCO, rivalMoment(view, RivalMove(TrucoAction.Say(Call.RETRUCO), RivalMood.CALM), null))
        assertEquals(RivalMoment.BLUFF, rivalMoment(view, plain.copy(bluff = true), null))
        assertEquals(RivalMoment.STRONG_HAND, rivalMoment(view, plain.copy(strong = true), null))
        assertEquals(RivalMoment.TRAILING, rivalMoment(view.copy(points = mapOf(Seat.HUMAN to 6, Seat.CPU to 2)), plain, null))
        assertEquals(RivalMoment.STRONG_PLAY, rivalMoment(view.copy(current = Played(Seat.HUMAN, Card(Suit.SWORDS, 1))), plain, null))
        assertEquals(RivalMoment.LOST, rivalMoment(view, plain, Seat.HUMAN))
        assertEquals(RivalMoment.WON, rivalMoment(view, plain, Seat.CPU))
        assertNull(rivalMoment(view, plain, null))
    }

    @Test fun cooldownAndHandBudgetNeverSuppressFormalCalls() {
        val rival = TrucoDialogue(Random(7))
        assertNotNull(rival.opening(1, 0, trailing = false))
        assertNull(rival.opening(1, 25, trailing = false))
        assertEquals("¡TRUCO!", rival.line(RivalMoment.TRUCO, Call.TRUCO, 20L, chance = 1f))
        assertNotNull(rival.line(RivalMoment.STRONG_PLAY, null, 3900, chance = 1f))
        assertNull(rival.line(RivalMoment.WON, null, 15_000, chance = 1f))
        assertEquals("¡QUIERO!", rival.line(null, Call.WANT, 15_000))
        assertEquals("¡NO QUIERO!", rival.line(RivalMoment.ENVIDO, Call.REFUSE, 15_000))
    }

    @Test fun repeatedSituationUsesDifferentPhrasesAndDoesNotInterruptCurrentSubtitle() {
        val rival = TrucoDialogue(Random(18))
        val first = rival.line(RivalMoment.ENVIDO, null, 0, chance = 1f)
        val second = rival.line(RivalMoment.ENVIDO, null, 4000, chance = 1f)
        assertNotNull(first)
        assertNotEquals(first, second)
        assertNull(rival.opening(2, 4010, trailing = true, available = false))
        assertNotNull(rival.line(RivalMoment.TRAILING, null, 8100, chance = 1f))
    }

    @Test fun cardsFitTypicalGalaxyWidthsAndWelcomeLeavesSpaceForTheCaption() {
        for (screen in listOf(320f, 360f, 411f, 640f)) {
            val inner = screen - 28
            val hand = trucoHandCardWidth(inner)
            val table = trucoTableCardWidth(inner - 20)
            assertTrue(hand >= 85f)
            assertTrue(table >= 90f)
            assertTrue(3 * hand + 2 * HAND_CARD_GAP <= inner)
            assertTrue(2 * table + TABLE_CARD_GAP <= inner - 20)
        }
        assertEquals(500f, centeredWelcomeHeight(660f, 80f), .01f)
        assertEquals(140f, centeredWelcomeHeight(320f, 90f), .01f)
    }

    @Test fun displayNameFallsBackForExistingProfiles() {
        assertEquals("Sol", preferredDisplayName(" Sol ", "Apodo", "Nombre"))
        assertEquals("Apodo", preferredDisplayName("   ", " Apodo ", "Nombre"))
        assertEquals("Nombre", preferredDisplayName(null, null, "Nombre"))
        assertEquals("", preferredDisplayName(null, "", null))
    }
}
