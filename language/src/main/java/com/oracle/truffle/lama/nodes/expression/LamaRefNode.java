package com.oracle.truffle.lama.nodes.expression;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;

@NodeField(name = "slot", type = FrameSlot.class)
public abstract class LamaRefNode extends LamaExpressionNode {

    protected abstract FrameSlot getSlot();

    @Specialization
    public Object readObject(VirtualFrame frame) {
        return getSlot();
    }
}
