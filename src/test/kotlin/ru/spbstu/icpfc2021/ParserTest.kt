package ru.spbstu.icpfc2021

import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.icpfc2021.model.readValue
import java.io.File
import kotlin.test.Test

class ParserTest {
    @Test
    fun testProblems() {
        for (i in 1..PROBLEM_LAST_INDEX) {
            val json = File("problems/$i.problem").readText()
            if (json.isEmpty()) continue
            println("$i.problem")
            println(readValue<Problem>(json))
        }
    }
}
