package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.dsl.Fallback
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.SLException
import com.oracle.truffle.lama.nodes.LamaBinaryNode

@NodeInfo(shortName = "/")
abstract class LamaDivNode : LamaBinaryNode() {
    @Specialization(rewriteOn = [ArithmeticException::class])
    @Throws(ArithmeticException::class)
    protected fun div(left: Long, right: Long): Long {
        val result = left / right
        if (left and right and result < 0) {
            throw ArithmeticException("long overflow")
        }
        return result
    }

    @Fallback
    protected fun typeError(left: Any?, right: Any?): Any {
        throw SLException.typeError(this, left, right)
    }
}