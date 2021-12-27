
package com.oracle.truffle.lama.nodes.expression;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.lama.LamaException;
import com.oracle.truffle.lama.nodes.LamaBinaryNode;

@NodeInfo(shortName = "-")
public abstract class LamaSubNode extends LamaBinaryNode {

    @Specialization(rewriteOn = ArithmeticException.class)
    protected long sub(long left, long right) {
        return Math.subtractExact(left, right);
    }

    @Fallback
    protected Object typeError(Object left, Object right) {
        throw LamaException.typeError(this, left, right);
    }

}
