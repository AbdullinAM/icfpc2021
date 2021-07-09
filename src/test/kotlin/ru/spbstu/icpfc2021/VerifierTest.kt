package ru.spbstu.icpfc2021

import org.junit.jupiter.api.Test
import ru.spbstu.icpfc2021.model.Pose
import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.icpfc2021.model.Verifier
import ru.spbstu.icpfc2021.model.Verifier.Status.OK
import ru.spbstu.icpfc2021.model.Verifier.Status.OVERLAP
import ru.spbstu.icpfc2021.model.readValue
import java.io.File
import kotlin.test.assertEquals

class VerifierTest {

    @Test
    fun testProblems() {
        for (i in 1..PROBLEM_LAST_INDEX) {
            val jsonP = File("problems/$i.problem").readText()
            if (jsonP.isEmpty()) continue

            val problem = readValue<Problem>(jsonP)

            val verifier = Verifier(problem)

            assertEquals(OVERLAP, verifier.check(problem.figure), "Oops: $i.problem")

            val solutionFile = File("solutions/$i.sol")
            if (!solutionFile.exists()) continue

            val jsonS = solutionFile.readText()
            if (jsonS.isEmpty()) continue

            val solution = readValue<Pose>(jsonS)
            val solutionFigure = problem.figure.copy(vertices = solution.vertices)

            assertEquals(OK, verifier.check(solutionFigure), "Oops: $i.sol")
        }
    }

}
