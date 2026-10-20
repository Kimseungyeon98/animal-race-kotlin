package com.example.animalrace.race

import com.example.animalrace.model.Animal
import com.example.animalrace.model.Animal.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** 트랙 길이(시뮬레이션 단위). 1착이 보통 17~19초에 들어오도록 속도 곡선을 맞춰 둠. */
const val TRACK = 1000.0

enum class EventKind { SKILL, LEAD, FINISH }

data class RaceEvent(val id: Int, val t: Double, val text: String, val kind: EventKind)

/** 화면에 그릴 한 프레임의 불변 스냅샷. */
data class RacerView(
    val lane: Int,
    val animal: Animal,
    val x: Double,
    val rank: Int,
    val fx: String?,
    val stopped: Boolean,
    val boosted: Boolean,
    val finished: Boolean,
)

data class RaceView(
    val t: Double,
    val racers: List<RacerView>,
    val recentEvents: List<RaceEvent>,
    val leadChanges: Int,
    val done: Boolean,
)

internal class Effect(val mult: Double, val from: Double, val until: Double, val icon: String)

class Racer internal constructor(val lane: Int, val animal: Animal, rnd: Random) {
    var x = 0.0
        internal set
    var finish: Double? = null
        internal set
    var uses = 0
        internal set
    internal var v = 0.0
    internal var rank = lane
    internal var fx: String? = null
    internal var mult = 1.0
    internal var cd = 1 + rnd.nextDouble() * 2
    internal val cond = 0.94 + rnd.nextDouble() * 0.12
    internal var noise = 1.0
    internal var noiseT = 0.0
    internal var flag = false
    internal val effects = mutableListOf<Effect>()
    val finished get() = finish != null
}

/**
 * 경주 시뮬레이션. UI와 무관한 순수 Kotlin이라 JVM에서 그대로 테스트할 수 있다.
 * 매 [step]마다 기본 속도가 올라가고(후반 가속), 뒤처질수록 보정을 받으며(역전 유도),
 * 동물별 스킬이 확률적으로 발동한다.
 */
class Race(animals: List<Animal>, private val rnd: Random = Random.Default) {
    val racers = animals.mapIndexed { i, a -> Racer(i, a, rnd) }
    var t = 0.0
        private set
    var done = false
        private set
    var leadChanges = 0
        private set
    private var finishCount = 0
    private var leader: Racer? = null
    private val events = mutableListOf<RaceEvent>()

    fun ranking(): List<Racer> =
        racers.sortedWith(compareBy<Racer>({ it.finish ?: Double.MAX_VALUE }, { -it.x }))

    fun step(dt: Double) {
        if (done) return
        t += dt
        ranking().forEachIndexed { i, r -> r.rank = i }
        val active = racers.filter { !it.finished }
        val leadX = active.maxOf { it.x }
        val base = 24 + 3 * t

        for (r in active) {
            if (t >= r.noiseT) {
                val spread = if (r.animal == TURTLE) 0.1 else 0.25
                r.noise = 1 - spread + rnd.nextDouble() * spread * 2
                r.noiseT = t + 0.6 + rnd.nextDouble() * 0.9
            }
            r.cd -= dt
            tick(r)
            val rate = skillRate(r.animal)
            if (rate > 0 && r.cd <= 0 && t > 1.5 && rnd.nextDouble() < rate * dt && act(r)) {
                r.cd = cooldown(r.animal)
                r.uses++
            }

            var m = 1.0
            r.fx = null
            for (e in r.effects) if (t >= e.from && t < e.until) { m *= e.mult; r.fx = e.icon }
            r.effects.removeAll { it.until <= t }
            r.mult = m

            val rubber = 1 + min(0.35, (leadX - r.x) / 500)
            r.v = base * r.animal.speed * trait(r) * r.cond * r.noise * m * rubber
            r.x += r.v * dt
            if (r.x >= TRACK) {
                r.finish = t - (r.x - TRACK) / max(r.v, 1.0)
                r.x = TRACK
                r.fx = null
                finishCount++
                if (finishCount == 1) say("🏁 1착! ${tag(r)}", EventKind.FINISH)
            }
        }

        if (finishCount == 0) {
            val lead = active.maxBy { it.x }
            val current = leader
            if (current == null) leader = lead
            else if (lead !== current && lead.x - current.x > 6) {
                leader = lead
                if (t > 2) { leadChanges++; say("선두 교체! ${tag(lead)}", EventKind.LEAD) }
            }
        }
        if (t > 90) racers.filter { !it.finished }.forEach { it.finish = 90 + (TRACK - it.x) / 100 }
        if (racers.all { it.finished }) done = true
    }

    fun view(): RaceView {
        val order = ranking()
        return RaceView(
            t = t,
            racers = racers.map { r ->
                RacerView(
                    lane = r.lane, animal = r.animal, x = r.x, rank = order.indexOf(r),
                    fx = r.fx, stopped = !r.finished && r.mult == 0.0,
                    boosted = !r.finished && r.mult > 1.05, finished = r.finished,
                )
            },
            recentEvents = events.takeLast(5).reversed(),
            leadChanges = leadChanges,
            done = done,
        )
    }

    // ---------- 동물별 특성 ----------

    private fun trait(r: Racer): Double = when (r.animal) {
        HORSE -> if (r.x > TRACK * 0.65) 1.27 else 0.96
        RABBIT -> 1.1
        TURTLE -> 0.76 + min(0.62, t * 0.032)
        CAT -> 1.04
        PIG, DOG, ELEPHANT, CHICKEN -> 0.98
        PENGUIN -> 0.97
        KANGAROO -> 0.95
        SNAIL -> 0.8
        CHEETAH, MONKEY, DUCK -> 1.0
    }

    private fun skillRate(a: Animal): Double = when (a) {
        CHEETAH -> 0.2; RABBIT -> 0.5; DOG -> 0.25; CAT -> 0.28; PIG -> 0.16
        MONKEY -> 0.24; PENGUIN -> 0.2; KANGAROO -> 0.3; ELEPHANT -> 0.16
        DUCK -> 0.15; SNAIL -> 0.14; CHICKEN -> 0.28
        HORSE, TURTLE -> 0.0
    }

    private fun cooldown(a: Animal): Double = when (a) {
        CHEETAH, RABBIT -> 4.5; DOG -> 3.5; CAT, CHICKEN -> 3.0; PIG, ELEPHANT, DUCK -> 5.0
        MONKEY, PENGUIN -> 4.0; KANGAROO -> 2.5; SNAIL -> 99.0
        HORSE, TURTLE -> 0.0
    }

    /** 매 스텝 확인하는 지속형 특성. */
    private fun tick(r: Racer) {
        when (r.animal) {
            HORSE -> if (!r.flag && r.x > TRACK * 0.65) {
                r.flag = true; r.uses++
                addFx(r, 1.0, 2.0, "⚡"); say("${tag(r)} 막판 스퍼트 시작!")
            }
            SNAIL -> if (!r.flag && t > 13) { act(r); r.uses++ }
            else -> Unit
        }
    }

    /** 확률 발동형 스킬. 조건이 안 맞아 발동하지 않았으면 false. */
    private fun act(r: Racer): Boolean {
        val active = racers.filter { !it.finished }
        when (r.animal) {
            CHEETAH -> {
                addFx(r, 2.1, 1.2, "🔥"); addFx(r, 0.5, 1.6, "😮‍💨", delay = 1.2)
                say("${tag(r)} 폭발 질주!")
            }
            RABBIT -> {
                if (r.rank != 0 || active.any { it !== r && r.x - it.x < 35 }) return false
                addFx(r, 0.0, 1.5, "💤"); say("${tag(r)} 너무 앞서서 방심, 낮잠…")
            }
            DOG -> {
                if (r.rank < ceil(racers.size / 2.0)) return false
                addFx(r, 1.65, 1.6, "👃"); say("${tag(r)} 추격 본능 발동!")
            }
            CAT -> if (rnd.nextBoolean()) {
                addFx(r, 1.6, 1.0, "😼"); say("${tag(r)} 갑자기 전력질주!")
            } else {
                addFx(r, 0.2, 0.9, "🧶"); say("${tag(r)} 멈춰서 그루밍 중…")
            }
            PIG -> {
                addFx(r, 0.0, 1.0, "🍎"); addFx(r, 2.1, 1.8, "💪", delay = 1.0)
                say("${tag(r)} 간식 발견! 먹고 돌진")
            }
            MONKEY -> {
                val target = active.filter { it !== r && it.x > r.x && it.x - r.x < 260 }.minByOrNull { it.x }
                    ?: return false
                say("${tag(r)} 바나나 투척 → ${withSubject(tag(target))} 미끄러짐!")
                addFx(target, 0.0, 1.1, "🍌", debuff = true)
            }
            PENGUIN -> { addFx(r, 1.75, 2.0, "🧊"); say("${tag(r)} 배 슬라이딩!") }
            KANGAROO -> {
                r.x += 45; addFx(r, 1.0, 0.5, "⤴️")
                if (rnd.nextDouble() < 0.35) say("${tag(r)} 껑충!")
            }
            ELEPHANT -> {
                val hit = active.filter { it !== r && abs(it.x - r.x) < 150 }
                if (hit.isEmpty()) return false
                say("${tag(r)} 쿵쿵! 주변 ${hit.size}마리 휘청")
                hit.forEach { addFx(it, 0.45, 1.3, "💫", debuff = true) }
            }
            DUCK -> {
                val lead = active.maxBy { it.x }
                if (lead === r) return false
                say("${tag(r)} 꽥!! ${withSubject(tag(lead))} 깜짝 놀람")
                addFx(lead, 0.0, 1.2, "😱", debuff = true)
            }
            SNAIL -> {
                if (t < 6 || r.flag) return false
                r.flag = true
                addFx(r, 2.2, 2.6, "🚀"); say("${tag(r)} 로켓 점화!!!")
            }
            CHICKEN -> {
                addFx(r, 1.6, 1.0, "🪶")
                if (rnd.nextBoolean()) say("${tag(r)} 푸드덕!")
            }
            HORSE, TURTLE -> return false
        }
        return true
    }

    private fun addFx(r: Racer, mult: Double, dur: Double, icon: String, delay: Double = 0.0, debuff: Boolean = false) {
        if (debuff && r.animal == TURTLE) { say("${tag(r)} 등껍질로 막아냄!"); return }
        val from = t + delay
        r.effects += Effect(mult, from, from + dur, icon)
    }

    private fun say(text: String, kind: EventKind = EventKind.SKILL) {
        events += RaceEvent(events.size, t, text, kind)
    }

    private fun tag(r: Racer) = "${r.animal.emoji} ${r.animal.label}"
}

/** 받침 유무에 따라 주격 조사(이/가)를 붙인다. */
internal fun withSubject(word: String): String {
    val c = word.last()
    if (c !in '가'..'힣') return word + "가"
    return word + if ((c - '가') % 28 != 0) "이" else "가"
}

/** 받침 유무에 따라 목적격 조사(을/를)를 붙인다. */
internal fun withObject(word: String): String {
    val c = word.last()
    if (c !in '가'..'힣') return word + "를"
    return word + if ((c - '가') % 28 != 0) "을" else "를"
}
