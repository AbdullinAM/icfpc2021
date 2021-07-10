package ru.spbstu.icpfc2021

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import ru.spbstu.icpfc2021.model.Verifier
import ru.spbstu.icpfc2021.model.readProblem
import ru.spbstu.icpfc2021.solver.OtherDummySolver
import ru.spbstu.icpfc2021.solver.SolverMode
import java.io.File

suspend fun main(args: Array<String>) {
    val from = args[0].toInt()
    val to = args[1].toInt()
    coroutineScope {
        (from..to).map { index ->
            async {
                val json = File("problems/$index.problem").readText()
                println("$index.problem")
                val problem = readProblem(index, json)
                println(problem)
                val solver = OtherDummySolver(
                    Verifier(problem).getHolePoints().toSet(),
                    problem,
                    findAllSolutions = false,
                    showGraphics = false,
                    mode = SolverMode.RANDOM
                )
                solver.solve()
            }
        }.joinAll()
    }
}
