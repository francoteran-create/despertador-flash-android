package com.despertadorflash

import org.junit.Assert.*
import org.junit.Test

class SoundStagePolicyTest {
    @Test fun motionSignalIsBoundedAndQuietWithoutInput() {
        assertArrayEquals(FloatArray(48), soundWave(FloatArray(0)), 0f)
        assertTrue(soundWave(floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY)).all { it == 0f })
        val signal = soundWave(FloatArray(2048) { if (it < 1024) -.9f else .8f })
        assertEquals(-1f, signal.first(), 0f)
        assertEquals(1f, signal.last(), 0f)
        assertTrue(signal.all { it in -1f..1f })
    }

    @Test fun classifierScoreAloneDoesNotEarnHigherConfidence() {
        assertFalse(SoundStageReading("POSIBLE PERRO", 97, false).higherConfidence)
        assertFalse(SoundStageReading("POSIBLE PERRO", 64, true).higherConfidence)
        val higher = SoundStageReading("POSIBLE PERRO", 65, true)
        assertTrue(higher.higherConfidence)
        assertEquals("PERRO", higher.title)
        assertTrue(higher.qualifier.contains("Mayor confianza"))
    }

    @Test fun distinctSoundFamiliesUseDistinctArtwork() {
        assertEquals(SoundArt.BELL, soundArt("POSIBLE TIMBRE DE PUERTA"))
        assertEquals(SoundArt.DOOR, soundArt("POSIBLE PORTAZO"))
        assertEquals(SoundArt.DOG, soundArt("POSIBLE PERRO LADRANDO"))
        assertEquals(SoundArt.SIREN, soundArt("POSIBLE ALARMA DE INCENDIO"))
        assertEquals(SoundArt.HORN, soundArt("POSIBLE BOCINA"))
        assertEquals(SoundArt.CRY, soundArt("POSIBLE LLANTO"))
        assertEquals(SoundArt.PHONE, soundArt("POSIBLE TELÉFONO"))
        assertEquals(SoundArt.PEOPLE, soundArt("POSIBLE CONVERSACIÓN"))
        assertEquals(SoundArt.UNKNOWN, soundArt("POSIBLE VIENTO"))
    }
}
