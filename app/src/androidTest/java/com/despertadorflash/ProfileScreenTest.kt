package com.despertadorflash

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ProfileActivity>()

    private var usingEmptyProfile = false

    @Before
    fun requireDisposableProfile() {
        var empty = false
        compose.activityRule.scenario.onActivity { activity ->
            empty = activity.getSharedPreferences("aurora_profile", 0).all.isEmpty() &&
                activity.filesDir.listFiles().orEmpty().none { it.name.startsWith("aurora_profile_photo.jpg") }
        }
        assumeTrue("Usá una instalación de prueba con el perfil vacío.", empty)
        usingEmptyProfile = true
    }

    @After
    fun clearTestData() {
        if (usingEmptyProfile) {
            compose.activityRule.scenario.onActivity { activity ->
                activity.getSharedPreferences("aurora_profile", 0).edit().clear().commit()
            }
        }
    }

    @Test
    fun savesProfileAndRestoresItAfterRecreation() {
        compose.onNodeWithTag("profileName").performScrollTo().performTextInput("María")
        compose.onNodeWithTag("profileNickname").performScrollTo().performTextInput("Luz")
        compose.onNodeWithText("Más datos personales").performScrollTo().performClick()
        compose.onNodeWithTag("profileContact").performScrollTo().performTextInput("Contacto")
        compose.onNodeWithText("Accesibilidad y preferencias").performScrollTo().performClick()
        compose.onNodeWithTag("profileCud").performScrollTo().performClick()
        compose.onNodeWithText("En trámite").performClick()
        compose.onNodeWithTag("profileNotes").performScrollTo().performTextInput("Prefiero texto")
        compose.onNodeWithText("Guardar mi perfil").performScrollTo().performClick()
        compose.onNodeWithText("Perfil guardado en este teléfono").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.onActivity { activity ->
            val saved = activity.getSharedPreferences("aurora_profile", 0)
            assertEquals("María", saved.getString("name", null))
            assertEquals("Luz", saved.getString("nickname", null))
            assertEquals("Contacto", saved.getString("contact", null))
            assertEquals("Prefiero texto", saved.getString("notes", null))
            assertEquals(3, saved.getInt("cud", -1))
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("profileName").performScrollTo().assertTextContains("María")
        compose.onNodeWithTag("profileNickname").performScrollTo().assertTextContains("Luz")
        compose.onNodeWithTag("profileContact").performScrollTo().assertTextContains("Contacto")
        compose.onNodeWithText("En trámite").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("profileNotes").performScrollTo().assertTextContains("Prefiero texto")
    }

    @Test
    fun preservesUnsavedDraftOnRecreation() {
        compose.onNodeWithTag("profileName").performScrollTo().performTextInput("Borrador")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("profileName").performScrollTo().assertTextContains("Borrador")
    }

    @Test
    fun avatarSelectionAndDeleteConfirmation() {
        compose.onNodeWithText("Foto y avatar").performScrollTo().performClick()
        compose.onNodeWithText("Personalizar avatar").performScrollTo().performClick()
        compose.onNodeWithText("Avatar con figura").performScrollTo().performClick()
        compose.onNodeWithText("Avatar actualizado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("profileName").performScrollTo().performTextInput("Luz")
        compose.onNodeWithText("Privacidad").performScrollTo().performClick()
        compose.onNodeWithText("Borrar datos del perfil").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithTag("profileName").performScrollTo().assertTextContains("Luz")
        compose.onNodeWithText("Borrar datos del perfil").performScrollTo().performClick()
        compose.onNodeWithText("Borrar", substring = false).performClick()
        compose.onNodeWithText("Perfil borrado").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(emptyMap<String, Any>(), activity.getSharedPreferences("aurora_profile", 0).all)
        }
    }
}
