package com.oracle.truffle.lama.nodes.controlflow

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.LoopNode
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.lama.runtime.LamaNull

@NodeInfo(shortName = "while", description = "The node implementing a while loop")
class LamaWhileNode(conditionNode: LamaExpressionNode?, bodyNode: LamaStatementNode?) : LamaExpressionNode() {
    @Child
    private val loopNode: LoopNode

    override fun executeGeneric(frame: VirtualFrame?): Any {
        loopNode.execute(frame)
        return LamaNull.SINGLETON
    }

    init {
        loopNode = Truffle.getRuntime().createLoopNode(
            LamaWhileRepeatingNode(
                conditionNode,
                bodyNode
            )
        )
    }
}