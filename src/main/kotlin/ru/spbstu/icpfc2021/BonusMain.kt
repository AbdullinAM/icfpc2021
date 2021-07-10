package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.model.*
import java.io.File

fun main(args: Array<String>) {
    val problems = args[0]
    val solutions = args[1]
    val res = args[2]

    val problemMap = File(problems).walkTopDown()
        .filter { it.isFile }
        .map { file ->
            val json = file.readText()
            val problem = readValue<Problem>(json)
            val num = file.nameWithoutExtension.toInt()

            Pair(num, problem)
        }.associateBy(Pair<Int, Problem>::first, Pair<Int, Problem>::second)

    val solutionMap = File(solutions).walkTopDown()
        .filter { it.isFile }
        .map { file ->
            val json = file.readText()
            val problem = readValue<Pose>(json)
            val num = file.nameWithoutExtension.toInt()

            Pair(num, problem)
        }.associateBy(Pair<Int, Pose>::first, Pair<Int, Pose>::second)

    val info = mutableListOf<BonusUse>()

    for ((n, p) in problemMap) {
        val s = solutionMap[n] ?: continue

        for (b in p.bonuses) {
            if (b.position !in s.vertices) continue

            info += BonusUse(b.bonus, b.problem)
        }
    }

    val bonusInfo = BonusInfo(info)

    writeValue(File(res).writer(), bonusInfo)
}
