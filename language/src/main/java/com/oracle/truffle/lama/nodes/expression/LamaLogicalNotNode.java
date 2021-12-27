
package com.oracle.truffle.lama.nodes.expression;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.lama.LamaException;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;

@NodeChild("valueNode")
@NodeInfo(shortName = "!")
public abstract class LamaLogicalNotNode extends LamaExpressionNode {

    @Specialization
    protected boolean doBoolean(boolean value) {
        return !value;
    }

    @Fallback
    protected Object typeError(Object value) {
        throw LamaException.typeError(this, value);
    }
}
