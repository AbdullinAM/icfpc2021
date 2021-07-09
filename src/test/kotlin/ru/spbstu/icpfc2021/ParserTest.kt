package ru.spbstu.icpfc2021

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.spbstu.icpfc2021.model.Problem
import kotlin.test.*
import ru.spbstu.icpfc2021.model.readValue
import java.io.File

class ParserTest {
    @Test
    fun testProblems() {
        for (i in 1..59) {
            val json = File("problems/$i.problem").readText()
            if (json.isEmpty()) continue
            println("$i.problem")
            println(readValue<Problem>(json))
        }
    }
}
