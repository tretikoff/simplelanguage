package com.oracle.truffle.lama.nodes.controlflow

import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.api.nodes.UnexpectedResultException
import com.oracle.truffle.api.profiles.ConditionProfile
import com.oracle.truffle.lama.SLException
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.lama.runtime.LamaNull

@NodeInfo(shortName = "if", description = "The node implementing a condional statement")
class LamaIfNode(
    @Child private val conditionNode: LamaExpressionNode,
    @Child private val thenPartNode: LamaExpressionNode,
    @Child private val elsePartNode: LamaExpressionNode?,
) : LamaExpressionNode() {
    private val condition = ConditionProfile.createCountingProfile()
    override fun executeVoid(frame: VirtualFrame) {
        if (condition.profile(evaluateCondition(frame))) {
            thenPartNode.executeVoid(frame)
        } else {
            elsePartNode?.executeVoid(frame)
        }
    }

    override fun executeGeneric(frame: VirtualFrame): Any {
        return if (condition.profile(evaluateCondition(frame))) {
            thenPartNode.executeGeneric(frame)
        } else {
            elsePartNode?.executeGeneric(frame) ?: LamaNull.SINGLETON
        }
    }

    private fun evaluateCondition(frame: VirtualFrame): Boolean {
        return try {
            conditionNode.executeBoolean(frame)
        } catch (ex: UnexpectedResultException) {
            throw SLException.typeError(this, ex.result)
        }
    }
}