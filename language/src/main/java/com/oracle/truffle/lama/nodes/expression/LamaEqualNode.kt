package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.dsl.Specialization
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaBinaryNode
import com.oracle.truffle.lama.runtime.LamaNull
import com.oracle.truffle.lama.runtime.SLFunction

@NodeInfo(shortName = "==")
abstract class LamaEqualNode : LamaBinaryNode() {
    @Specialization
    protected fun doLong(left: Long, right: Long): Boolean {
        return left == right
    }

    @Specialization
    protected fun doBoolean(left: Boolean, right: Boolean): Boolean {
        return left == right
    }

    @Specialization
    protected fun doString(left: String, right: String): Boolean {
        return left == right
    }

    @Specialization
    protected fun doNull(left: LamaNull, right: LamaNull): Boolean {
        return left == right
    }

    @Specialization
    protected fun doFunction(left: SLFunction, right: Any): Boolean {
        return left == right
    }
}