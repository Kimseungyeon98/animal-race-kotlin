import com.example.animalrace.model.Animal
import com.example.animalrace.race.Race
import kotlin.math.abs
import kotlin.math.sqrt

/** 동물 × 순위 분포를 집계하고, 동물마다 "모든 순위가 같은 확률" 카이제곱 검정을 한다. */
fun run(label: String, races: Int, field: Int) {
    val all = Animal.entries
    val k = all.size
    val counts = Array(k) { LongArray(field) }
    val starts = LongArray(k)
    var leadChanges = 0L; var t1 = 0.0; var tLast = 0.0
    val t0 = System.currentTimeMillis()
    repeat(races) {
        val race = Race(all.shuffled().take(field))
        while (!race.done) race.step(1 / 60.0)
        val order = race.ranking()
        order.forEachIndexed { i, r -> counts[r.animal.ordinal][i]++; starts[r.animal.ordinal]++ }
        leadChanges += race.leadChanges; t1 += order.first().finish!!; tLast += order.last().finish!!
    }
    val sec = (System.currentTimeMillis() - t0) / 1000.0
    println("=== $label: ${"%,d".format(races)}경기 × ${field}마리 (소요 %.0f초) ===".format(sec))
    println("1등 평균 %.2f초, 꼴찌 평균 %.2f초, 경기당 선두 교체 %.2f회".format(t1 / races, tLast / races, leadChanges.toDouble() / races))
    println("공정 기준: 각 순위 %.2f%%  |  표 값 = 해당 순위에 온 비율(%%)".format(100.0 / field))
    val head = (1..field).joinToString(" ") { "%5s".format("${it}위") }
    println("%-14s %s %8s %7s".format("동물", head, "최대편차", "χ²"))
    var worst = 0.0; var chiTotal = 0.0
    for (a in all) {
        val n = starts[a.ordinal].toDouble()
        val e = n / field
        val row = counts[a.ordinal]
        val chi = row.sumOf { (it - e) * (it - e) / e }
        chiTotal += chi
        val dev = row.maxOf { abs(it / n - 1.0 / field) } * 100
        worst = maxOf(worst, dev)
        println("%-14s %s %7.2f%% %7.1f".format("${a.emoji}${a.label}", row.joinToString(" ") { "%5.2f".format(it / n * 100) }, dev, chi))
    }
    val se = sqrt((1.0 / field) * (1 - 1.0 / field) / (races.toDouble() * field / k)) * 100
    println("가장 큰 편차 %.2f%%p (표본오차 1σ ≈ %.2f%%p)".format(worst, se))
    println("${k}종 합산 χ² = %.1f (자유도 %d, 기대값 = 자유도)".format(chiTotal, k * (field - 1)))
    println()
}

fun main() {
    run("20종 전원 출전", 100_000, 20)
    run("10마리 출전 (20종 중 무작위)", 100_000, 10)
    run("2마리 출전 (20종 중 무작위)", 100_000, 2)
}
