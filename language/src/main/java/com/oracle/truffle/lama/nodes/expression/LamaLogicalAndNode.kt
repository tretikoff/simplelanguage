package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.sl.nodes.expression.LamaShortCircuitNode

@NodeInfo(shortName = "&&")
class LamaLogicalAndNode(left: LamaExpressionNode, right: LamaExpressionNode) : LamaShortCircuitNode(left, right) {
    override fun isEvaluateRight(leftValue: Boolean): Boolean {
        return leftValue
    }

    override fun execute(leftValue: Boolean, rightValue: Boolean): Boolean {
        return leftValue && rightValue
    }
}