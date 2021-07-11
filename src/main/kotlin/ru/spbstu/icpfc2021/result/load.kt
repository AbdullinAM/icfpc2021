package ru.spbstu.icpfc2021.result

import ru.spbstu.icpfc2021.model.Figure
import ru.spbstu.icpfc2021.model.Pose
import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.icpfc2021.model.readValue
import java.io.File


fun loadSolution(problem: Problem): Figure {
    val resultFile = File("solutions/${problem.number}.sol").also {
        it.parentFile?.mkdirs()
    }
    val pose = readValue<Pose>(resultFile)
    return Figure(pose.vertices, problem.figure.edges)
}


fun loadInvalidSolution(problem: Problem): Figure {
    val resultFile = File("solutions/${problem.number}.invalid.sol").also {
        it.parentFile?.mkdirs()
    }
    val pose = readValue<Pose>(resultFile)
    return Figure(pose.vertices, problem.figure.edges)
}