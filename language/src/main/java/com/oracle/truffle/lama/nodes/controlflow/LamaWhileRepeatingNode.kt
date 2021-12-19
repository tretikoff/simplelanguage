package com.oracle.truffle.lama.nodes.controlflow

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.Node
import com.oracle.truffle.api.nodes.RepeatingNode
import com.oracle.truffle.api.nodes.UnexpectedResultException
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.lama.nodes.util.LamaUnboxNodeGen

class LamaWhileRepeatingNode(
    conditionNode: LamaExpressionNode?,
    @Child private val bodyNode: LamaExpressionNode
) : Node(), RepeatingNode {
    @Child
    private val conditionNode: LamaExpressionNode = LamaUnboxNodeGen.create(conditionNode)

    override fun executeRepeating(frame: VirtualFrame): Boolean {
        /* Normal exit of the loop when loop condition is false. */
        if (!evaluateCondition(frame)) {
            return false
        }

        bodyNode.executeVoid(frame)
        /* Continue with next loop iteration. */
        return true
    }

    private fun evaluateCondition(frame: VirtualFrame): Boolean {
        return try {
            conditionNode.executeBoolean(frame)
        } catch (ex: UnexpectedResultException) {
            /*
             * The condition evaluated to a non-boolean result. This is a type error in the SL
             * program. We report it with the same exception that Truffle DSL generated nodes used to
             * report type errors.
             */
            throw UnsupportedSpecializationException(this, arrayOf<Node>(conditionNode), ex.result)
        }
    }

    override fun toString(): String {
        return LamaStatementNode.formatSourceSection(this)
    }
}