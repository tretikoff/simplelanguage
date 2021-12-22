
package com.oracle.truffle.lama.nodes;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.lama.LamaLanguage;
import com.oracle.truffle.lama.builtins.LamaBuiltinNode;
import com.oracle.truffle.lama.nodes.local.LamaWriteLocalVariableNode;
import com.oracle.truffle.lama.nodes.local.LamaReadArgumentNode;
import com.oracle.truffle.lama.runtime.LamaContext;

@NodeInfo(language = "SL", description = "The root of all SL execution trees")
public class SLRootNode extends RootNode {
    /** The function body that is executed, and specialized during execution. */
    @Child private LamaExpressionNode bodyNode;

    /** The name of the function, for printing purposes only. */
    private final String name;

    private boolean isCloningAllowed;

    private final SourceSection sourceSection;

    @CompilerDirectives.CompilationFinal(dimensions = 1) private volatile LamaWriteLocalVariableNode[] argumentNodesCache;

    public SLRootNode(LamaLanguage language, FrameDescriptor frameDescriptor, LamaExpressionNode bodyNode, SourceSection sourceSection, String name) {
        super(language, frameDescriptor);
        this.bodyNode = bodyNode;
        this.name = name;
        this.sourceSection = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert LamaContext.Companion.get(this) != null;
        return bodyNode.executeGeneric(frame);
    }

    public LamaExpressionNode getBodyNode() {
        return bodyNode;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setCloningAllowed(boolean isCloningAllowed) {
        this.isCloningAllowed = isCloningAllowed;
    }

    @Override
    public boolean isCloningAllowed() {
        return isCloningAllowed;
    }

    @Override
    public String toString() {
        return "root " + name;
    }

    public final LamaWriteLocalVariableNode[] getDeclaredArguments() {
        LamaWriteLocalVariableNode[] argumentNodes = argumentNodesCache;
        if (argumentNodes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            argumentNodesCache = argumentNodes = findArgumentNodes();
        }
        return argumentNodes;
    }

    private LamaWriteLocalVariableNode[] findArgumentNodes() {
        List<LamaWriteLocalVariableNode> writeArgNodes = new ArrayList<>(4);
        NodeUtil.forEachChild(this.getBodyNode(), new NodeVisitor() {

            private LamaWriteLocalVariableNode wn; // The current write node containing a slot

            @Override
            public boolean visit(Node node) {
                // When there is a write node, search for SLReadArgumentNode among its children:
                if (node instanceof InstrumentableNode.WrapperNode) {
                    return NodeUtil.forEachChild(node, this);
                }
                if (node instanceof LamaWriteLocalVariableNode) {
                    wn = (LamaWriteLocalVariableNode) node;
                    boolean all = NodeUtil.forEachChild(node, this);
                    wn = null;
                    return all;
                } else if (wn != null && (node instanceof LamaReadArgumentNode)) {
                    writeArgNodes.add(wn);
                    return true;
//                } else if (wn == null && (node instanceof LamaStatementNode && !(node instanceof SLBlockNode || node instanceof SLFunctionBodyNode))) {
                } else if (wn == null) {
                    // A different SL node - we're done.
                    return false;
                } else {
                    return NodeUtil.forEachChild(node, this);
                }
            }
        });
        return writeArgNodes.toArray(new LamaWriteLocalVariableNode[writeArgNodes.size()]);
    }

}
