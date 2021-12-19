package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode

@NodeInfo(shortName = "&&")
class LamaLogicalAndNode(left: LamaExpressionNode?, right: LamaExpressionNode?) : LamaShortCircuitNode(left, right) {
    override fun isEvaluateRight(left: Boolean): Boolean {
        return left
    }

    override fun execute(left: Boolean, right: Boolean): Boolean {
        return left && right
    }
}