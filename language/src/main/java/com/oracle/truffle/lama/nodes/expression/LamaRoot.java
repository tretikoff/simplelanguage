package com.oracle.truffle.lama.nodes.expression;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.lama.LamaLanguage;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;

public class LamaRoot extends RootNode {
    @Child
    private LamaExpressionNode body;

    public LamaRoot(LamaExpressionNode body, FrameDescriptor frameDescriptor) {
        super(LamaLanguage.Companion.get(body), frameDescriptor);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }
}
