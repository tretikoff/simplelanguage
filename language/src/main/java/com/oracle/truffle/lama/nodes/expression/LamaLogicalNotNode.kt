package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.dsl.Fallback
import com.oracle.truffle.api.dsl.NodeChild
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.SLException
import com.oracle.truffle.lama.nodes.LamaExpressionNode

@NodeChild("valueNode")
@NodeInfo(shortName = "!")
abstract class LamaLogicalNotNode : LamaExpressionNode() {
    @Specialization
    protected fun doBoolean(value: Boolean) = !value

    @Fallback
    protected fun typeError(value: Any?): Any {
        throw SLException.typeError(this, value)
    }
}