package ru.spbstu.icpfc2021

import edu.mcgill.kaliningraph.LGVertex
import edu.mcgill.kaliningraph.LabeledEdge
import edu.mcgill.kaliningraph.LabeledGraph
import edu.mcgill.kaliningraph.show
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.model.Link
import ru.spbstu.icpfc2021.model.*
import java.io.File

fun main(args: Array<String>) {
    val problems = args[0]
    val solutions = args[1]
    val res = args[2]

    val problemMap = File(problems).walkTopDown()
        .filter { it.isFile }
        .map { file ->
            val json = file.readText()
            val problem = readValue<Problem>(json)
            val num = file.nameWithoutExtension.toInt()

            Pair(num, problem)
        }.associateBy(Pair<Int, Problem>::first, Pair<Int, Problem>::second)

    val solutionMap = File(solutions).walkTopDown()
        .filter { it.isFile }
        .map { file ->
            val json = file.readText()
            val problem = readValue<Pose>(json)
            val num = file.nameWithoutExtension.toInt()

            Pair(num, problem)
        }.associateBy(Pair<Int, Pose>::first, Pair<Int, Pose>::second)

    val info = mutableListOf<BonusUse>()

    for ((n, p) in problemMap) {
        val s = solutionMap[n] ?: continue

        for (b in p.bonuses.orEmpty()) {
            if (b.position !in s.vertices) continue

            info += BonusUse(b.bonus, b.problem)
        }
    }

    val bonusInfo = BonusInfo(info)

    writeValue(File(res).writer(), bonusInfo)

    var graph = LabeledGraph {}

    for ((n, p) in problemMap) {
        graph += LabeledGraph {
            for (b in p.bonuses.orEmpty()) {
                LGVertex("$n") + MyLabeledEdge(LGVertex("$n"), LGVertex("${b.problem}"), "${b.bonus}")
            }
        }
    }

    graph.show()
}

class MyLabeledEdge(s: LGVertex, t: LGVertex, l: String) : LabeledEdge(s, t, l) {
    override fun render(): Link {
        return super.render().also { it.add(Label.of(label)) }
    }
}
