package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.lama.nodes.LamaBinaryNode
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.dsl.Fallback
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.runtime.SLBigNumber
import com.oracle.truffle.lama.SLException

/**
 * This class is similar to the [LamaLessThanNode].
 */
@NodeInfo(shortName = "<=")
abstract class LamaLessOrEqualNode : LamaBinaryNode() {
    @Specialization
    protected fun lessOrEqual(left: Long, right: Long): Boolean {
        return left <= right
    }

    @Specialization
    @TruffleBoundary
    protected fun lessOrEqual(left: SLBigNumber, right: SLBigNumber?): Boolean {
        return left.compareTo(right) <= 0
    }

    @Fallback
    protected fun typeError(left: Any?, right: Any?): Any {
        throw SLException.typeError(this, left, right)
    }
}