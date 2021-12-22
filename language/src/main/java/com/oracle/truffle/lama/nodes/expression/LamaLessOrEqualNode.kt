package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.dsl.Fallback
import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.SLException
import com.oracle.truffle.lama.nodes.LamaBinaryNode

@NodeInfo(shortName = "<=")
abstract class LamaLessOrEqualNode : LamaBinaryNode() {
    @Specialization
    protected fun lessOrEqual(left: Long, right: Long): Boolean {
        return left <= right
    }


    @Fallback
    protected fun typeError(left: Any?, right: Any?): Any {
        throw SLException.typeError(this, left, right)
    }
}