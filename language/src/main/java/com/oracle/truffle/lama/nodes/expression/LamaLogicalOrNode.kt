package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode

@NodeInfo(shortName = "!!")
class LamaLogicalOrNode(left: LamaExpressionNode?, right: LamaExpressionNode?) : LamaShortCircuitNode(left, right) {
    protected fun isEvaluateRight(left: Boolean): Boolean {
        return !left
    }

    protected fun execute(left: Boolean, right: Boolean): Boolean {
        return left || right
    }
}