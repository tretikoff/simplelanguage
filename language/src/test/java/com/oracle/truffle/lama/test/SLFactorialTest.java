
package com.oracle.truffle.lama.test;

import static org.junit.Assert.assertEquals;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SLFactorialTest {

    private Context context;
    private Value factorial;

    @Before
    public void initEngine() throws Exception {
        context = Context.create();
        // @formatter:off
        context.eval("sl", "\n" +
                "function fac(n) {\n" +
                "  if (n <= 1) {\n" +
                "    return 1;\n" +
                "  }\n" +
                "  prev = fac(n - 1);\n" +
                "  return prev * n;\n" +
                "}\n"
        );
        // @formatter:on
        factorial = context.getBindings("sl").getMember("fac");
    }

    @After
    public void dispose() {
        context.close();
    }

    @Test
    public void factorialOf5() throws Exception {
        Number ret = factorial.execute(5).as(Number.class);
        assertEquals(120, ret.intValue());
    }

    @Test
    public void factorialOf3() throws Exception {
        Number ret = factorial.execute(3).as(Number.class);
        assertEquals(6, ret.intValue());
    }

    @Test
    public void factorialOf1() throws Exception {
        Number ret = factorial.execute(1).as(Number.class);
        assertEquals(1, ret.intValue());
    }
}
