package ru.spbstu.icpfc2021.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ru.spbstu.ktuples.jackson.registerKTuplesModule
import ru.spbstu.ktuples.zip
import java.io.File
import java.io.InputStream
import java.io.Writer
import kotlin.math.abs

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class Point(
    val x: Int, val y: Int
) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
}

fun Int.sqr() = this.toLong() * this.toLong()

infix fun Point.squaredDistance(other: Point) = (x - other.x).sqr() + (y - other.y).sqr()

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class DataEdge(
    val startIndex: Int, val endIndex: Int
)

data class Edge(val start: Point, val end: Point) {
    val squaredLength get() = start.squaredDistance(end)
}

fun checkCorrect(from: Edge, to: Edge, epsilon: Int) =
    abs(from.squaredLength / to.squaredLength - 1) <= epsilon / 1_000_000.0
fun checkCorrect(from: Figure, to: Figure, epsilon: Int) =
    zip(from.calculatedEdges, to.calculatedEdges).all { (a, b) -> checkCorrect(a, b, epsilon) }

typealias Hole = List<Point>

data class Figure(
    val vertices: List<Point>,
    val edges: List<DataEdge>
) {
    val calculatedEdges by lazy { edges.map { Edge(vertices[it.startIndex], vertices[it.endIndex]) }}

    fun moveAll(dp: Point) = copy(vertices = vertices.map { it + dp })
}

data class Problem(
    val hole: Hole,
    val figure: Figure,
    val epsilon: Int
)

data class Pose(
    val vertices: List<Point>
)

fun dislikes(hole: Hole, pose: Pose) =
    hole.sumOf { hp -> pose.vertices.minOf { vp -> hp.squaredDistance(vp) } }

@PublishedApi
internal val om = ObjectMapper().registerKotlinModule().registerKTuplesModule()

inline fun <reified T> readValue(file: File): T = om.readValue(file)
inline fun <reified T> readValue(file: String): T = om.readValue(file)
inline fun <reified T> readValue(file: InputStream): T = om.readValue(file)

fun readProblem(json: String): Problem = readValue(json)

inline fun <reified T> writeValue(w: Writer, value: T) = om.writeValue(w, value)
inline fun <reified T> T.toJsonString() = om.writeValueAsString(this)