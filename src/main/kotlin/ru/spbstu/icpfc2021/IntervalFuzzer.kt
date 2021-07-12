package ru.spbstu.icpfc2021

import kotlinx.coroutines.coroutineScope
import ru.spbstu.icpfc2021.model.Pose
import ru.spbstu.icpfc2021.model.dislikes
import ru.spbstu.icpfc2021.model.readProblem
import ru.spbstu.icpfc2021.model.readValue
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.icpfc2021.solver.fuzzer
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

val fuzzerIsRunning = AtomicBoolean(false)

@OptIn(ExperimentalTime::class)
private inline fun <reified T> Timer.withTimeout(duration: Duration, computation: () -> T): T {
    fuzzerIsRunning.set(true)
    val cancelation = object : TimerTask() {
        override fun run() {
            fuzzerIsRunning.set(false)
        }
    }
    schedule(cancelation, duration.inWholeMilliseconds)
    val result = computation()
    cancelation.cancel()
    return result
}

@OptIn(ExperimentalTime::class)
suspend fun main(args: Array<String>) = coroutineScope {
    val startInclusive = args[0].toInt()
    val endExclusive = args[1].toInt()
    val defaultDuration = Duration.Companion.minutes(60)
    val timeout = if (args.size > 2) {
        args[2].toIntOrNull()?.let { Duration.Companion.minutes(120) } ?: defaultDuration
    } else {
        defaultDuration
    }

    val timer = Timer()
    for (index in startInclusive until endExclusive) {
        val problemJson = File("problems/$index.problem").readText()
        val problem = readProblem(index, problemJson)
        println("Running on problem $index")

        val solutionJson = File("solutions/$index.sol").readText()
        val solution = readValue<Pose>(solutionJson)

        val startFigure = problem.figure.copy(vertices = solution.vertices)
        val fuzzer = fuzzer(
            problem, startFigure,
            strictlyLowerDislikes = args.contains("--strict"),
            invalidityMode = args.contains("--invalid"),
            explosionMode = args.contains("--explode"),
        )
        timer.withTimeout(timeout) {
            while (true) {
                if (!fuzzerIsRunning.get()) return@withTimeout
                fuzzer.fuzz(scope = this)
                saveResult(problem, fuzzer.currentFigure, false)

                if (fuzzer.totalBestScore == 0L) {
                    println("Guess, we're done here")
                    return@withTimeout
                }
            }
        }
        fuzzerIsRunning.set(false)
    }
    timer.cancel()
}