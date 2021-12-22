
package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.lama.runtime.LamaNull;

@TypeSystem({long.class, LamaNull.class})
public abstract class LamaTypes {

    @TypeCheck(LamaNull.class)
    public static boolean isSLNull(Object value) {
        return value == LamaNull.SINGLETON;
    }

    @TypeCast(LamaNull.class)
    public static LamaNull asSLNull(Object value) {
        assert isSLNull(value);
        return LamaNull.SINGLETON;
    }
}
