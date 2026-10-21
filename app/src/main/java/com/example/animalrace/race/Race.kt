package com.example.animalrace.race

import com.example.animalrace.model.Animal
import com.example.animalrace.model.Animal.*
import com.example.animalrace.model.Fx
import com.example.animalrace.model.Venue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** 트랙 길이(시뮬레이션 단위, m). */
const val TRACK = 1000.0

/** 기준 곡선 x(τ) = 24τ + 1.5τ² 가 TRACK에 닿는 시각. 처음엔 느리고 갈수록 빨라지는 모양. */
private const val REF_T = 19.0308

/** 이 진행률(경과 시간 / 자기 결승 시각) 이후로는 모두 기준 곡선(= 최종 순위 순서)으로 달린다. */
private const val SETTLE = 0.93

/** 출발 직후 연출 없이 다 같이 달리는 시간(초). */
private const val CALM = 1.5

/** 대본 위치를 계산하는 간격(초). */
private const val GRID = 1.0 / 120

/** 스킬이 아닌 구간에서 허용하는 속도 범위(기준 속도 대비). 이 밖으로 벗어나는 "이유 없는 급가속·급감속"은 없다. */
internal const val PACE_MIN = 0.7
internal const val PACE_MAX = 1.3

/** 가속 한 번의 최대 이득(기준 속도로 달리는 시간 기준, 초). 배율은 1 + 이 값 / 지속 시간을 넘지 않는다. */
private const val BOOST_GAIN = 1.3

/** 중반(우승자 결승 시각의 절반) 선두가 그대로 이기는 판의 목표 비율. */
internal const val LEADER_WINS_2 = 0.5
internal const val LEADER_WINS_N = 0.4

/** 방해를 받지 않는 동물(거북이 등껍질, 카피바라 무념무상). */
private val IMMUNE = setOf(TURTLE, CAPYBARA)

/** 남을 멈추게 하는 공격 스킬을 가진 동물. */
private val ATTACKERS = setOf(MONKEY, DUCK, ELEPHANT, SKUNK)

enum class EventKind { SKILL, LEAD, FINISH }

data class RaceEvent(val id: Int, val t: Double, val text: String, val kind: EventKind)

/** 화면에 그릴 한 프레임의 불변 스냅샷. */
data class RacerView(
    val lane: Int,
    val animal: Animal,
    val x: Double,
    val rank: Int,
    val fx: Fx,
    /** 현재 연출이 시작된 뒤 지난 시간(초). 연출 애니메이션 진행에 쓴다. */
    val fxElapsed: Double,
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

/**
 * 대본의 한 구간: [from, until) 동안 기준 속도 × [mult]로 달리고 [fx] 연출을 보여 준다.
 * [mult]가 null이면 속도는 그대로 두고 연출만 보여 준다(공격하는 쪽, 거북이 방어 등).
 */
internal class Seg(val from: Double, val until: Double, val mult: Double?, var fx: Fx) {
    operator fun contains(t: Double) = t >= from && t < until
}

private class Cue(val t: Double, val text: String, val user: Racer?)

class Racer internal constructor(val lane: Int, val animal: Animal) {
    var x = 0.0
        internal set
    var finish: Double? = null
        internal set
    var uses = 0
        internal set
    internal var rank = 0
    internal var fx = Fx.NONE
    internal var fxFrom = 0.0
    internal var mult = 1.0

    /** 대본: [GRID] 간격의 위치. */
    internal var path = DoubleArray(0)
    internal val segs = mutableListOf<Seg>()
    val finished get() = finish != null

    internal fun pathAt(t: Double): Double {
        val p = t / GRID
        val k = p.toInt().coerceIn(0, path.size - 2)
        val a = (p - k).coerceIn(0.0, 1.0)
        return path[k] + (path[k + 1] - path[k]) * a
    }

    /** 속도에 영향을 주는 구간의 배율 곱. */
    internal fun motionAt(t: Double): Double {
        var m = 1.0
        for (s in segs) if (s.mult != null && t in s) m *= s.mult
        return m
    }

    internal fun busy(a: Double, b: Double) = segs.any { it.from < b && it.until > a }
}

/**
 * 경주. UI와 무관한 순수 Kotlin이라 JVM에서 그대로 테스트할 수 있다.
 *
 * 공정성: 최종 순위는 출발 시점에 균등한 무작위 순열로 먼저 정해진다([finishAt]).
 * 동물 종류·스킬과 무관하게 모든 동물이 모든 순위에 같은 확률로 온다.
 *
 * 연출(스토리보드): 그 결과에 이르는 전개를 출발할 때 미리 짠다.
 * 우승자는 밀렸다가 역전하고, 가짜 선두는 앞서다가 방해를 받는 식이다.
 * 큰 순위 변화는 반드시 눈에 보이는 스킬(가속·방해) 구간에서만 일어나고,
 * 그 밖의 구간은 기준 속도의 [PACE_MIN]~[PACE_MAX] 안에서만 움직인다.
 * 그래서 "방해받고 멈췄다가 아무 이유 없이 훅 따라잡는" 장면이 나오지 않는다.
 */
/**
 * [venue]는 "혼자 넘어짐" 해설 문구만 바꾼다(문구 수가 같아 난수 사용·결과는 경기장과 무관).
 */
class Race(animals: List<Animal>, private val rnd: Random = Random.Default, private val venue: Venue = Venue.MEADOW) {
    val racers = animals.mapIndexed { i, a -> Racer(i, a) }

    /** 레인별 결승 시각(초). 1등 16.8~18.4초, 이후 0.08~0.45초 간격. */
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
    private val cues = mutableListOf<Cue>()
    private var cueIndex = 0

    init {
        Director().plan()
    }

    fun ranking(): List<Racer> =
        racers.sortedWith(compareBy<Racer>({ it.finish ?: Double.MAX_VALUE }, { -it.x }))

    fun step(dt: Double) {
        if (done) return
        t += dt
        for (r in racers) {
            if (r.finished) continue
            val f = finishAt[r.lane]
            if (t >= f) {
                r.finish = f
                r.x = TRACK
                r.fx = Fx.NONE
                r.mult = 1.0
                finishCount++
                if (finishCount == 1) say("🏁 1등! ${tag(r)}", EventKind.FINISH)
                continue
            }
            r.x = r.pathAt(t)
            r.mult = r.motionAt(t)
            val active = r.segs.filter { t in it }.maxByOrNull { it.from }
            r.fx = active?.fx ?: Fx.NONE
            if (active != null) r.fxFrom = active.from
        }
        while (cueIndex < cues.size && cues[cueIndex].t <= t) {
            val c = cues[cueIndex++]
            c.user?.let { it.uses++ }
            say(c.text)
        }
        ranking().forEachIndexed { i, r -> r.rank = i }

        if (finishCount == 0) {
            val lead = racers.maxBy { it.x }
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
                    fx = r.fx, fxElapsed = t - r.fxFrom, stopped = !r.finished && r.mult == 0.0,
                    boosted = !r.finished && r.mult > 1.05, finished = r.finished, finishTime = r.finish,
                )
            },
            recentEvents = events.takeLast(5).reversed(),
            leadChanges = leadChanges,
            done = done,
        )
    }

    private fun say(text: String, kind: EventKind = EventKind.SKILL) {
        events += RaceEvent(events.size, t, text, kind)
    }

    private fun tag(r: Racer) = "${r.animal.emoji} ${r.animal.label}"

    // ================================================================ 스토리보드

    private enum class Beat { BOOST, SETBACK }

    private enum class Role { WINNER, DECOY, RUNNER_UP, LAST, OTHER }

    /**
     * 판 전체의 전개 모양. 중반 선두가 이기는 판(수성·재역전)과 뒤집히는 판(역전)을 섞어
     * "중반에 앞선 동물은 진다" 같은 패턴이 읽히지 않게 한다.
     * - WIRE: 우승자가 초반에 치고 나가 끝까지 지킴, 나머지는 막판 추격하다 아깝게 못 미침
     * - SEESAW: 우승자가 앞서다 중후반에 잠깐 뒤집히고, 막판에 재역전
     * - COMEBACK: 우승자가 밀렸다가 막판 역전, 가짜 선두는 막판에 당함
     */
    private enum class Story { WIRE, SEESAW, COMEBACK }

    /** 가속 스킬의 한 단계: 시작 후 [start]초부터 [dur]초 동안 기준 속도 × [mult]. */
    private class Phase(val start: Double, val dur: Double, val mult: Double, val fx: Fx)

    /** 한 동물의 대본 후보. 검증을 통과하면 [commit]으로 확정한다. */
    private class Plan(val path: DoubleArray, val segs: List<Seg>, val setbacks: List<Seg>, val cues: List<Cue>)

    private inner class Director {
        private val order = racers.sortedBy { finishAt[it.lane] }
        private val setbacks = mutableListOf<Pair<Racer, Seg>>()

        /** 중반 선두가 이기는 판의 목표 비율: 2마리면 절반, 3마리 이상이면 40%. */
        private val story = run {
            val leaderWins = if (racers.size <= 2) LEADER_WINS_2 else LEADER_WINS_N
            val p = rnd.nextDouble()
            when {
                racers.size < 2 -> Story.COMEBACK
                p < leaderWins * 0.6 -> Story.WIRE
                p < leaderWins -> Story.SEESAW
                else -> Story.COMEBACK
            }
        }
        private val wire = story == Story.WIRE
        private val seesaw = story == Story.SEESAW

        fun plan() {
            val n = racers.size
            val winner = order[0]
            // 가짜 선두: 초반에 치고 나갔다가 막판에 당하는 역할. 4마리 이상이면 3등 이하에서 고른다.
            val decoy = when {
                n >= 4 -> order[2 + rnd.nextInt(n - 2)]
                n >= 2 -> order[1 + rnd.nextInt(n - 1)]
                else -> null
            }
            val roles = racers.associateWith { r ->
                when {
                    r === winner -> Role.WINNER
                    r === decoy -> Role.DECOY
                    n >= 3 && r === order[1] -> Role.RUNNER_UP
                    n >= 3 && r === order[n - 1] -> Role.LAST
                    else -> Role.OTHER
                }
            }
            // 우승자의 역전 가속 시각(과 재역전 판의 주춤 시각)에 맞춰 나머지를 짜도록 우승자부터 짠다
            var winnerSurge: Double? = null
            var winnerDip: Double? = null
            val sequence = listOf(winner) + racers.filter { it !== winner }
            for (r in sequence) {
                // 앞서 나가는 우승자는 초반 리드를 중반 이후까지 유지하다가 막판에 기준 위치로 합류한다
                val settleFrom = when {
                    r !== winner -> CALM
                    wire -> finishAt[r.lane] * 0.5
                    seesaw -> finishAt[r.lane] * 0.36
                    else -> CALM
                }
                val plan = (0 until 14).firstNotNullOfOrNull { build(r, arc(r, roles.getValue(r), winnerSurge, winnerDip), settleFrom) }
                    ?: build(r, emptyList(), CALM)!!
                commit(r, plan)
                if (r === winner) {
                    winnerSurge = plan.segs.filter { (it.mult ?: 1.0) > 1.0 }.maxOfOrNull { it.from }
                    winnerDip = plan.setbacks.maxOfOrNull { it.from }
                }
            }
            resolveSetbacks()
            immuneShows()
            cues.sortBy { it.t }
        }

        private fun commit(r: Racer, plan: Plan) {
            r.path = plan.path
            r.segs += plan.segs
            plan.setbacks.forEach { setbacks += r to it }
            cues += plan.cues
        }

        /** 역할별 전개. 진행률 구간을 시각으로 바꿔 (종류, 시각) 목록을 만든다. */
        private fun arc(r: Racer, role: Role, winnerSurge: Double?, winnerDip: Double?): List<Pair<Beat, Double>> {
            val f = finishAt[r.lane]
            fun at(a: Double, b: Double) = f * (a + rnd.nextDouble() * (b - a))
            val p = rnd.nextDouble()
            val beats = when (role) {
                // 역전승이 대부분이지만, 가끔은 처음부터 치고 나가 끝까지 지키는 판도 있어야 패턴이 읽히지 않는다
                Role.WINNER -> when {
                    wire -> listOfNotNull(Beat.BOOST to at(0.1, 0.22), if (rnd.nextBoolean()) Beat.BOOST to at(0.4, 0.5) else null)
                    seesaw && r.animal in IMMUNE -> listOf(Beat.BOOST to at(0.1, 0.22), Beat.BOOST to at(0.72, 0.84))
                    seesaw -> listOf(Beat.BOOST to at(0.1, 0.24), Beat.SETBACK to at(0.53, 0.62), Beat.BOOST to at(0.72, 0.84))
                    p < 0.55 -> listOf(Beat.SETBACK to at(0.18, 0.42), Beat.BOOST to at(0.66, 0.8))
                    p < 0.8 -> listOf(Beat.BOOST to at(0.66, 0.82))
                    else -> listOf(Beat.BOOST to at(0.12, 0.3), Beat.SETBACK to at(0.4, 0.55), Beat.BOOST to at(0.68, 0.8))
                }
                // 선두 수성 판: 나머지는 초반에 앞서 나가지 않고, 막판에 추격하다 아깝게 못 미친다
                // 재역전 판: 가짜 선두가 우승자가 주춤할 때 치고 나갔다가 막판에 다시 뒤집힌다
                Role.DECOY -> when {
                    wire -> listOf(Beat.BOOST to at(0.66, 0.82))
                    seesaw -> listOf(Beat.BOOST to (winnerDip ?: at(0.53, 0.62)) + 0.1 + rnd.nextDouble() * 0.4)
                    else -> roleArc(r, role, winnerSurge, ::at, p)
                }
                Role.RUNNER_UP -> if (wire || seesaw) listOf(Beat.BOOST to at(0.66, 0.82)) else roleArc(r, role, winnerSurge, ::at, p)
                Role.LAST, Role.OTHER -> if (wire || seesaw) chaser(::at) else roleArc(r, role, winnerSurge, ::at, p)
            }
            // 거북이(등껍질)·카피바라(무념무상)는 방해를 받지 않으므로 당하는 장면이 없다
            return if (r.animal in IMMUNE) beats.filter { it.first == Beat.BOOST } else beats
        }

        private fun chaser(at: (Double, Double) -> Double): List<Pair<Beat, Double>> {
            val p = rnd.nextDouble()
            return when {
                p < 0.4 -> listOf(Beat.SETBACK to at(0.15, 0.4), Beat.BOOST to at(0.55, 0.82))
                p < 0.65 -> listOf(Beat.SETBACK to at(0.15, 0.7))
                else -> emptyList()
            }
        }

        private fun roleArc(r: Racer, role: Role, winnerSurge: Double?, at: (Double, Double) -> Double, p: Double): List<Pair<Beat, Double>> {
            val f = finishAt[r.lane]
            return when (role) {
                Role.WINNER -> emptyList()
                Role.DECOY -> {
                    val fall = (winnerSurge ?: at(0.62, 0.76)) - 0.3 - rnd.nextDouble() * 0.9
                    listOfNotNull(
                        Beat.BOOST to at(0.12, 0.32),
                        if (rnd.nextDouble() < 0.4) Beat.BOOST to at(0.38, 0.48) else null,
                        Beat.SETBACK to fall,
                    )
                }
                Role.RUNNER_UP -> if (p < 0.5) listOf(Beat.SETBACK to at(0.2, 0.45), Beat.BOOST to at(0.6, 0.8)) else other(at)
                Role.LAST -> when {
                    p < 0.5 -> listOf(Beat.BOOST to at(0.12, 0.35), Beat.SETBACK to at(0.45, 0.7))
                    p < 0.8 -> listOf(Beat.SETBACK to at(0.2, 0.45))
                    else -> emptyList()
                }
                Role.OTHER -> other(at)
            }
        }

        private fun other(at: (Double, Double) -> Double): List<Pair<Beat, Double>> {
            val p = rnd.nextDouble()
            return when {
                p < 0.35 -> listOf(Beat.BOOST to at(0.12, 0.42), Beat.SETBACK to at(0.48, 0.82))
                p < 0.65 -> listOf(Beat.SETBACK to at(0.12, 0.42), Beat.BOOST to at(0.48, 0.82))
                p < 0.75 -> listOf(Beat.BOOST to at(0.15, 0.8))
                p < 0.85 -> listOf(Beat.SETBACK to at(0.15, 0.8))
                else -> emptyList()
            }
        }

        /**
         * 전개를 위치 곡선으로 만든다. 스킬 구간은 기준 속도 × 배율로 달리고,
         * 스킬로 생긴 앞섬/뒤처짐의 합은 스킬이 아닌 구간에 완만하게 나눠서 [SETTLE] 시점에 0이 되게 한다.
         * 조건(겹침 없음, 완만함, 결승 전 도착 안 함)을 어기면 null.
         */
        private fun build(r: Racer, beats: List<Pair<Beat, Double>>, settleFrom: Double): Plan? {
            val f = finishAt[r.lane]
            val settle = SETTLE * f
            val segs = mutableListOf<Seg>()
            val setbackSegs = mutableListOf<Seg>()
            val planCues = mutableListOf<Cue>()
            var boosts = 0
            var lastEnd = CALM
            for ((kind, t0) in beats.sortedBy { it.second }) {
                if (t0 < lastEnd + 1.5) return null
                when (kind) {
                    Beat.BOOST -> {
                        val generic = r.animal == SNAIL && boosts > 0
                        val phases = boostPhases(r.animal, generic)
                        // 가속 한 번으로 앞서 나가는 양에 상한을 둔다 (길고 강한 가속은 되돌릴 수 없어 전개가 통째로 실패하므로)
                        phases.forEach {
                            val m = if (it.mult > 1) min(it.mult, 1 + BOOST_GAIN / it.dur) else it.mult
                            segs += Seg(t0 + it.start, t0 + it.start + it.dur, m, it.fx)
                        }
                        planCues += Cue(t0, "${tag(r)} ${boostText(r.animal, t0 / f, generic)}", r)
                        if (r.animal == CHEETAH) planCues += Cue(t0 + phases[1].start, "${tag(r)} 숨이 차서 헉헉…", null)
                        lastEnd = t0 + phases.maxOf { it.start + it.dur }
                        boosts++
                    }
                    Beat.SETBACK -> {
                        val d = 1.6 + rnd.nextDouble() * 0.8
                        val s = Seg(t0, t0 + d, 0.0, Fx.TRIP)
                        segs += s
                        setbackSegs += s
                        lastEnd = t0 + d
                    }
                }
            }
            if (lastEnd > settle - 0.2) return null

            // 스킬이 아닌 구간의 자연스러운 완급 (±10%)
            val wAmp = 0.1
            val wOmega = 2 * PI / (4 + rnd.nextDouble() * 4)
            val wPhase = rnd.nextDouble() * 2 * PI
            fun motion(tm: Double): Double {
                var m = 1.0
                for (s in segs) if (tm in s) m *= s.mult!!
                return m
            }

            val steps = ceil(f / GRID).toInt() + 2
            val kS = (settle / GRID).toInt()
            val raw = DoubleArray(steps + 1)
            val free = BooleanArray(steps)
            for (k in 0 until steps) {
                val tm = (k + 0.5) * GRID
                var m = motion(tm)
                free[k] = m == 1.0
                if (k < kS && m > 0) m *= 1 + wAmp * sin(wOmega * tm + wPhase) * envelope(tm, settle)
                raw[k + 1] = raw[k] + baseV(tm, f) * m * GRID
            }
            // 스킬로 생긴 차이를 스킬이 아닌 구간에 나눠서 되돌린다
            val net = raw[kS] - base(kS * GRID, f)
            val cum = DoubleArray(kS + 1)
            for (k in 0 until kS) {
                val tm = (k + 0.5) * GRID
                val rho = if (free[k] && tm > settleFrom) sin(PI * (tm - settleFrom) / (settle - settleFrom)).coerceAtLeast(0.0) else 0.0
                cum[k + 1] = cum[k] + rho
            }
            val total = cum[kS].takeIf { it > 0 } ?: return null
            val path = DoubleArray(steps + 1) { k ->
                when {
                    k <= kS -> raw[k] - net * cum[k] / total
                    else -> base(k * GRID, f)
                }
            }

            // 검증: 스킬이 아닌 구간은 완만하게, 결승 시각 전에는 결승선에 닿지 않게
            for (k in 0 until kS) {
                val tm = (k + 0.5) * GRID
                val v = (path[k + 1] - path[k]) / GRID
                if (v < -1e-9) return null
                if (free[k] && tm > CALM) {
                    val pace = v / baseV(tm, f)
                    if (pace < PACE_MIN || pace > PACE_MAX) return null
                }
                if (path[k] > TRACK - 30) return null
            }
            return Plan(path, segs, setbackSegs, planCues)
        }

        /**
         * 방해 장면마다 그 순간 상황에 맞는 원인을 붙인다
         * (오리는 1등에게, 원숭이는 바로 앞에게, 코끼리는 근처에게, 스컹크는 바로 뒤에게).
         * 고슴도치는 공격을 튕겨내므로, 공격하는 동물이 멈춰야 할 때 고슴도치에게 반사당하는 장면이 될 수 있다.
         */
        private fun resolveSetbacks() {
            for ((v, seg) in setbacks.sortedBy { it.second.from }) {
                val t0 = seg.from
                val options = mutableListOf<Pair<Double, () -> Unit>>()
                val running = racers.filter { t0 < finishAt[it.lane] }
                val leaderNow = running.maxByOrNull { it.pathAt(t0) }
                fun gap(a: Racer) = v.pathAt(t0) - a.pathAt(t0)
                // 고슴도치는 공격을 받으면 튕겨내므로 남의 공격으로 멈추지 않는다
                val attackable = v.animal != HEDGEHOG

                if (attackable) racers.filter { it.animal == DUCK && it !== v && it in running && leaderNow === v && !it.busy(t0 - 0.3, t0 + 1.2) }
                    .forEach { d ->
                        options += 3.0 to {
                            seg.fx = Fx.SCARED
                            d.segs += Seg(t0 - 0.2, t0 + 1.2, null, Fx.SKILL)
                            cues += Cue(t0 - 0.2, "${tag(d)} 꽥!! ${withSubject(tag(v))} 깜짝 놀라 멈춤", d)
                        }
                    }
                if (attackable) racers.filter { it.animal == MONKEY && it !== v && it in running && gap(it) in 5.0..260.0 && !it.busy(t0 - 0.7, t0 + 1.0) }
                    .forEach { m ->
                        options += 3.0 to {
                            seg.fx = Fx.SLIP
                            m.segs += Seg(t0 - 0.5, t0 + 1.0, null, Fx.SKILL)
                            cues += Cue(t0 - 0.5, "${tag(m)} 바나나 투척 → ${withSubject(tag(v))} 미끄러짐!", m)
                        }
                    }
                if (attackable) racers.filter { it.animal == ELEPHANT && it !== v && it in running && abs(gap(it)) < 160 && !it.busy(t0 - 0.3, t0 + 1.3) }
                    .forEach { e ->
                        options += 3.0 to {
                            seg.fx = Fx.QUAKE
                            e.segs += Seg(t0 - 0.2, t0 + 1.3, null, Fx.SKILL)
                            cues += Cue(t0 - 0.2, "${tag(e)} 쿵쿵! ${withSubject(tag(v))} 휘청", e)
                        }
                    }
                if (attackable) racers.filter { it.animal == SKUNK && it !== v && it in running && gap(it) in -120.0..-5.0 && !it.busy(t0 - 0.3, t0 + 1.3) }
                    .forEach { k ->
                        options += 3.0 to {
                            seg.fx = Fx.STINK
                            k.segs += Seg(t0 - 0.3, t0 + 1.3, null, Fx.SKILL)
                            cues += Cue(t0 - 0.3, "${tag(k)} 방귀 폭탄! ${withSubject(tag(v))} 으악 냄새!", k)
                        }
                    }
                // 공격하는 동물이 멈추는 차례라면: 고슴도치를 공격했다가 가시에 튕겨 대신 당한다
                if (v.animal in ATTACKERS && !v.busy(t0 - 0.6, t0 - 0.01)) {
                    racers.filter { h ->
                        h.animal == HEDGEHOG && h !== v && h in running && !h.busy(t0 - 0.4, t0 + 1.0) && when (v.animal) {
                            MONKEY -> gap(h) in -260.0..-5.0
                            DUCK -> leaderNow === h
                            ELEPHANT -> abs(gap(h)) < 160
                            SKUNK -> gap(h) in 5.0..120.0
                            else -> false
                        }
                    }.forEach { h ->
                        options += 3.0 to {
                            val (what, reaction) = when (v.animal) {
                                MONKEY -> "바나나 투척" to Fx.SLIP
                                DUCK -> "꽥!!" to Fx.SCARED
                                ELEPHANT -> "쿵쿵!" to Fx.QUAKE
                                else -> "방귀 폭탄" to Fx.STINK
                            }
                            seg.fx = reaction
                            v.segs += Seg(t0 - 0.5, t0, null, Fx.SKILL)
                            h.segs += Seg(t0 - 0.3, t0 + 0.9, null, Fx.SPIKE)
                            cues += Cue(t0 - 0.5, "${tag(v)} $what → ${tag(h)} 가시로 따끔 반사! ${withSubject(tag(v))} 대신 당함", h)
                        }
                    }
                }
                val flaw = when (v.animal) {
                    HAMSTER -> "볼에 해바라기씨 채우는 중…"
                    PANDA -> "길가 대나무 냠냠…"
                    SLOTH -> "하품하며 잠깐 쉬는 중…"
                    else -> null
                }
                if (flaw != null) {
                    options += 2.5 to {
                        seg.fx = Fx.FLAW
                        cues += Cue(t0, "${tag(v)} $flaw", v)
                    }
                }
                if (v.animal == RABBIT && running.count { it.pathAt(t0) > v.pathAt(t0) } <= 1) {
                    options += 2.5 to {
                        seg.fx = Fx.SKILL
                        cues += Cue(t0, "${tag(v)} 너무 앞서서 방심, 낮잠…", v)
                    }
                }
                if (v.animal == CAT) {
                    options += 2.5 to {
                        seg.fx = Fx.SKILL_ALT
                        cues += Cue(t0, "${tag(v)} 멈춰서 그루밍 중…", v)
                    }
                }
                options += 1.0 to {
                    seg.fx = Fx.TRIP
                    val why = TRIP_LINES.getValue(venue)
                    cues += Cue(t0, "${tag(v)} ${why[rnd.nextInt(why.size)]}", null)
                }
                pick(options)()
            }
        }

        /** 거북이·카피바라가 있으면 가끔 방해를 막아 내는(무시하는) 장면을 넣는다 (결과에는 영향 없음). */
        private fun immuneShows() {
            for (tt in racers.filter { it.animal in IMMUNE }) {
                if (rnd.nextDouble() > 0.6) continue
                repeat(8) {
                    val t0 = 3.0 + rnd.nextDouble() * (finishAt[tt.lane] * 0.75 - 3.0)
                    if (tt.busy(t0 - 0.5, t0 + 1.2)) return@repeat
                    val gap: (Racer) -> Double = { tt.pathAt(t0) - it.pathAt(t0) }
                    val running = racers.filter { t0 < finishAt[it.lane] }
                    val attacker = running.firstOrNull { a ->
                        !a.busy(t0 - 0.7, t0 + 1.2) && when (a.animal) {
                            MONKEY -> gap(a) in 5.0..260.0
                            ELEPHANT -> abs(gap(a)) < 160
                            DUCK -> running.maxBy { it.pathAt(t0) } === tt
                            SKUNK -> gap(a) in -120.0..-5.0
                            else -> false
                        }
                    } ?: return@repeat
                    val turtle = tt.animal == TURTLE
                    attacker.segs += Seg(t0 - 0.5, t0 + 0.9, null, Fx.SKILL)
                    tt.segs += Seg(t0, t0 + 1.2, null, if (turtle) Fx.BLOCK else Fx.ZEN)
                    val what = when (attacker.animal) {
                        MONKEY -> "바나나 투척"
                        ELEPHANT -> "쿵쿵!"
                        SKUNK -> "방귀 폭탄"
                        else -> "꽥!!"
                    }
                    val how = if (turtle) "등껍질로 막아냄!" else "수건 얹고 평온… 끄떡없음"
                    cues += Cue(t0 - 0.5, "${tag(attacker)} $what → ${tag(tt)} $how", attacker)
                    return
                }
            }
        }

        private fun <T> pick(options: List<Pair<Double, T>>): T {
            var x = rnd.nextDouble() * options.sumOf { it.first }
            for ((w, o) in options) {
                x -= w
                if (x <= 0) return o
            }
            return options.last().second
        }
    }

    // ---------- 동물별 가속 스킬 ----------

    private fun boostPhases(a: Animal, generic: Boolean): List<Phase> {
        if (generic) return listOf(Phase(0.0, 1.6, 1.9, Fx.RALLY))
        return when (a) {
            HORSE -> listOf(Phase(0.0, 2.0, 1.9, Fx.SKILL))
            CHEETAH -> listOf(Phase(0.0, 1.3, 2.3, Fx.SKILL), Phase(1.3, 1.6, 0.55, Fx.SKILL_ALT))
            DOG -> listOf(Phase(0.0, 1.8, 1.8, Fx.SKILL))
            CAT -> listOf(Phase(0.0, 1.3, 2.0, Fx.SKILL))
            PIG -> listOf(Phase(0.0, 1.0, 0.0, Fx.SKILL), Phase(1.0, 1.8, 2.4, Fx.SKILL_ALT))
            PENGUIN -> listOf(Phase(0.0, 2.0, 1.85, Fx.SKILL))
            KANGAROO -> listOf(Phase(0.0, 1.5, 2.0, Fx.SKILL))
            SNAIL -> listOf(Phase(0.0, 2.6, 2.3, Fx.SKILL))
            CHICKEN -> listOf(Phase(0.0, 1.4, 1.9, Fx.SKILL))
            TURTLE -> listOf(Phase(0.0, 2.0, 1.7, Fx.SKILL))
            HEDGEHOG -> listOf(Phase(0.0, 1.4, 2.0, Fx.SKILL))
            CAPYBARA -> listOf(Phase(0.0, 1.8, 1.7, Fx.SKILL))
            SLOTH -> listOf(Phase(0.0, 2.2, 2.1, Fx.SKILL))
            HAMSTER -> listOf(Phase(0.0, 1.3, 2.1, Fx.SKILL))
            PANDA -> listOf(Phase(0.0, 1.2, 2.3, Fx.SKILL))
            RABBIT, MONKEY, ELEPHANT, DUCK, SKUNK -> listOf(Phase(0.0, 1.6, 1.9, Fx.RALLY))
        }
    }

    private fun boostText(a: Animal, q: Double, generic: Boolean): String = if (generic) "이를 악물고 분발!" else when (a) {
        HORSE -> if (q >= 0.5) "막판 스퍼트!" else "갈기를 휘날리며 질주!"
        CHEETAH -> "폭발 질주!"
        DOG -> "추격 본능 발동!"
        CAT -> "갑자기 전력질주!"
        PIG -> "간식 발견! 먹고 돌진"
        PENGUIN -> "배 슬라이딩!"
        KANGAROO -> "껑충! 껑충!"
        SNAIL -> "로켓 점화!!!"
        CHICKEN -> "푸드덕!"
        TURTLE -> "성큼성큼 가속!"
        RABBIT -> "깡충깡충 전력질주!"
        HEDGEHOG -> "몸을 말아 스핀 대시!"
        CAPYBARA -> "유자 얹고 둥실둥실~"
        SLOTH -> "진심 모드 발동!!"
        HAMSTER -> "쳇바퀴 풀가동!"
        PANDA -> "대나무 장대높이뛰기!"
        MONKEY, ELEPHANT, DUCK, SKUNK -> "이를 악물고 분발!"
    }

    private fun base(t: Double, f: Double): Double {
        val tau = t * REF_T / f
        return min(TRACK, 24 * tau + 1.5 * tau * tau)
    }

    private fun baseV(t: Double, f: Double): Double = (24 + 3 * t * REF_T / f) * REF_T / f

    /** 출발 직후와 마무리 구간에서는 0, 그 사이에서 1. */
    private fun envelope(t: Double, settle: Double): Double =
        smoothstep(CALM, CALM + 2.5, t) * (1 - smoothstep(settle * 0.8, settle, t))

    private fun smoothstep(a: Double, b: Double, x: Double): Double {
        val k = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
        return k * k * (3 - 2 * k)
    }

    // ---------- 테스트용 ----------

    /** 대본 검증용: 해당 동물이 [t]초에 스킬(가속·방해) 구간 안에 있는가. */
    internal fun inSkill(r: Racer, t: Double) = r.segs.any { it.mult != null && t in it }

    internal fun finishTimeOf(r: Racer) = finishAt[r.lane]

    internal fun baseSpeed(r: Racer, t: Double) = baseV(t, finishAt[r.lane])
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

/** 경기장별 "혼자 넘어짐" 해설. 경기장마다 4개씩(난수 사용량을 같게). */
private val TRIP_LINES = mapOf(
    Venue.MEADOW to listOf("돌부리에 걸려 꽈당!", "돌부리를 못 보고 꽈당!", "발이 꼬여 돌부리에 꽈당!", "한눈팔다 돌부리에 꽈당!"),
    Venue.BEACH to listOf("파도가 철썩! 흠뻑 젖어 멈칫", "큰 파도를 정통으로 맞았어요!", "파도에 휩쓸려 철썩!", "물보라에 눈을 못 뜨고 멈칫!"),
    Venue.SNOW to listOf("빙판에서 미끄덩~ 눈더미까지 와르르!", "얼음판에 미끄덩! 머리 위로 눈이 와르르", "헛발질하다 꽈당, 눈 폭탄까지!", "미끄덩~ 와르르! 눈사람이 될 뻔"),
    Venue.DESERT to listOf("모래 토네이도에 휘말려 빙글빙글~", "회오리바람에 붕 떠올랐어요!", "모래바람에 휘잉~ 빙글빙글", "토네이도 속에서 정신없이 빙글빙글!"),
)
