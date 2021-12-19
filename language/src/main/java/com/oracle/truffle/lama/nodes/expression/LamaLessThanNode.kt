package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.lama.nodes.LamaBinaryNode
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.dsl.Fallback
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.runtime.SLBigNumber
import com.oracle.truffle.lama.SLException

@NodeInfo(shortName = "<")
abstract class LamaLessThanNode : LamaBinaryNode() {
    @Specialization
    protected fun lessThan(left: Long, right: Long): Boolean {
        return left < right
    }

    @Fallback
    protected fun typeError(left: Any?, right: Any?): Any {
        throw SLException.typeError(this, left, right)
    }
}