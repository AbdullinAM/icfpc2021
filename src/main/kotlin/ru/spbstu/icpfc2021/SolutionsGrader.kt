package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.model.dislikes
import ru.spbstu.icpfc2021.model.readProblem
import ru.spbstu.icpfc2021.result.loadSolution
import java.io.File
import java.io.FileNotFoundException

fun main(args: Array<String>) {
    for (index in 1..106) {
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