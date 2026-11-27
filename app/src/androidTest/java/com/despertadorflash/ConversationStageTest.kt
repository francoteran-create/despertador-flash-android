package com.despertadorflash

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConversationStageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun transcriptAndProfileAvatarAreVisibleWithTranslationInMenu() {
        val selectedAvatar = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val actions = mutableListOf<String>()
        compose.setContent {
            ConversationScreen(ConversationUi(transcript = "Nos vemos mañana"), selectedAvatar, "Luz") {
                actions += it
            }
        }
        compose.onNodeWithText("Nos vemos mañana").assertIsDisplayed()
        compose.onNodeWithContentDescription("Avatar del perfil", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Traducción").assertDoesNotExist()
        compose.onNodeWithContentDescription("Opciones de transcripción").performClick()
        compose.onNodeWithText("Traducción").performClick()
        compose.runOnIdle { assertEquals(listOf("translation"), actions) }
    }

    @Test fun translationAppearsOnlyWhenRequestedAndListenButtonRoutes() {
        val screen = mutableStateOf(ConversationUi(transcript = "Hola", translated = "Hello"))
        val actions = mutableListOf<String>()
        compose.setContent { ConversationScreen(screen.value, null, "Luz") { actions += it } }
        compose.onNodeWithText("Hello").assertDoesNotExist()
        compose.onNodeWithText("Transcribir").performClick()
        compose.runOnIdle { screen.value = screen.value.copy(listening = true, showTranslation = true) }
        compose.onNodeWithText("Hello").assertIsDisplayed()
        compose.onNodeWithText("Detener").performClick()
        compose.runOnIdle { assertEquals(listOf("start", "stop"), actions) }
    }
}
