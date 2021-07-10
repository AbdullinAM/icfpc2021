package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.gui.drawFigure
import ru.spbstu.icpfc2021.model.Verifier
import ru.spbstu.icpfc2021.model.readProblem
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.icpfc2021.solver.OtherDummySolver
import java.io.File

fun main(args: Array<String>) {
    val index = args[0].toInt()
    val json = File("problems/$index.problem").readText()
    println("$index.problem")
    val problem = readProblem(index, json)
    println(problem)
    val solver = OtherDummySolver(
        Verifier(problem).getHolePoints().toSet(),
        problem,
        findAllSolutions = false,
        showGraphics = true
    )
    val figure = solver.solve()

    saveResult(problem, figure)
    drawFigure(problem, figure)
//    drawFigure(problem)
}