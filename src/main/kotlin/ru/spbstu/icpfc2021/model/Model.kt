package ru.spbstu.icpfc2021.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import ru.spbstu.ktuples.Tuple2
import ru.spbstu.ktuples.jackson.registerKTuplesModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.io.InputStream
import java.io.Writer

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class Point(
    val x: Int, val y: Int
)

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class Edge(
    val startIndex: Int, val endIndex: Int
)

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

@PublishedApi
internal val om = ObjectMapper().registerKotlinModule().registerKTuplesModule()

inline fun <reified T> readValue(file: File): T = om.readValue(file)
inline fun <reified T> readValue(file: String): T = om.readValue(file)
inline fun <reified T> readValue(file: InputStream): T = om.readValue(file)

fun readProblem(json: String): Problem = readValue(json)

inline fun <reified T> writeValue(w: Writer, value: T) = om.writeValue(w, value)
inline fun <reified T> T.toJsonString() = om.writeValueAsString(this)