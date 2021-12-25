package com.oracle.truffle.lama.nodes.controlflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BlockNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NodeInfo(shortName = "block", description = "The node implementing a source code block")
public final class LamaBlockNode extends LamaExpressionNode implements BlockNode.ElementExecutor<LamaExpressionNode> {

    @Node.Child
    private BlockNode<LamaExpressionNode> block;

    public LamaBlockNode(LamaExpressionNode[] block) {
        BlockNode<LamaExpressionNode> created = block != null ? BlockNode.create(block, this) : null;
        this.block = created;
    }

    public List<LamaExpressionNode> getStatements() {
        if (block == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(block.getElements()));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (this.block != null) {
            return this.block.executeGeneric(frame, BlockNode.NO_ARGUMENT);
        }
        return null;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        if (this.block != null) {
            this.block.executeVoid(frame, BlockNode.NO_ARGUMENT);
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame, LamaExpressionNode node, int index, int argument) {
        node.executeVoid(frame);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame, LamaExpressionNode node, int index, int argument) {
        return node.executeGeneric(frame);
    }
}
