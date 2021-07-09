package ru.spbstu.icpfc2021.result

import ru.spbstu.icpfc2021.model.*
import java.io.File

fun saveResult(problem: Problem, figure: Figure) {
    val verifier = Verifier(problem)
    when (verifier.check(figure)) {
        Verifier.Status.OVERLAP ->  {
            println("Edges are overlapping")
            return
        }
        Verifier.Status.EDGE_VIOLATION -> {
            println("Edges violated!")
            return
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
    } else {
        println("Current solution is worse than existing solution")
    }
}