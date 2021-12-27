
package com.oracle.truffle.lama.nodes.controlflow

import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.api.nodes.BlockNode.ElementExecutor
import com.oracle.truffle.api.nodes.BlockNode
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import java.util.Collections
import java.util.Arrays

// decided to do it with Expressions since it's easier to pass by, Kotlin still does not compile ((
@NodeInfo(shortName = "block", description = "The node implementing a source code block")
class LamaBlockNodeKt(bodyNodes: Array<LamaExpressionNode>) : LamaExpressionNode(), ElementExecutor<LamaExpressionNode> {
    @Child
    private val block: BlockNode<LamaExpressionNode>?

    override fun executeVoid(frame: VirtualFrame?) {
        block?.executeVoid(frame, BlockNode.NO_ARGUMENT)
    }

    override fun executeGeneric(frame: VirtualFrame?): Any? {
        return block?.executeGeneric(frame, BlockNode.NO_ARGUMENT)
    }

    val statements: List<LamaExpressionNode>
        get() = if (block == null) {
            emptyList()
        } else Collections.unmodifiableList(Arrays.asList(*block.elements))

    override fun executeVoid(frame: VirtualFrame, node: LamaExpressionNode, index: Int, argument: Int) {
        node.executeVoid(frame)
    }

    init {
        block = if (bodyNodes.isNotEmpty()) BlockNode.create(bodyNodes, this) else null
    }
}