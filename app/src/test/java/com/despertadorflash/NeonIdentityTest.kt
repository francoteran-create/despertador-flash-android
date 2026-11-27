package com.despertadorflash

import org.junit.Assert.*
import org.junit.Test

class NeonIdentityTest {
    private val portraits = List(32) { index ->
        NeonPortrait("id-$index", "Personaje $index", if (index % 2 == 0) NeonGender.MAN else NeonGender.WOMAN,
            "asset-$index")
    }
    private val catalog = NeonCatalog("logo", portraits)

    @Test fun privateChoiceIncludesAllOriginalEntries() {
        assertEquals(portraits, catalog.forGender(NeonGender.PRIVATE))
        assertEquals(portraits.filter { it.gender == NeonGender.MAN }, catalog.forGender(NeonGender.MAN))
        assertEquals(portraits.filter { it.gender == NeonGender.WOMAN }, catalog.forGender(NeonGender.WOMAN))
    }

    @Test fun nameAndGenderAreRequiredBeforeSelection() {
        assertFalse(NeonDraft(name = "Luz").canContinue())
        assertFalse(NeonDraft(NeonGender.PRIVATE, "  ").canContinue())
        assertFalse(NeonDraft(NeonGender.PRIVATE, "x".repeat(101)).canContinue())
        assertTrue(NeonDraft(NeonGender.PRIVATE, " Luz ").canContinue())
    }

    @Test fun changingGenderResetsThePortraitAndInvalidChoicesCannotFinish() {
        val draft = NeonDraft(NeonGender.MAN, "Luz", "id-0")
        assertTrue(draft.validFor(catalog))
        assertFalse(draft.copy(avatar = "id-1").validFor(catalog))
        assertEquals(draft, draft.selectGender(NeonGender.MAN))
        assertFalse(draft.selectGender(NeonGender.WOMAN).validFor(catalog))
        assertTrue(draft.copy(gender = NeonGender.PRIVATE, avatar = "id-1").validFor(catalog))
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteCatalogCannotActivateTheWelcome() {
        NeonCatalog("logo", portraits.dropLast(1))
    }
}
