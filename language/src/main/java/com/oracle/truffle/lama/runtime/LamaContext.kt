package com.oracle.truffle.lama.runtime

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal
import com.oracle.truffle.api.TruffleLanguage
import com.oracle.truffle.api.TruffleLanguage.ContextReference
import com.oracle.truffle.api.nodes.Node
import com.oracle.truffle.lama.LamaLanguage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

class LamaContext(language: LamaLanguage, @field:CompilationFinal private var env: TruffleLanguage.Env) {
    private val language: LamaLanguage

    /**
     * Returns the default input, i.e., the source for the [SLReadlnBuiltin]. To allow unit
     * testing, we do not use [System. in] directly.
     */
    val input: BufferedReader = BufferedReader(InputStreamReader(env.`in`()))

    /**
     * The default default, i.e., the output for the [LamaWriteBuiltin]. To allow unit
     * testing, we do not use [System.out] directly.
     */
    val output: PrintWriter = PrintWriter(env.out(), true)

    fun patchContext(newEnv: TruffleLanguage.Env) {
        env = newEnv
    }

    companion object {
        private val REFERENCE = ContextReference.create(
            LamaLanguage::class.java
        )

        fun get(node: Node?): LamaContext {
            return REFERENCE[node]
        }
    }

    init {
        this.language = language
    }
}