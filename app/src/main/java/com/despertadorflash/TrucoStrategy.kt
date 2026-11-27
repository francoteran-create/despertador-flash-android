package com.despertadorflash

import kotlin.random.Random

internal enum class TrucoLevel(val title: String) {
    BEGINNER("Principiante"), NORMAL("Normal"), EXPERT("Experto")
}
internal enum class RivalMood { CALM, CHEEKY, HAPPY, WORRIED }
internal sealed interface TrucoAction {
    data class Say(val call: Call) : TrucoAction
    data class Use(val card: Card) : TrucoAction
}
internal data class RivalMove(val action: TrucoAction, val mood: RivalMood,
    val bluff: Boolean = false, val strong: Boolean = false)

internal class TrucoStrategy(private val level: TrucoLevel, private val random: Random = Random.Default) {
    fun choose(view: TrucoView): RivalMove {
        val estimate = estimate(view)
        val hand = estimate.first
        val tantos = estimate.second
        val offer = view.bid
        val bluff = random.nextFloat() < when(level) {
            TrucoLevel.BEGINNER -> .035f
            TrucoLevel.NORMAL -> .10f
            TrucoLevel.EXPERT -> .16f
        }
        val options = view.calls
        val voice = when {
            Call.FLOR in options -> Call.FLOR
            offer?.kind == BidKind.FLOR -> when {
                (flor(view.original) ?: 0) >= 33 && Call.CONTRA_RESTO in options && random.nextFloat() < .32f -> Call.CONTRA_RESTO
                (flor(view.original) ?: 0) >= 28 -> Call.WANT
                else -> Call.REFUSE
            }
            offer?.kind == BidKind.ENVIDO -> when {
                tantos > .88f && Call.FALTA in options && random.nextFloat() < .2f -> Call.FALTA
                tantos > .77f && Call.REAL in options && random.nextFloat() < .45f -> Call.REAL
                tantos + (if (bluff) .23f else 0f) > (if (offer.stake >= 5) .58f else .43f) -> Call.WANT
                else -> Call.REFUSE
            }
            offer?.kind == BidKind.TRICK -> when {
                hand + (if (bluff) .28f else 0f) > .81f && random.nextFloat() < .48f ->
                    TrucoMatch.nextTruco(offer.stake)?.takeIf { it in options } ?: Call.WANT
                hand + (if (bluff) .22f else 0f) > .42f -> Call.WANT
                else -> Call.REFUSE
            }
            Call.FALTA in options && tantos > .96f &&
                view.points.getValue(view.seat) >= view.goal - 5 && random.nextFloat() < .55f -> Call.FALTA
            Call.ENVIDO in options && (tantos > .72f || bluff) && random.nextFloat() < .54f -> Call.ENVIDO
            options.any { it in trickCalls } && (hand > .72f || bluff) && random.nextFloat() < .48f ->
                options.first { it in trickCalls }
            Call.FOLD in options && hand < .18f && view.tricks.any { it.winner == view.seat.other() } &&
                random.nextFloat() < .44f -> Call.FOLD
            else -> null
        }
        if (voice != null) return RivalMove(TrucoAction.Say(voice),
            when { voice == Call.REFUSE || voice == Call.FOLD -> RivalMood.WORRIED
                bluff -> RivalMood.CHEEKY; else -> RivalMood.HAPPY },
            bluff = bluff && (voice in trickCalls && hand <= .72f || voice == Call.ENVIDO && tantos <= .72f), strong = hand > .72f)

        val card = pickCard(view)
        return RivalMove(TrucoAction.Use(card),
            if (bluff) RivalMood.CHEEKY else if (hand < .33f) RivalMood.WORRIED else RivalMood.CALM,
            bluff = bluff, strong = hand > .72f)
    }


    private fun estimate(view: TrucoView): Pair<Float, Float> {
        val revealed = view.publicCards.filter { it.by != view.seat }.map { it.card }
        val unseen = spanishDeck - (view.original + revealed).toSet()
        val samples = when(level) { TrucoLevel.BEGINNER -> 8; TrucoLevel.NORMAL -> 22; TrucoLevel.EXPERT -> 64 }
        var wins = 0f
        var side = 0f
        val ownTantos = envido(view.original)
        val ownStrengths = view.own.map { it.strength }.sortedDescending()
        repeat(samples) {
            val candidates = unseen.shuffled(random).take(3 - revealed.size)
            val opposing = (candidates + revealed)
            val other = envido(opposing)
            if (ownTantos > other || ownTantos == other && view.mano == view.seat) side++
            val local = ownStrengths.zip(candidates.map { it.strength }.sortedDescending())
            wins += if (local.isEmpty()) .5f else local.map { (ours, theirs) ->
                if (ours > theirs) 1f else if (ours == theirs) .5f else 0f
            }.average().toFloat()
        }
        val advantage = view.tricks.count { it.winner == view.seat } - view.tricks.count { it.winner == view.seat.other() }
        return (wins / samples + .17f * advantage).coerceIn(0f,1f) to side / samples
    }

    private fun pickCard(view: TrucoView): Card {
        if (level == TrucoLevel.BEGINNER && random.nextFloat() < .22f) return view.own.random(random)
        val otherCard = view.current?.card
        if (otherCard != null) return view.own.filter { it.strength >= otherCard.strength }
            .minByOrNull { it.strength } ?: view.own.minBy { it.strength }
        val underPressure = view.tricks.any { it.winner == view.seat.other() }
        return if (underPressure || level == TrucoLevel.EXPERT && random.nextFloat() < .42f)
            view.own.maxBy { it.strength } else view.own.sortedBy { it.strength }[view.own.size / 2]
    }

    companion object { private val trickCalls = setOf(Call.TRUCO, Call.RETRUCO, Call.VALE4) }
}
