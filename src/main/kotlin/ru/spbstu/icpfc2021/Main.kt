package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.gui.drawFigure
import ru.spbstu.icpfc2021.model.readProblem
import java.io.File

fun main(args: Array<String>) {
    val index = args[0].toInt()
    val json = File("problems/$index.problem").readText()
    println("$index.problem")
    val problem = readProblem(index, json)
    println(problem)
    drawFigure(problem)
}