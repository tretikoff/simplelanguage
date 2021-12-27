package com.oracle.truffle.lama.runtime;

import com.oracle.truffle.api.interop.TruffleObject;

public class LamaArray implements TruffleObject {
    private final Object[] data;

    public LamaArray(Object[] data) {
        this.data = data;
    }

    public Object getAt(int ind) {
        return data[ind];
    }

    public void setAt(int ind, Object val) {
        data[ind] = val;
    }

    public long getLength() {
        return data.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Arr: ");
        for (Object o : data) {
            sb.append(o.toString()).append(',');
        }
        return "Arr:" + sb;
    }
}
