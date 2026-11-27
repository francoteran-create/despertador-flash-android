package com.despertadorflash

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NeonGamesTest {
    @get:Rule val compose = createAndroidComposeRule<NeonGamesActivity>()

    @Test fun trucoShowsItsTableAndListeningStartsOff() {
        compose.onNodeWithText("Truco argentino").performClick()
        compose.onNodeWithText("Jugar contra La Rusa").performClick()
        compose.onNodeWithText("Micrófono apagado").assertIsDisplayed()
        compose.onNodeWithText("ESCUCHAR").assertHasClickAction()
        compose.onNodeWithText("Vos  0").assertIsDisplayed()
        compose.onNodeWithText("Mano: vos").assertIsDisplayed()
    }

    @Test fun sectorOpensBoardAndExitsWithoutStartingTheMicrophone() {
        compose.onNodeWithText("Bloques de neón").performClick()
        compose.onNodeWithContentDescription("Tablero de bloques").assertIsDisplayed()
        compose.onNodeWithText("Micrófono apagado").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mover a la izquierda").performClick()
        compose.onNodeWithContentDescription("Girar pieza").performClick()
        compose.onNodeWithContentDescription("Caída instantánea").performClick()
        compose.onNodeWithContentDescription("Opciones de la partida").performClick()
        compose.onNodeWithText("Pausar").performClick()
        compose.onNodeWithContentDescription("Caída instantánea").assertIsNotEnabled()
        compose.onNodeWithText("Continuar partida").performClick()
        compose.onNodeWithContentDescription("Opciones de la partida").performClick()
        compose.onNodeWithText("Salir").performClick()
        compose.onNodeWithText("Bloques de neón").assertIsDisplayed()
    }
}
