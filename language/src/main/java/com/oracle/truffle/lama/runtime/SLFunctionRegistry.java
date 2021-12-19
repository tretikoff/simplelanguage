
package com.oracle.truffle.lama.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.lama.LamaLanguage;
import com.oracle.truffle.lama.parser.SimpleLanguageParser;

/**
 * Manages the mapping from function names to {@link SLFunction function objects}.
 */
public final class SLFunctionRegistry {

    private final LamaLanguage language;
    private final FunctionsObject functionsObject = new FunctionsObject();
    private final Map<Map<String, RootCallTarget>, Void> registeredFunctions = new IdentityHashMap<>();

    public SLFunctionRegistry(LamaLanguage language) {
        this.language = language;
    }

    /**
     * Returns the canonical {@link SLFunction} object for the given name. If it does not exist yet,
     * it is created.
     */
    @TruffleBoundary
    public SLFunction lookup(String name, boolean createIfNotPresent) {
        SLFunction result = functionsObject.functions.get(name);
        if (result == null && createIfNotPresent) {
            result = new SLFunction(language, name);
            functionsObject.functions.put(name, result);
        }
        return result;
    }

    /**
     * Associates the {@link SLFunction} with the given name with the given implementation root
     * node. If the function did not exist before, it defines the function. If the function existed
     * before, it redefines the function and the old implementation is discarded.
     */
    SLFunction register(String name, RootCallTarget callTarget) {
        SLFunction result = functionsObject.functions.get(name);
        if (result == null) {
            result = new SLFunction(callTarget);
            functionsObject.functions.put(name, result);
        } else {
            result.setCallTarget(callTarget);
        }
        return result;
    }

    /**
     * Registers a map of functions. The once registered map must not change in order to allow to
     * cache the registration for the entire map. If the map is changed after registration the
     * functions might not get registered.
     */
    @TruffleBoundary
    public void register(Map<String, RootCallTarget> newFunctions) {
        if (registeredFunctions.containsKey(newFunctions)) {
            return;
        }
        for (Map.Entry<String, RootCallTarget> entry : newFunctions.entrySet()) {
            register(entry.getKey(), entry.getValue());
        }
        registeredFunctions.put(newFunctions, null);
    }

    public void register(Source newFunctions) {
        register(SimpleLanguageParser.parseSL(language, newFunctions));
    }

    public SLFunction getFunction(String name) {
        return functionsObject.functions.get(name);
    }

    /**
     * Returns the sorted list of all functions, for printing purposes only.
     */
    public List<SLFunction> getFunctions() {
        List<SLFunction> result = new ArrayList<>(functionsObject.functions.values());
        Collections.sort(result, new Comparator<SLFunction>() {
            public int compare(SLFunction f1, SLFunction f2) {
                return f1.toString().compareTo(f2.toString());
            }
        });
        return result;
    }

    public TruffleObject getFunctionsObject() {
        return functionsObject;
    }

}
