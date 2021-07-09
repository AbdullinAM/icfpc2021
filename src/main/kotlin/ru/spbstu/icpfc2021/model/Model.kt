package ru.spbstu.icpfc2021.model

import com.fasterxml.jackson.databind.ObjectMapper
import ru.spbstu.ktuples.Tuple2
import ru.spbstu.ktuples.jackson.registerKTuplesModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

typealias Point = Tuple2<Int, Int>
typealias Edge = Tuple2<Int, Int>

typealias Hole = List<Point>

data class Figure(
    val vertices: List<Point>,
    val edges: List<Edge>
)

data class Problem(
    val hole: Hole,
    val figure: Figure,
    val epsilon: Int
)

data class Pose(
    val vertices: List<Point>
)

private val om = ObjectMapper().registerKotlinModule().registerKTuplesModule()

fun readProblem(s: String) = om.readValue<Problem>(s)

