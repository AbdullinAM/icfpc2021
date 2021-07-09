package ru.spbstu.icpfc2021.solver

import ru.spbstu.icpfc2021.model.Edge
import ru.spbstu.icpfc2021.model.Point
import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.wheels.MDMap
import ru.spbstu.wheels.MapToSet

class OtherDummySolver(val allHolePoints: Set<Point>,
                       val problem: Problem) {
    fun solve() {
        val ranking: MapToSet<Point, Edge> = MapToSet()
        for (edge in problem.figure.calculatedEdges) {
            ranking[edge.start] += edge
            ranking[edge.end] += edge
        }
        val initialPick = problem.figure.vertices.maxByOrNull { ranking[it].size }!!

        for (hpoint in allHolePoints) {

        }
    }
}