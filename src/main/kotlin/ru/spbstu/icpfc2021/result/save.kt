package ru.spbstu.icpfc2021.result

import ru.spbstu.icpfc2021.model.*
import java.io.File

fun saveResult(problem: Problem, figure: Figure): Boolean {
    val verifier = Verifier(problem)
    when (verifier.check(figure)) {
        Verifier.Status.OVERLAP -> {
            println("Edges are overlapping")
            return false
        }
        Verifier.Status.EDGE_VIOLATION -> {
            println("Edges violated!")
            return false
        }
        Verifier.Status.OK -> println("Verification successful")
    }

    val resultFile = File("solutions/${problem.number}.sol").also {
        it.parentFile?.mkdirs()
    }
    val previousSolution = try {
        readValue<Pose>(resultFile)
    } catch (e: Throwable) {
        println("Could not read previous solution: ${e.message}")
        null
    }

    val currentPose = figure.currentPose
    val currentDislikes = dislikes(problem.hole, currentPose)
    val previousDislikes = previousSolution?.let { dislikes(problem.hole, it) } ?: Long.MAX_VALUE
    if (currentDislikes < previousDislikes) {
        resultFile.writeText(currentPose.toJsonString())
        println("Solution saved")
        return true
    } else {
        println("Current solution is worse than existing solution")
        return false
    }
}

fun saveInvalidResult(problem: Problem, figure: Figure): Boolean {
    val resultFile = File("solutions/${problem.number}.invalid.sol").also {
        it.parentFile?.mkdirs()
    }

    val currentPose = figure.currentPose
    resultFile.writeText(currentPose.toJsonString())
    println("Invalid saved")
    return true
}


fun mergeResults(problemFolder: String, outputFolder: String, vararg solutionFolders: String) {
    val problems = File(problemFolder).listFiles()?.map { readProblem(it.nameWithoutExtension.toInt(), it.readText()) }
        ?: throw IllegalStateException()

    for (problem in problems) {
        println("Merging solutions for problem ${problem.number}")

        val candidateSolutions = solutionFolders.mapNotNull {
            val solutionFile = File("$it/${problem.number}.sol")
            if (solutionFile.exists()) solutionFile.readText() else null
        }.map { readValue<Pose>(it) }.toSet()
        if (candidateSolutions.isEmpty()) {
            println("No solutions for problem ${problem.number}")
            continue
        }

        val bestSolutions = candidateSolutions.minByOrNull { dislikes(problem.hole, it) }!!
        println("Updated solution for problem ${problem.number}")
        val resultFile = File("$outputFolder/${problem.number}.sol").also {
            it.parentFile?.mkdirs()
        }
        resultFile.writeText(bestSolutions.toJsonString())
    }
}