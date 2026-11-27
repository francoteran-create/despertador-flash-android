package com.despertadorflash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NeonWelcomeTest {
    @get:Rule val compose = createComposeRule()
    private val catalog = NeonCatalog("unused-logo", List(32) { index ->
        NeonPortrait("id-$index", "Personaje ${index + 1}",
            if (index % 2 == 0) NeonGender.MAN else NeonGender.WOMAN, "unused-$index")
    })

    @Test fun privateChoiceAllowsBrowsingAllAndCompletingLater() {
        verifyFlow(NeonGender.PRIVATE, "1 de 32", "2 de 32", "id-1", "DEJARLO PARA DESPUÉS", false)
    }

    @Test fun filteredCarouselAllowsOpeningTheProfile() {
        verifyFlow(NeonGender.WOMAN, "1 de 16", "2 de 16", "id-3", "COMPLETAR PERFIL", true)
    }

    private fun verifyFlow(
        gender: NeonGender, initialCount: String, nextCount: String, id: String, finishLabel: String, open: Boolean
    ) {
        var result: Pair<NeonDraft, Boolean>? = null
        compose.setContent { NeonWelcome(catalog, "") { draft, openProfile -> result = draft to openProfile } }
        compose.onNodeWithText("EMPEZAR").performClick()
        compose.onNodeWithText(gender.label).performClick()
        compose.onNodeWithText("Nombre visible").performTextInput("Luz")
        compose.onNodeWithText("CONTINUAR").performClick()
        compose.onNodeWithText(initialCount).assertIsDisplayed()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText(nextCount).assertIsDisplayed()
        compose.onNodeWithText("Elegí tu avatar").assertIsDisplayed()
        compose.onNodeWithText("CONTINUAR").performClick()
        compose.onNodeWithText(finishLabel).performClick()
        compose.runOnIdle { assertEquals(NeonDraft(gender, "Luz", id) to open, result) }
    }
}
