
package com.oracle.truffle.lama.nodes.expression;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;

@NodeInfo(shortName = "read")
@NodeField(name = "slot", type = FrameSlot.class)
public abstract class LamaReadNode extends LamaExpressionNode {

    @Specialization
    protected Object read(VirtualFrame frame) {
        return null;
    }
}
