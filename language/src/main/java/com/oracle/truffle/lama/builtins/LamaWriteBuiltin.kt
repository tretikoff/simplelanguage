package com.oracle.truffle.lama.builtins

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.interop.InteropLibrary
import com.oracle.truffle.api.library.CachedLibrary
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.runtime.LamaContext
import com.oracle.truffle.lama.runtime.SLLanguageView

@NodeInfo(shortName = "write")
abstract class LamaWriteBuiltin : LamaBuiltinNode() {
    @Specialization
    @TruffleBoundary
    fun println(value: Any, @CachedLibrary(limit = "3") interop: InteropLibrary): Any {
        LamaContext.get(this).output.println(interop.toDisplayString(SLLanguageView.forValue(value)))
        return value
    }
}