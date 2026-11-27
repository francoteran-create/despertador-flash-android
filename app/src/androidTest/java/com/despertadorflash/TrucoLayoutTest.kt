package com.despertadorflash

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrucoLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cardsFitInANarrowTableWithSeparateCaptionAndScore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("truco_layout_test", Context.MODE_PRIVATE)
        val session = TrucoSession().apply {
            match = TrucoMatch(15, false, Random(4))
            val table = requireNotNull(match)
            table.play(Seat.HUMAN, table.view(Seat.HUMAN).own.first())
            paused = true
        }
        val played = requireNotNull(session.match?.view(Seat.HUMAN)?.current).card.label
        compose.setContent {
            Box(Modifier.size(320.dp, 620.dp)) {
                TrucoScreen(session, prefs, "Micrófono apagado", "", false, {}, {}, { _, _ -> })
            }
        }
        val card = compose.onNodeWithContentDescription(played).getUnclippedBoundsInRoot()
        val score = compose.onNodeWithTag("trucoScore").getUnclippedBoundsInRoot()
        val caption = compose.onNodeWithTag("trucoCaption").getUnclippedBoundsInRoot()
        assertTrue(card.right - card.left >= 90.dp)
        assertTrue(score.bottom <= caption.top)
        assertTrue(caption.bottom <= card.top)
    }

    @Test fun greetingKeepsPortraitCenteredAndDoesNotCoverIt() {
        compose.setContent {
            val bitmap = remember { Bitmap.createBitmap(72, 90, Bitmap.Config.ARGB_8888) }
            Box(Modifier.size(360.dp, 620.dp)) { FullAvatarGreeting(bitmap, "Luz", NeonGender.WOMAN, false) }
        }
        val image = compose.onNodeWithTag("fullWelcomeAvatar").getUnclippedBoundsInRoot()
        val caption = compose.onNodeWithTag("welcomeCaption").getUnclippedBoundsInRoot()
        assertTrue(image.right - image.left >= 305.dp)
        assertTrue(image.bottom <= caption.top)
        compose.onNodeWithText("BIENVENIDA").assertExists()
        compose.onNodeWithText("Luz").assertExists()
    }
}
