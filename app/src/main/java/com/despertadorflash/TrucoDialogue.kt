package com.despertadorflash

import java.util.Locale
import kotlin.random.Random

internal enum class RivalMoment {
    OPENING, ENVIDO, TRUCO, STRONG_PLAY, WON, LOST, BLUFF, STRONG_HAND, TRAILING
}

internal fun rivalMoment(view: TrucoView, move: RivalMove, result: Seat?): RivalMoment? {
    val call = (move.action as? TrucoAction.Say)?.call
    return when {
        result != null -> if (result == view.seat) RivalMoment.WON else RivalMoment.LOST
        move.bluff -> RivalMoment.BLUFF
        call in setOf(Call.ENVIDO, Call.REAL, Call.FALTA, Call.FLOR, Call.CONTRAFLOR, Call.CONTRA_RESTO) ||
            call in replies && view.bid?.kind in setOf(BidKind.ENVIDO, BidKind.FLOR) -> RivalMoment.ENVIDO
        call in setOf(Call.TRUCO, Call.RETRUCO, Call.VALE4) ||
            call in replies && view.bid?.kind == BidKind.TRICK -> RivalMoment.TRUCO
        (view.current?.card?.strength ?: 0) >= 9 -> RivalMoment.STRONG_PLAY
        view.points.getValue(view.seat.other()) - view.points.getValue(view.seat) >= 3 -> RivalMoment.TRAILING
        move.strong -> RivalMoment.STRONG_HAND
        else -> null
    }
}

private val replies = setOf(Call.WANT, Call.REFUSE)

internal class TrucoDialogue(private val random: Random = Random.Default) {
    private val recent = ArrayDeque<String>()
    private var hand = 0
    private var flavors = 0
    private var lastFlavor = Long.MIN_VALUE

    fun opening(handNumber: Int, now: Long, trailing: Boolean, available: Boolean = true): String? {
        if (hand == handNumber) return null
        hand = handNumber
        flavors = 0
        if (!available || handNumber != 1 && random.nextFloat() >= .41f) return null
        return line(if (trailing) RivalMoment.TRAILING else RivalMoment.OPENING, null, now, 1f)
    }

    /** The formal call always survives a cooldown. Optional flavor shares its subtitle. */
    fun line(moment: RivalMoment?, call: Call?, now: Long, chance: Float = if (call != null) .38f else .56f): String? {
        val formal = call?.let { "¡${it.text.uppercase(Locale.ROOT)}!" }
        if (moment == null || flavors >= 2 || lastFlavor != Long.MIN_VALUE && now - lastFlavor < 3800L ||
            random.nextFloat() >= chance) return formal
        val pool = repertoire.getValue(moment)
        val flavor = (pool - recent.toSet()).ifEmpty { pool.filter { it != recent.lastOrNull() } }.random(random)
        flavors++
        lastFlavor = now
        recent.addLast(flavor)
        if (recent.size > 7) recent.removeFirst()
        return listOfNotNull(formal, flavor).joinToString(" ")
    }

    companion object {
        private val repertoire = mapOf(
            RivalMoment.OPENING to listOf("Dale, animate.", "Barajá esas ganas.", "Te estoy esperando.",
                "A ver si te plantás.", "No me regalés la mano.", "Que arranque la payada."),
            RivalMoment.ENVIDO to listOf("¿Trajiste tantos?", "Qué picardía…", "Vamos a ver si te da.",
                "Mirá esos tantos.", "A ver qué guardás.", "Ahí hay porotos."),
            RivalMoment.TRUCO to listOf("No me asustás.", "¿Y ahora qué hacés?", "Te la jugaste…",
                "Uh, se puso lindo.", "A ver si lo bancás.", "Cantar es fácil.", "Plantate, che."),
            RivalMoment.STRONG_PLAY to listOf("Esa no me la esperaba.", "Ahí me quisiste apretar.",
                "Linda carta, eh.", "Eso estuvo cerca.", "Me hiciste pensar.", "Mirá vos."),
            RivalMoment.WON to listOf("¡Te enganché!", "¿Eso era todo?", "Bien peleada, eh.",
                "Esta queda de mi lado.", "Te la guardé para el final.", "Acá junté porotos."),
            RivalMoment.LOST to listOf("Bien jugado.", "Linda mano, eh.", "Me sacaste el sombrero.",
                "Por poquito, che.", "Esa te la ganaste.", "Habrá revancha."),
            RivalMoment.BLUFF to listOf("¿Con eso me venís?", "Mirá bien lo que hacés.",
                "Esta te va a doler.", "Yo no cantaría eso.", "Te veo flojo.", "¿Te animás a querer?"),
            RivalMoment.STRONG_HAND to listOf("Hoy ligué lindo.", "Me gusta cómo pinta.",
                "Tengo con qué bancarla.", "No me apures, che.", "Todavía hay tela."),
            RivalMoment.TRAILING to listOf("Todavía hay partido.", "No cuentes los porotos antes.",
                "La vuelta recién empieza.", "No te agrandés.", "Vamos a remontarla.", "Falta el último canto.")
        )
    }
}
