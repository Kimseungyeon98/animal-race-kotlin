package com.example.animalrace.race

import com.example.animalrace.model.Animal
import kotlin.random.Random

fun main() {
    diag()
    val rnd = Random(42)
    for (n in listOf(2, 3, 4, 6, 10)) {
        val races = 3000
        var leadChanges = 0.0; var midLeaderLoses = 0; var comeback = 0; var winnerBoost = 0
        var maxFreePace = 0.0; var minFreePace = 9.0; var skillEvents = 0.0; var noDrama = 0
        val causes = HashMap<String, Int>()
        repeat(races) {
            val race = Race(Animal.entries.shuffled(rnd).take(n), rnd)
            val winner = race.racers.minBy { race.finishTimeOf(it) }
            val fw = race.finishTimeOf(winner)
            if (race.racers.all { it.segs.isEmpty() }) noDrama++
            if (winner.segs.any { (it.mult ?: 1.0) > 1.0 }) winnerBoost++
            var midDone = false; var lateDone = false
            val prevX = DoubleArray(n)
            val dt = 1 / 60.0
            while (!race.done) {
                race.racers.forEachIndexed { i, r -> prevX[i] = r.x }
                race.step(dt)
                race.racers.forEachIndexed { i, r ->
                    if (!r.finished && race.t > 1.6 && !race.inSkill(r, race.t) && !race.inSkill(r, race.t - dt)) {
                        val pace = (r.x - prevX[i]) / dt / race.baseSpeed(r, race.t)
                        maxFreePace = maxOf(maxFreePace, pace); minFreePace = minOf(minFreePace, pace)
                    }
                }
                if (!midDone && race.t >= fw * 0.5) { midDone = true; if (race.ranking()[0] !== winner) midLeaderLoses++ }
                if (!lateDone && race.t >= fw * 0.62) { lateDone = true; if (race.ranking().indexOf(winner) >= 2) comeback++ }
            }
            leadChanges += race.leadChanges
            val ev = race.view().let { _ -> race.racers.sumOf { it.uses } }
            skillEvents += ev
        }
        println("n=$n  역전 평균 %.1f회 | 중반 선두≠우승 %.0f%% | 62%%시점 우승자 3등 이하 %.0f%% | 우승자 가속 %.0f%% | 스킬 사용 평균 %.1f | 전개 없음 %d | 비스킬 구간 속도 %.2f~%.2f".format(
            leadChanges / races, 100.0 * midLeaderLoses / races, 100.0 * comeback / races, 100.0 * winnerBoost / races,
            skillEvents / races, noDrama, minFreePace, maxFreePace))
    }
    // 예시 한 판: 신규 동물 위주
    val race = Race(listOf(Animal.SKUNK, Animal.HEDGEHOG, Animal.CAPYBARA, Animal.SLOTH, Animal.HAMSTER, Animal.PANDA, Animal.MONKEY, Animal.DUCK), Random(5))
    val seen = HashSet<Int>()
    while (!race.done) {
        race.step(1 / 60.0)
        race.view().recentEvents.filter { seen.add(it.id) }.reversed().forEach { println("  %5.2f  %s".format(it.t, it.text)) }
    }
    println("  결과: " + race.ranking().joinToString(" > ") { it.animal.label })
    // 신규 상호작용 빈도
    val counts = HashMap<String, Int>()
    val r2 = Random(9)
    repeat(3000) {
        val rc = Race(Animal.entries.shuffled(r2).take(10), r2)
        val seen2 = HashSet<Int>()
        while (!rc.done) {
            rc.step(1 / 60.0)
            rc.view().recentEvents.filter { seen2.add(it.id) }.forEach { e ->
                listOf("방귀 폭탄", "가시로 따끔", "끄떡없음", "막아냄", "해바라기씨", "대나무 냠냠", "하품", "진심 모드", "꽈당").forEach { key ->
                    if (key in e.text) counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
    }
    println("  10마리 3000판 장면 수: $counts")
}
