package com.despertadorflash

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NeonWelcomeTest {
    @get:Rule val compose = createComposeRule()
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val catalog get() = requireNotNull(NeonIdentity.catalog(target))

    @Test fun privateChoiceAllowsSwipingBothWaysAndCompletingLater() {
        var result: Pair<NeonDraft, Boolean>? = null
        compose.setContent { NeonWelcome(catalog, NeonDraft()) { draft, open -> result = draft to open } }
        enterName(NeonGender.PRIVATE)
        compose.onNodeWithContentDescription("AURORA").assertIsDisplayed()
        compose.onNodeWithText("1 de 32").assertIsDisplayed()
        compose.onNodeWithTag("neonAvatarPager").performTouchInput { swipeLeft() }
        compose.onNodeWithText("2 de 32").assertIsDisplayed()
        compose.onNodeWithTag("neonAvatarPager").performTouchInput { swipeRight() }
        compose.onNodeWithText("1 de 32").assertIsDisplayed()
        compose.onNodeWithText("Elegí tu avatar").assertIsDisplayed()
        compose.onNodeWithText("CONTINUAR").assertHasClickAction().performClick()
        compose.onNodeWithText("DEJARLO PARA DESPUÉS").performClick()
        compose.runOnIdle { assertEquals(NeonDraft(NeonGender.PRIVATE, "Luz", catalog.portraits.first().id) to false, result) }
    }

    @Test fun filteredCarouselSupportsAccessibleArrowsAndOpeningTheProfile() {
        var result: Pair<NeonDraft, Boolean>? = null
        compose.setContent { NeonWelcome(catalog, NeonDraft()) { draft, open -> result = draft to open } }
        enterName(NeonGender.WOMAN)
        compose.onNodeWithText("1 de 20").assertIsDisplayed()
        compose.onNodeWithText("Anterior").assertIsNotEnabled()
        compose.onNodeWithText("Siguiente").assertHasClickAction().performClick()
        compose.onNodeWithText("2 de 20").assertIsDisplayed()
        compose.onNodeWithText("CONTINUAR").performClick()
        compose.onNodeWithText("COMPLETAR PERFIL").performClick()
        compose.runOnIdle {
            assertEquals(NeonDraft(NeonGender.WOMAN, "Luz", catalog.forGender(NeonGender.WOMAN)[1].id) to true, result)
        }
    }

    @Test fun identityIsPersistedWithoutErasingTheProfileOrReopeningWelcome() {
        val prefsName = "neon_instrumentation_test"
        val isolated = object : ContextWrapper(target) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(prefsName, mode)
        }
        val prefs = NeonIdentity.preferences(isolated)
        prefs.edit().clear().putString("contact", "Contacto de prueba").commit()
        try {
            assertTrue(NeonIdentity.shouldWelcome(isolated))
            val first = catalog.portraits.first()
            val saved = NeonDraft(NeonGender.PRIVATE, "Luz", first.id)
            NeonIdentity.save(isolated, saved)
            assertEquals(saved, NeonIdentity.savedDraft(isolated))
            assertFalse(prefs.contains("neon_gender"))
            assertEquals("Contacto de prueba", prefs.getString("contact", null))
            assertFalse(NeonIdentity.shouldWelcome(isolated))
        } finally {
            target.deleteSharedPreferences(prefsName)
        }
    }

    @Test fun cancelInEditorDoesNotConfirmChanges() {
        var saved = false
        var cancelled = false
        val initial = NeonDraft(NeonGender.PRIVATE, "Luz", catalog.portraits.first().id)
        compose.setContent {
            NeonWelcome(catalog, initial, editing = true, onCancel = { cancelled = true }) { _, _ -> saved = true }
        }
        compose.onNodeWithText("SOY HOMBRE").performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle {
            assertTrue(cancelled)
            assertFalse(saved)
        }
    }

    private fun enterName(gender: NeonGender) {
        compose.onNodeWithText("EMPEZAR").performClick()
        compose.onNodeWithText(gender.label).performClick()
        compose.onNodeWithText("Nombre visible").performTextInput("Luz")
        compose.onNodeWithText("CONTINUAR").performClick()
    }
}
