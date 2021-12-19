
package com.oracle.truffle.lama.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;

public class SLParseInContextTest {
    private Context context;

    @Before
    public void setup() throws Exception {
        context = Context.newBuilder().allowPolyglotAccess(PolyglotAccess.ALL).build();
    }

    @After
    public void tearDown() throws Exception {
        context.close();
    }

    @Test
    public void parseAPlusB() throws Exception {
        Value value = context.eval("x-test-eval", "");
        assertTrue("Result is a number: " + value, value.isNumber());
        assertEquals(42, value.asInt());
    }

    @TruffleLanguage.Registration(id = "x-test-eval", name = "EvalLang", version = "1.0")
    public static final class EvalLang extends TruffleLanguage<Env> {

        @Override
        protected Env createContext(Env env) {
            return env;
        }

        @Override
        protected CallTarget parse(ParsingRequest request) throws Exception {
            return Truffle.getRuntime().createCallTarget(new RootNode(this) {

                @Override
                public Object execute(VirtualFrame frame) {
                    return parseAndEval();
                }

                @TruffleBoundary
                private Object parseAndEval() {
                    Source aPlusB = Source.newBuilder("sl", "a + b", "plus.sl").build();
                    return CONTEXT_REF.get(this).parsePublic(aPlusB, "a", "b").call(30, 12);
                }
            });
        }

        private static final ContextReference<Env> CONTEXT_REF = ContextReference.create(EvalLang.class);

    }
}
