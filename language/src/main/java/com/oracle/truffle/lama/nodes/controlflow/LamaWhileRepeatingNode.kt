package com.oracle.truffle.lama.nodes.controlflow

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.Node
import com.oracle.truffle.api.nodes.RepeatingNode
import com.oracle.truffle.api.nodes.UnexpectedResultException
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.lama.nodes.LamaStatementNode

class LamaWhileRepeatingNode(
    @Child private val conditionNode: LamaExpressionNode?,
    @Child private val bodyNode: LamaExpressionNode
) : Node(), RepeatingNode {
    override fun executeRepeating(frame: VirtualFrame): Boolean {
        if (!evaluateCondition(frame)) {
            return false
        }

        bodyNode.executeVoid(frame)
        return true
    }

    private fun evaluateCondition(frame: VirtualFrame): Boolean {
        return try {
            conditionNode?.executeLong(frame) != 0L
        } catch (ex: UnexpectedResultException) {
            throw UnsupportedSpecializationException(this, arrayOf(conditionNode), ex.result)
        }
    }

    override fun toString(): String {
        return LamaStatementNode.formatSourceSection(this)
    }
}