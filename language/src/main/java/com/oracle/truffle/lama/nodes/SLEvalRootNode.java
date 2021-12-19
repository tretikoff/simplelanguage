
package com.oracle.truffle.lama.nodes;

import java.util.Collections;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.lama.LamaLanguage;
import com.oracle.truffle.lama.runtime.LamaContext;
import com.oracle.truffle.lama.runtime.LamaNull;

/**
 * This class performs two additional tasks:
 *
 * <ul>
 * <li>Lazily registration of functions on first execution. This fulfills the semantics of
 * "evaluating" source code in SL.</li>
 * <li>Conversion of arguments to types understood by SL. The SL source code can be evaluated from a
 * different language, i.e., the caller can be a node from a different language that uses types not
 * understood by SL.</li>
 * </ul>
 */
public final class SLEvalRootNode extends RootNode {

    private final Map<String, RootCallTarget> functions;
    @CompilationFinal private boolean registered;

    @Child private DirectCallNode mainCallNode;
    private final LamaLanguage language;

    public SLEvalRootNode(LamaLanguage language, RootCallTarget rootFunction, Map<String, RootCallTarget> functions) {
        super(language);
        this.language = language;
        this.functions = Collections.unmodifiableMap(functions);
        this.mainCallNode = rootFunction != null ? DirectCallNode.create(rootFunction) : null;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    protected boolean isInstrumentable() {
        return false;
    }

    @Override
    public String getName() {
        return "root eval";
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (language.isSingleContext()) {
            /*
             * Lazy registrations of functions on first execution. This optimization only works in
             * the single context case. Otherwise function registration needs to be repeated for
             * every context on first execute.
             */
            if (!registered) {
                /* Function registration is a slow-path operation that must not be compiled. */
                CompilerDirectives.transferToInterpreterAndInvalidate();
                registerFunctions();
                registered = true;
            }
        } else {
            /*
             * In the multi context case we always want to ensure that functions are registered. The
             * multi-context case is initialized with SLLanguage#initializeMultipleContexts. That
             * typically happens when a polyglot Context was created with an explicit Engine or if
             * an internal context was created. See Context.Builder#engine for details.
             */
            registerFunctions();
        }
        if (mainCallNode == null) {
            /* The source code did not have a "main" function, so nothing to execute. */
            return LamaNull.SINGLETON;
        } else {
            /* Conversion of arguments to types understood by SL. */
            Object[] arguments = frame.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = LamaContext.fromForeignValue(arguments[i]);
            }
            return mainCallNode.call(arguments);
        }
    }

    @TruffleBoundary
    private void registerFunctions() {
        LamaContext.Companion.get(this).getFunctionRegistry().register(functions);
    }

}
