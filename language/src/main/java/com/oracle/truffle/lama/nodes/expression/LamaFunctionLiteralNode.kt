package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.CompilerAsserts
import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.LamaLanguage
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.lama.runtime.LamaContext
import com.oracle.truffle.lama.runtime.SLFunction

@NodeInfo(shortName = "func")
class LamaFunctionLiteralNode(
    private val functionName: String
) : LamaExpressionNode() {
    @CompilationFinal
    private var cachedFunction: SLFunction? = null
    override fun executeGeneric(frame: VirtualFrame): SLFunction {
        val l = LamaLanguage[this]
        CompilerAsserts.partialEvaluationConstant<Any>(l)
        var function: SLFunction?
        if (l.isSingleContext()) {
            function = cachedFunction
            if (function == null) {
                /* We are about to change a @CompilationFinal field. */
                CompilerDirectives.transferToInterpreterAndInvalidate()
                /* First execution of the node: lookup the function in the function registry. */function =
                    LamaContext.get(this).getFunctionRegistry().lookup(
                        functionName, true
                    )
                cachedFunction = function
            }
        } else {
            /*
             * We need to rest the cached function otherwise it might cause a memory leak.
             */
            if (cachedFunction != null) {
                CompilerDirectives.transferToInterpreterAndInvalidate()
                cachedFunction = null
            }
            // in the multi-context case we are not allowed to store
            // SLFunction objects in the AST. Instead we always perform the lookup in the hash map.
            function = LamaContext.get(this).getFunctionRegistry().lookup(functionName, true)
        }
        return function
    }
}