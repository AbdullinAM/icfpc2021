package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.model.Verifier
import ru.spbstu.icpfc2021.model.readProblem
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.icpfc2021.solver.OtherDummySolver
import java.io.File

fun main(args: Array<String>) {
    val from = args[0].toInt()
    val to = args[1].toInt()
    for (index in from..to) {
        println("Solving problem $index")
        val json = File("problems/$index.problem").readText()
        println("$index.problem")
        val problem = readProblem(index, json)
        println(problem)
        val solver = OtherDummySolver(
            Verifier(problem).getHolePoints().toSet(),
            problem
        )
        val figure = solver.solve()

        saveResult(problem, figure)
    }
}