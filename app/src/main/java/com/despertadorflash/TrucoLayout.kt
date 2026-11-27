package com.despertadorflash

internal const val HAND_CARD_GAP = 8f
internal const val TABLE_CARD_GAP = 14f

/** Dp only. Reserve all three hand slots so cards keep their size as they are played. */
internal fun trucoHandCardWidth(available: Float) =
    ((available - 2 * HAND_CARD_GAP) / 3).coerceIn(1f, 91f)

internal fun trucoTableCardWidth(available: Float) =
    ((available - TABLE_CARD_GAP) / 2).coerceIn(1f, if (available < 315f) 91f else 105f)

internal fun centeredWelcomeHeight(total: Float, caption: Float) =
    (total - 2 * caption).coerceAtLeast(1f)
