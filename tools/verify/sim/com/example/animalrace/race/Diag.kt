package com.example.animalrace.race

import com.example.animalrace.model.Animal
import kotlin.random.Random

/** 중반 선두가 이긴 판 중, 중반 이후 한 번 뒤집혔다가 재역전한 비율 (재역전 판이 실제로 보이는지). */
fun diag() {
    for (n in listOf(2, 6)) {
        val rnd = Random(1)
        var midLeaderWins = 0; var flipped = 0
        repeat(2000) {
            val race = Race(Animal.entries.shuffled(rnd).take(n), rnd)
            val winner = race.racers.minBy { race.finishTimeOf(it) }
            val fw = race.finishTimeOf(winner)
            var midLead = false; var lostAfter = false
            while (!race.done) {
                race.step(1 / 60.0)
                val leader = race.racers.filter { !it.finished }.maxByOrNull { it.x }
                if (race.t in fw * 0.5..fw * 0.5 + 0.02) midLead = leader === winner
                if (midLead && race.t > fw * 0.5 && race.t < fw * 0.95 && leader != null && leader !== winner && leader.x - winner.x > 6) lostAfter = true
            }
            if (midLead) { midLeaderWins++; if (lostAfter) flipped++ }
        }
        println("n=$n 중반 선두 우승 ${midLeaderWins}/2000, 그중 중간에 뒤집혔다 재역전 ${flipped}")
    }
}
