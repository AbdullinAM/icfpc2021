package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.gui.drawProblem
import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.icpfc2021.model.readValue
import java.io.File

fun main(args: Array<String>) {
    val index = args[0].toInt()
    val json = File("problems/$index.problem").readText()
    println("$index.problem")
    val problem = readValue<Problem>(json)
    println(problem)
    drawProblem(problem)
}