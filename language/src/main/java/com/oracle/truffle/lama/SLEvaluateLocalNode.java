
package com.oracle.truffle.lama;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

final class SLEvaluateLocalNode extends RootNode {

    private final String variable;
    private final MaterializedFrame inspectFrame;

    SLEvaluateLocalNode(LamaLanguage language, String variableName, MaterializedFrame frame) {
        super(language);
        this.variable = variableName;
        this.inspectFrame = frame;
    }

    @Override
    public Object execute(VirtualFrame currentFrame) {
        for (FrameSlot slot : inspectFrame.getFrameDescriptor().getSlots()) {
            if (variable.equals(slot.getIdentifier())) {
                return inspectFrame.getValue(slot);
            }
        }
        return null;
    }
}
