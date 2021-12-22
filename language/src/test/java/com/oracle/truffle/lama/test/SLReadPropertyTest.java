
package com.oracle.truffle.lama.test;

import static org.junit.Assert.assertNull;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SLReadPropertyTest {

    private Context ctx;
    private Value slObject;

    @Before
    public void setUp() {
        this.ctx = Context.create("sl");
        this.slObject = ctx.eval("sl", "function createObject() {\n" +
                        "obj1 = new();\n" +
                        "obj1.number = 42;\n" +
                        "return obj1;\n" +
                        "}\n" +
                        "function main() {\n" +
                        "return createObject;\n" +
                        "}").execute();
    }

    @After
    public void tearDown() {
        this.ctx.close();
    }

    @Test
    public void testRead() {
        Assert.assertEquals(42, slObject.getMember("number").asInt());
        assertNull(slObject.getMember("nonexistent"));
    }
}
