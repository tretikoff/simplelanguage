package com.oracle.truffle.lama.test

import com.oracle.truffle.lama.LamaLanguage
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Source
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import kotlin.test.assertNotNull

class LamaTests {
    private val regression = File("tests/regression")
    private val testFiles = regression.listFiles() ?: error("Test files do not exist")
    private val orig = File("tests/regression/orig")

    @Test
    fun runTests() {
        val lamaFiles = testFiles.filter { it.name.endsWith(".lama") }.sortedBy { it.name }
        lamaFiles.forEach { runTest(it) }
    }

    fun runTest(src: File) {
        val input = testFiles.find { it.name == src.name.replace(".lama", ".input") }
        val output = orig.listFiles().find { it.name == src.name.replace(".lama", ".log") }
        assertNotNull(input)
        assertNotNull(output)


        var context: Context? = null
        try {
            val out = ByteArrayOutputStream()
            val builder =
                Context.newBuilder().allowExperimentalOptions(true).allowHostClassLookup { s: String? -> true }
                    .allowHostAccess(HostAccess.ALL).`in`(ByteArrayInputStream(input.readBytes())).out(out)
            context = builder.build()
            val printer = PrintWriter(out)
            run(context, src, printer)
            printer.flush()
            val actualOutput = String(out.toByteArray())
            Assert.assertEquals(src.name, output.readText(), actualOutput)
        } finally {
            context?.close()
        }
    }

    private fun run(context: Context, src: File, out: PrintWriter) {
        try {

            val source = Source.newBuilder(LamaLanguage.ID, src).build()
            context.eval(source)
        } catch (e: PolyglotException) {
            if (!e.isInternalError) {
                out.println(e.message)
            } else {
                throw e
            }
        }
    }
}