package com.example.animalrace.race

import com.example.animalrace.model.Animal
import com.example.animalrace.model.Animal.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** 트랙 길이(시뮬레이션 단위, m). */
const val TRACK = 1000.0

/** 기준 곡선 x(τ) = 24τ + 1.5τ² 가 TRACK에 닿는 시각. 처음엔 느리고 갈수록 빨라지는 모양. */
private const val REF_T = 19.0308

/** 이 진행률(경과 시간 / 자기 결승 시각)이 지나면 스킬이 발동하지 않고 정해진 결과로 수렴한다. */
private const val SETTLE = 0.8

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
    val finishTime: Double?,
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
    internal var noise = 1.0
    internal var noiseT = 0.0
    internal var flag = false
    internal val effects = mutableListOf<Effect>()
    val finished get() = finish != null
}

/**
 * 경주 시뮬레이션. UI와 무관한 순수 Kotlin이라 JVM에서 그대로 테스트할 수 있다.
 *
 * 공정성: 최종 순위는 출발 시점에 균등한 무작위 순열로 먼저 정해지고([finishAt]),
 * 동물 종류·스킬과 무관하게 모든 동물이 모든 순위에 같은 확률로 온다.
 * 스킬·특성·흔들림은 화면 속 위치만 흔드는 연출이고, 보정 항이 각 동물을
 * 자기 결승 시각으로 끌고 가서 결과는 바뀌지 않는다.
 */
class Race(animals: List<Animal>, private val rnd: Random = Random.Default) {
    val racers = animals.mapIndexed { i, a -> Racer(i, a, rnd) }

    /** 레인별 결승 시각(초). 1착 16.8~18.4초, 이후 0.08~0.45초 간격. */
    private val finishAt = DoubleArray(racers.size).also { at ->
        var f = 16.8 + rnd.nextDouble() * 1.6
        racers.shuffled(rnd).forEach { r ->
            at[r.lane] = f
            f += 0.08 + rnd.nextDouble() * 0.37
        }
    }
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

        for (r in active) {
            if (t >= r.noiseT) {
                val spread = if (r.animal == TURTLE) 0.1 else 0.25
                r.noise = 1 - spread + rnd.nextDouble() * spread * 2
                r.noiseT = t + 0.6 + rnd.nextDouble() * 0.9
            }
            r.cd -= dt
            tick(r)
            val rate = skillRate(r.animal)
            if (rate > 0 && r.cd <= 0 && t > 1.5 && progress(r, 2.0) < SETTLE &&
                rnd.nextDouble() < rate * dt && act(r)
            ) {
                r.cd = cooldown(r.animal)
                r.uses++
            }

            var m = 1.0
            r.fx = null
            for (e in r.effects) if (t >= e.from && t < e.until) { m *= e.mult; r.fx = e.icon }
            r.effects.removeAll { it.until <= t }
            r.mult = m

            // 정해진 결승 시각 f에 맞춘 기준 위치 d와 기준 속도 dv
            val f = finishAt[r.lane]
            val q = t / f
            val tau = t * REF_T / f
            val d = min(TRACK, 24 * tau + 1.5 * tau * tau)
            val dv = (24 + 3 * tau) * REF_T / f
            // 연출(특성·흔들림·스킬) 속도 + 기준 위치로 끌어당기는 보정. 보정은 후반으로 갈수록 강해진다.
            val pull = 0.5 + 3.0 * q * q
            // 앞서 나간 동물도 멈추지 않고 느려지기만 하도록 최소 속도를 둔다 (스킬 정지는 예외)
            val v = if (m == 0.0) 0.0 else max(0.4 * dv * min(1.0, m), dv * trait(r) * r.noise * m + pull * (d - r.x))
            var nx = r.x + v * dt
            // 마지막 구간은 기준 위치로 부드럽게 합류 (뒤로 가지는 않음)
            val blend = smoothstep(SETTLE, 1.0, q)
            nx = max(r.x + 0.2 * dv * dt * blend, nx * (1 - blend) + d * blend)
            nx = if (t >= f) TRACK else min(nx, TRACK - 0.5)
            r.v = (nx - r.x) / dt
            r.x = nx
            if (t >= f) {
                r.finish = f
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
                    boosted = !r.finished && r.mult > 1.05, finished = r.finished, finishTime = r.finish,
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
            SNAIL -> if (!r.flag && t > 11 && progress(r, 2.6) < SETTLE) { act(r); r.uses++ }
            else -> Unit
        }
    }

    /** 확률 발동형 스킬. 조건이 안 맞아 발동하지 않았으면 false. */
    private fun act(r: Racer): Boolean {
        val active = racers.filter { !it.finished }
        when (r.animal) {
            CHEETAH -> {
                if (progress(r, 2.8) >= SETTLE) return false
                addFx(r, 2.1, 1.2, "🔥"); addFx(r, 0.5, 1.6, "😮‍💨", delay = 1.2)
                say("${tag(r)} 폭발 질주!")
            }
            RABBIT -> {
                if (r.rank != 0 || active.any { it !== r && r.x - it.x < 35 } || progress(r, 1.5) >= SETTLE) return false
                addFx(r, 0.0, 1.5, "💤"); say("${tag(r)} 너무 앞서서 방심, 낮잠…")
            }
            DOG -> {
                if (r.rank < ceil(racers.size / 2.0)) return false
                addFx(r, 1.65, 1.6, "👃"); say("${tag(r)} 추격 본능 발동!")
            }
            CAT -> if (rnd.nextBoolean() || progress(r, 0.9) >= SETTLE) {
                addFx(r, 1.6, 1.0, "😼"); say("${tag(r)} 갑자기 전력질주!")
            } else {
                addFx(r, 0.2, 0.9, "🧶"); say("${tag(r)} 멈춰서 그루밍 중…")
            }
            PIG -> {
                if (progress(r, 2.8) >= SETTLE) return false
                addFx(r, 0.0, 1.0, "🍎"); addFx(r, 2.1, 1.8, "💪", delay = 1.0)
                say("${tag(r)} 간식 발견! 먹고 돌진")
            }
            MONKEY -> {
                val target = active.filter { it !== r && it.x > r.x && it.x - r.x < 260 && progress(it, 1.1) < SETTLE }
                    .minByOrNull { it.x }
                    ?: return false
                say("${tag(r)} 바나나 투척 → ${withSubject(tag(target))} 미끄러짐!")
                addFx(target, 0.0, 1.1, "🍌", debuff = true)
            }
            PENGUIN -> { addFx(r, 1.75, 2.0, "🧊"); say("${tag(r)} 배 슬라이딩!") }
            KANGAROO -> {
                if (progress(r, 1.0) >= SETTLE) return false
                r.x += 45; addFx(r, 1.0, 0.5, "⤴️")
                if (rnd.nextDouble() < 0.35) say("${tag(r)} 껑충!")
            }
            ELEPHANT -> {
                val hit = active.filter { it !== r && abs(it.x - r.x) < 150 && progress(it, 1.3) < SETTLE }
                if (hit.isEmpty()) return false
                say("${tag(r)} 쿵쿵! 주변 ${hit.size}마리 휘청")
                hit.forEach { addFx(it, 0.45, 1.3, "💫", debuff = true) }
            }
            DUCK -> {
                val lead = active.maxBy { it.x }
                if (lead === r || progress(lead, 1.2) >= SETTLE) return false
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

    /** [extra]초 뒤의 진행률. 스킬 효과가 끝나는 시점이 마무리 구간 전인지 확인할 때 쓴다. */
    private fun progress(r: Racer, extra: Double = 0.0) = (t + extra) / finishAt[r.lane]

    private fun smoothstep(a: Double, b: Double, x: Double): Double {
        val k = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
        return k * k * (3 - 2 * k)
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
