package com.example.animalrace.model

import java.util.Calendar

/**
 * 우리 기록 (4.2): 이름을 붙여 달린 판들에서 사람별로 센 숫자.
 * 쌓인 기록을 세기만 하고 다음 판에는 아무 영향이 없다. "이 동물이 유리하다"로 읽힐 동물 통계는 넣지 않는다.
 */
enum class Period(val label: String) { WEEK("이번 주"), MONTH("이번 달"), ALL("전체") }

data class PersonStat(val who: String, val games: Int, val penalties: Int, val wins: Int)

data class OurStats(
    /** 센 판 수 */
    val games: Int,
    /** 벌칙 많은 순 (같으면 판 수 많은 순) */
    val people: List<PersonStat>,
    /** 지금 이어지는 연속 당첨 (2판 이상일 때만) */
    val streak: Pair<String, Int>?,
    /** 최근 판부터 (벌칙 이름, 당첨자들) */
    val recent: List<Pair<String, List<String>>>,
) {
    /** 벌칙 왕: 가장 많이 걸린 사람 (아무도 안 걸렸으면 null) */
    val penaltyKing get() = people.firstOrNull()?.takeIf { it.penalties > 0 }
    /** 1등 왕 */
    val winKing get() = people.filter { it.wins > 0 }.maxWithOrNull(compareBy<PersonStat> { it.wins }.thenBy { it.games })
    /** 가장 운 좋은 사람: 2판 이상 달린 사람 중 걸린 비율이 가장 낮은 사람 */
    val lucky get() = people.filter { it.games >= 2 }
        .minWithOrNull(compareBy<PersonStat> { it.penalties.toDouble() / it.games }.thenByDescending { it.games })
}

/** 이 판에서 벌칙을 받는 사람들: 몰아주기를 했으면 그 한 명, 아니면 당첨 등수(꼴찌)의 사람들 */
fun penaltyPeople(record: RaceRecord): List<String> {
    val mini = record.miniGame
    return if (mini != null) listOfNotNull(mini.who) else record.losers.mapNotNull { it.who }
}

/** 이름 붙인 사람이 있는 판만, [period] 안에서, [group]을 고르면 그 묶음 사람만 달린 판만 센다. [history]는 최근 판이 앞 */
fun ourStats(history: List<RaceRecord>, period: Period, group: List<String>? = null, now: Long = System.currentTimeMillis()): OurStats {
    val from = periodStart(period, now)
    val records = history.filter { r ->
        val names = r.results.mapNotNull { it.who }
        r.at >= from && names.isNotEmpty() && (group == null || (names.size >= 2 && names.all { it in group }))
    }
    val games = mutableMapOf<String, Int>()
    val penalties = mutableMapOf<String, Int>()
    val wins = mutableMapOf<String, Int>()
    records.forEach { r ->
        r.results.mapNotNull { it.who }.distinct().forEach { games[it] = (games[it] ?: 0) + 1 }
        penaltyPeople(r).distinct().forEach { penalties[it] = (penalties[it] ?: 0) + 1 }
        r.results.first().who?.let { wins[it] = (wins[it] ?: 0) + 1 }
    }
    val people = games.map { (who, n) -> PersonStat(who, n, penalties[who] ?: 0, wins[who] ?: 0) }
        .sortedWith(compareByDescending<PersonStat> { it.penalties }.thenByDescending { it.games }.thenBy { it.who })
    val streak = records.firstOrNull()?.let { first ->
        penaltyPeople(first).distinct().map { who -> who to records.takeWhile { who in penaltyPeople(it) }.size }.maxByOrNull { it.second }
    }?.takeIf { it.second >= 2 }
    val recent = records.take(5).map { it.bet to penaltyPeople(it) }
    return OurStats(records.size, people, streak, recent)
}

/** 이번 주(월요일 0시) · 이번 달(1일 0시)의 시작 시각 */
private fun periodStart(period: Period, now: Long): Long {
    if (period == Period.ALL) return Long.MIN_VALUE
    val c = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    if (period == Period.MONTH) c.set(Calendar.DAY_OF_MONTH, 1)
    else {
        val back = (c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        c.add(Calendar.DAY_OF_MONTH, -back)
    }
    return c.timeInMillis
}
