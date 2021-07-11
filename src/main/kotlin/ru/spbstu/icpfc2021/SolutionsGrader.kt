package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.model.dislikes
import ru.spbstu.icpfc2021.model.readProblem
import ru.spbstu.icpfc2021.result.loadSolution
import java.io.File
import java.io.FileNotFoundException

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val solutionId = args[0].toInt()

        val json = File("problems/$solutionId.problem").readText()
        val problem = readProblem(solutionId, json)
        val solution = loadSolution(problem)
        print("${dislikes(problem.hole, solution.currentPose)}")
        return
    }

    for (index in 1..132) {
        val json = File("problems/$index.problem").readText()
        val problem = readProblem(index, json)
        try {
            val solution = loadSolution(problem)
            println("${problem.number} | ${dislikes(problem.hole, solution.currentPose)}")
        } catch (e: FileNotFoundException) {
            println("$index | no solution")
            continue
        }
    }
}