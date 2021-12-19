package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.dsl.Fallback
import com.oracle.truffle.lama.nodes.LamaBinaryNode
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.nodes.NodeInfo
import java.lang.ArithmeticException
import com.oracle.truffle.lama.SLException

/**
 * This class is similar to the extensively documented [LamaAddNode].
 */
@NodeInfo(shortName = "-")
abstract class LamaSubNode : LamaBinaryNode() {
    @Specialization(rewriteOn = [ArithmeticException::class])
    protected fun sub(left: Long, right: Long): Long {
        return Math.subtractExact(left, right)
    }

    @Fallback
    protected fun typeError(left: Any?, right: Any?): Any {
        throw SLException.typeError(this, left, right)
    }
}