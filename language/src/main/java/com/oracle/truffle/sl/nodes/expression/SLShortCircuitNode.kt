package com.oracle.truffle.sl.nodes.expression

import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.UnexpectedResultException
import com.oracle.truffle.api.profiles.ConditionProfile
import com.oracle.truffle.lama.SLException
import com.oracle.truffle.lama.nodes.LamaExpressionNode

abstract class LamaShortCircuitNode(
    @Child private val left: LamaExpressionNode,
    @Child private val right: LamaExpressionNode
) : LamaExpressionNode() {
    private val evaluateRightProfile: ConditionProfile = ConditionProfile.createCountingProfile()
    override fun executeGeneric(frame: VirtualFrame?): Any {
        return executeLong(frame)
    }

    override fun executeLong(frame: VirtualFrame?): Long {
        val leftValue = try {
            left.executeLong(frame)
        } catch (e: UnexpectedResultException) {
            throw SLException.typeError(this, e.result, null)
        }
        val rightValue: Long = try {
            if (evaluateRightProfile.profile(isEvaluateRight(leftValue != 0L))) {
                right.executeLong(frame)
            } else {
                0
            }
        } catch (e: UnexpectedResultException) {
            throw SLException.typeError(this, leftValue, e.result)
        }
        return if (execute(leftValue != 0L, rightValue != 0L)) 1L else 0L
    }

    protected abstract fun isEvaluateRight(leftValue: Boolean): Boolean
    protected abstract fun execute(leftValue: Boolean, rightValue: Boolean): Boolean
}