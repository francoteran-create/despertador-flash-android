package com.despertadorflash

import kotlin.random.Random

internal enum class Seat { HUMAN, CPU; fun other() = if (this == HUMAN) CPU else HUMAN }
internal enum class Suit(val label: String) { SWORDS("espadas"), CLUBS("bastos"), COINS("oros"), CUPS("copas") }

internal data class Card(val suit: Suit, val number: Int) {
    val label: String get() = "$number de ${suit.label}"
    val strength: Int get() = when {
        number == 1 && suit == Suit.SWORDS -> 14
        number == 1 && suit == Suit.CLUBS -> 13
        number == 7 && suit == Suit.SWORDS -> 12
        number == 7 && suit == Suit.COINS -> 11
        number == 3 -> 10
        number == 2 -> 9
        number == 1 -> 8
        number == 12 -> 7
        number == 11 -> 6
        number == 10 -> 5
        number == 7 -> 4
        number == 6 -> 3
        number == 5 -> 2
        else -> 1
    }
    val pips get() = if (number < 10) number else 0
}

internal val spanishDeck = Suit.entries.flatMap { suit -> (1..7).plus(10..12).map { Card(suit, it) } }

internal fun envido(cards: List<Card>): Int {
    val pairs = cards.groupBy { it.suit }.values.filter { it.size >= 2 }
    return pairs.maxOfOrNull { suited -> 20 + suited.map { it.pips }.sortedDescending().take(2).sum() }
        ?: cards.maxOf { it.pips }
}

internal fun flor(cards: List<Card>): Int? =
    if (cards.size == 3 && cards.all { it.suit == cards.first().suit }) 20 + cards.sumOf { it.pips } else null

internal fun winnerOfHand(tricks: List<Seat?>, mano: Seat): Seat? {
    if (tricks.size < 2) return null
    val first = tricks[0]
    val second = tricks[1]
    if (first != null && (second == null || first == second)) return first
    if (first == null && second != null) return second
    if (tricks.size == 2) return null
    return tricks[2] ?: first ?: mano
}

internal enum class Call(val text: String) {
    ENVIDO("Envido"), REAL("Real envido"), FALTA("Falta envido"),
    TRUCO("Truco"), RETRUCO("Retruco"), VALE4("Vale cuatro"),
    FLOR("Flor"), CONTRAFLOR("Contraflor"), CONTRA_RESTO("Contraflor al resto"),
    WANT("Quiero"), REFUSE("No quiero"), FOLD("Me voy al mazo")
}

internal enum class BidKind { ENVIDO, TRICK, FLOR }
internal data class Bid(val kind: BidKind, val caller: Seat, val call: Call, val stake: Int, val refusal: Int,
    val chain: List<Call> = listOf(call))
internal data class Played(val by: Seat, val card: Card)
internal data class Trick(val plays: List<Played>, val winner: Seat?)
internal data class TrucoView(
    val seat: Seat, val own: List<Card>, val original: List<Card>, val publicCards: List<Played>,
    val points: Map<Seat, Int>, val goal: Int, val mano: Seat, val turn: Seat,
    val tricks: List<Trick>, val current: Played?, val bid: Bid?, val calls: Set<Call>,
    val handStake: Int, val opponentCount: Int
)

/** Rule state uses seat-scoped views. A future remote peer can submit the same actions. */
internal class TrucoMatch(val goal: Int, val withFlor: Boolean, private val random: Random = Random.Default) {
    init { require(goal == 15 || goal == 30) }
    var mano = Seat.HUMAN
        private set
    var turn = mano
        private set
    var matchWinner: Seat? = null
        private set
    var handWinner: Seat? = null
        private set
    var scoredSide: Seat? = null
        private set
    var resolution = ""
        private set
    var handNumber = 0
        private set
    private val scores = mutableMapOf(Seat.HUMAN to 0, Seat.CPU to 0)
    private var hands = mapOf<Seat,List<Card>>()
    private var original = mapOf<Seat,List<Card>>()
    private val tricks = ArrayList<Trick>(3)
    private var current: Played? = null
    private var trickLead = mano
    private var bid: Bid? = null
    private var suspendedTruco: Bid? = null
    private var envidoClosed = false
    private var florClosed = false
    private var stake = 1
    private var nextTrucoCaller: Seat? = null

    init { startHand() }
    fun score(seat: Seat) = scores.getValue(seat)
    fun nextHand(): Boolean {
        if (handWinner == null || matchWinner != null) return false
        mano = mano.other()
        startHand()
        return true
    }

    private fun startHand() {
        val deal = spanishDeck.shuffled(random)
        original = mapOf(mano to listOf(deal[0],deal[2],deal[4]), mano.other() to listOf(deal[1],deal[3],deal[5]))
        hands = original
        tricks.clear()
        current = null
        turn = mano
        trickLead = mano
        handWinner = null
        scoredSide = null
        resolution = ""
        bid = null
        suspendedTruco = null
        envidoClosed = false
        florClosed = false
        stake = 1
        nextTrucoCaller = null
        handNumber++
    }

    fun view(seat: Seat): TrucoView = TrucoView(seat, hands.getValue(seat), original.getValue(seat),
        tricks.flatMap { it.plays } + listOfNotNull(current), scores.toMap(), goal, mano, turn,
        tricks.toList(), current, bid, legalCalls(seat), stake, hands.getValue(seat.other()).size)

    private fun florOf(seat: Seat) = if (withFlor) flor(original.getValue(seat)) else null
    private fun mustDeclareFlor(seat: Seat) = !florClosed && florOf(seat) != null
    private fun canEnvido(seat: Seat) = !envidoClosed && florOf(seat) == null && tricks.isEmpty() && hands.getValue(seat).size == 3

    private fun legalCalls(seat: Seat): Set<Call> {
        if (seat != turn || handWinner != null || matchWinner != null) return emptySet()
        val offer = bid
        if (offer != null && offer.kind != BidKind.FLOR && mustDeclareFlor(seat)) return setOf(Call.FLOR)
        if (offer != null) return buildSet {
            add(Call.WANT); add(Call.REFUSE)
            when (offer.kind) {
                BidKind.TRICK -> {
                    nextTruco(offer.stake)?.let { add(it) }
                    if (canEnvido(seat)) addAll(envidoCalls)
                }
                BidKind.ENVIDO -> {
                    if (offer.call != Call.FALTA) {
                        add(Call.FALTA)
                        add(Call.REAL)
                        if (Call.REAL !in offer.chain) add(Call.ENVIDO)
                    }
                }
                BidKind.FLOR -> when (offer.call) {
                    Call.FLOR -> { add(Call.CONTRAFLOR); add(Call.CONTRA_RESTO) }
                    Call.CONTRAFLOR -> add(Call.CONTRA_RESTO)
                    else -> Unit
                }
            }
        }
        if (mustDeclareFlor(seat)) return setOf(Call.FLOR)
        return buildSet {
            if (canEnvido(seat)) addAll(envidoCalls)
            if (nextTrucoCaller == null || nextTrucoCaller == seat) nextTruco(stake)?.let { add(it) }
            add(Call.FOLD)
        }
    }

    fun play(seat: Seat, card: Card): Boolean {
        if (turn != seat || bid != null || handWinner != null || matchWinner != null ||
            mustDeclareFlor(seat) || card !in hands.getValue(seat)) return false
        hands = hands + (seat to (hands.getValue(seat) - card))
        val previous = current
        if (previous == null) {
            current = Played(seat, card)
            turn = seat.other()
        } else {
            val victor = when {
                previous.card.strength > card.strength -> previous.by
                previous.card.strength < card.strength -> seat
                else -> null
            }
            tricks.add(Trick(listOf(previous, Played(seat, card)), victor))
            current = null
            winnerOfHand(tricks.map { it.winner }, mano)?.let { finishHand(it, stake, "Ganó la mano") }
            if (victor != null) trickLead = victor
            turn = trickLead
        }
        return true
    }

    fun call(seat: Seat, action: Call): Boolean {
        if (action !in legalCalls(seat)) return false
        val offer = bid
        when (action) {
            Call.WANT, Call.REFUSE -> respond(seat, offer!!, action == Call.WANT)
            Call.FOLD -> finishHand(seat.other(), stake, "Al mazo")
            Call.TRUCO, Call.RETRUCO, Call.VALE4 -> {
                val value = when(action) { Call.TRUCO -> 2; Call.RETRUCO -> 3; else -> 4 }
                bid = Bid(BidKind.TRICK, seat, action, value, value - 1)
                turn = seat.other()
            }
            Call.ENVIDO, Call.REAL, Call.FALTA -> {
                if (offer?.kind != BidKind.ENVIDO) {
                    resumeTurn = playTurn()
                    suspendedTruco = offer
                }
                val earlier = if (offer?.kind == BidKind.ENVIDO) offer else null
                val value = when(action) {
                    Call.ENVIDO -> (earlier?.stake ?: 0) + 2
                    Call.REAL -> (earlier?.stake ?: 0) + 3
                    else -> faltaPoints(goal, scores.values.max())
                }
                bid = Bid(BidKind.ENVIDO, seat, action, value, earlier?.stake ?: 1,
                    earlier?.chain.orEmpty() + action)
                turn = seat.other()
            }
            Call.FLOR -> {
                envidoClosed = true
                if (offer?.kind == BidKind.TRICK) suspendedTruco = offer
                if (florOf(seat.other()) == null) {
                    florClosed = true
                    award(seat, 3, "Flor", side = true)
                    bid = suspendedTruco
                    suspendedTruco = null
                    turn = bid?.caller?.other() ?: playTurn()
                } else {
                    resumeTurn = playTurn()
                    bid = Bid(BidKind.FLOR, seat, action, 6, 4)
                    turn = seat.other()
                }
            }
            Call.CONTRAFLOR, Call.CONTRA_RESTO -> {
                bid = Bid(BidKind.FLOR, seat, action,
                    if (action == Call.CONTRAFLOR) 6 else goal - scores.values.max() + 6,
                    4)
                turn = seat.other()
            }
        }
        return true
    }

    private var resumeTurn = mano
    private fun playTurn() = current?.by?.other() ?: trickLead

    private fun respond(seat: Seat, offer: Bid, accepted: Boolean) {
        bid = null
        if (offer.kind == BidKind.TRICK) {
            if (!accepted) finishHand(offer.caller, offer.refusal, "Truco no querido")
            else {
                stake = offer.stake
                nextTrucoCaller = seat
                turn = playTurn()
            }
            return
        }
        val value = if (offer.kind == BidKind.FLOR) ::florOf else { who -> envido(original.getValue(who)) }
        val human = requireNotNull(value(Seat.HUMAN))
        val cpu = requireNotNull(value(Seat.CPU))
        val winner = if (!accepted) offer.caller else when { human > cpu -> Seat.HUMAN; cpu > human -> Seat.CPU; else -> mano }
        val title = if (offer.kind == BidKind.FLOR) "Flor" else "Envido"
        val reveal = if (accepted) " · $human a $cpu" else " no querido"
        envidoClosed = true
        if (offer.kind == BidKind.FLOR) florClosed = true
        award(winner, if (accepted) offer.stake else offer.refusal, title + reveal, side = true)
        bid = suspendedTruco
        suspendedTruco = null
        turn = bid?.caller?.other() ?: resumeTurn
    }

    private fun award(seat: Seat, amount: Int, title: String, side: Boolean = false) {
        scores[seat] = minOf(goal, score(seat) + amount)
        if (side) scoredSide = seat
        resolution = "$title · +$amount para " + if (seat == Seat.HUMAN) "vos" else "la mesa"
        if (score(seat) >= goal) matchWinner = seat
    }

    private fun finishHand(seat: Seat, amount: Int, title: String) {
        handWinner = seat
        bid = null
        award(seat, amount, title)
    }

    companion object {
        private val envidoCalls = setOf(Call.ENVIDO, Call.REAL, Call.FALTA)
        fun nextTruco(value: Int) = when(value) { 1 -> Call.TRUCO; 2 -> Call.RETRUCO; 3 -> Call.VALE4; else -> null }
    }
}

internal fun faltaPoints(goal: Int, leading: Int): Int =
    (if (goal == 30 && leading < 15) 15 - leading else goal - leading).coerceAtLeast(1)
