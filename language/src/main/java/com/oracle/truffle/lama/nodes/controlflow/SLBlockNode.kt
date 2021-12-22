
package com.oracle.truffle.lama.nodes.controlflow

import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.api.nodes.BlockNode.ElementExecutor
import com.oracle.truffle.api.nodes.BlockNode
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo
import java.util.Collections
import java.util.Arrays
@NodeInfo(shortName = "block", description = "The node implementing a source code block")
class SLBlockNode(bodyNodes: Array<LamaStatementNode>) : LamaStatementNode(), ElementExecutor<LamaStatementNode> {
    @Child
    private val block: BlockNode<LamaStatementNode>?

    override fun executeVoid(frame: VirtualFrame?) {
        block?.executeVoid(frame, BlockNode.NO_ARGUMENT)
    }

    val statements: List<LamaStatementNode>
        get() = if (block == null) {
            emptyList()
        } else Collections.unmodifiableList(Arrays.asList(*block.elements))

    override fun executeVoid(frame: VirtualFrame, node: LamaStatementNode, index: Int, argument: Int) {
        node.executeVoid(frame)
    }

    init {
        block = if (bodyNodes.isNotEmpty()) BlockNode.create(bodyNodes, this) else null
    }
}