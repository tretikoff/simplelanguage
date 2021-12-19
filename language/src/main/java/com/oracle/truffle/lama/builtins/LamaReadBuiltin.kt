package com.oracle.truffle.lama.builtins

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.SLException
import com.oracle.truffle.lama.runtime.LamaContext
import java.io.BufferedReader
import java.io.IOException

@NodeInfo(shortName = "read")
abstract class LamaReadBuiltin : LamaBuiltinNode() {
    @Specialization
    fun read(): String {
        return doRead(LamaContext.get(this).input) ?: ""
    }

    @TruffleBoundary
    private fun doRead(`in`: BufferedReader): String? {
        return try {
            `in`.readLine()
        } catch (ex: IOException) {
            throw SLException(ex.message, this)
        }
    }
}