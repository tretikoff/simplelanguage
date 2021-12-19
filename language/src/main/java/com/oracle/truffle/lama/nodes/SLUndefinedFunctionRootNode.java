
package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.lama.LamaLanguage;
import com.oracle.truffle.lama.runtime.SLFunction;
import com.oracle.truffle.lama.runtime.SLUndefinedNameException;

/**
 * The initial {@link RootNode} of {@link SLFunction functions} when they are created, i.e., when
 * they are still undefined. Executing it throws an
 * {@link SLUndefinedNameException#undefinedFunction exception}.
 */
public class SLUndefinedFunctionRootNode extends SLRootNode {
    public SLUndefinedFunctionRootNode(LamaLanguage language, String name) {
        super(language, null, null, null, name);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw SLUndefinedNameException.undefinedFunction(null, getName());
    }
}
