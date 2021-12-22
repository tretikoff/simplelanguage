package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

@TypeSystemReference(LamaTypes.class)
@NodeInfo(description = "The abstract base node for all expressions")
@GenerateWrapper
public abstract class LamaExpressionNode extends LamaStatementNode {

    private boolean hasExpressionTag;

    public abstract Object executeGeneric(VirtualFrame frame);

    @Override
    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    @Override
    public InstrumentableNode.WrapperNode createWrapper(ProbeNode probe) {
        return new LamaExpressionNodeWrapper(this, probe);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.ExpressionTag.class) {
            return hasExpressionTag;
        }
        return super.hasTag(tag);
    }

    public final void addExpressionTag() {
        hasExpressionTag = true;
    }

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return LamaTypesGen.expectLong(executeGeneric(frame));
    }
}
