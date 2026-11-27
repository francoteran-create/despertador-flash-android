package com.despertadorflash

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SoundStageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun controlsAreAccessibleAndStartAndStopOneSession() {
        val listening = mutableStateOf(false)
        val actions = mutableListOf<String>()
        val wave = mutableStateOf(FloatArray(48))
        compose.setContent {
            SoundStageScreen(listening.value, "", "", emptyList(), wave, false) {
                actions += it
                listening.value = it == "start"
            }
        }
        compose.onNodeWithContentDescription("Iniciar escucha").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Iniciar escucha").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Detener escucha").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Detener escucha").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(listOf("start", "stop"), actions) }
    }

    @Test fun secondaryMenuRoutesHistorySensitivityAndPreferences() {
        val actions = mutableListOf<String>()
        val wave = mutableStateOf(FloatArray(48))
        compose.setContent { SoundStageScreen(false, "", "", emptyList(), wave, false) { actions += it } }
        listOf("Historial de sonidos", "Sensibilidad", "Preferencias").forEach { title ->
            compose.onNodeWithContentDescription("Opciones de sonidos").performClick()
            compose.onNodeWithText(title).performScrollTo().performClick()
        }
        compose.runOnIdle { assertEquals(listOf("history", "sensitivity", "preferences"), actions) }
    }

    @Test fun possibleHypothesesDoNotDisplayAsHigherConfidence() {
        val reading = mutableStateOf(SoundStageReading("POSIBLE TIMBRE DE PUERTA", 90, false))
        val wave = mutableStateOf(FloatArray(48))
        compose.setContent { SoundStageScreen(true, "Escuchando", "", listOf(reading.value), wave, false) {} }
        compose.onNodeWithText("Detección probable · IA 90/100").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { reading.value = reading.value.copy(accepted = true) }
        compose.onNodeWithText("Mayor confianza · IA 90/100").assertIsDisplayed()
    }
}
