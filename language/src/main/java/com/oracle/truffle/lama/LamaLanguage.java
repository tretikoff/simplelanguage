package com.oracle.truffle.lama;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.lama.builtins.LamaBuiltinNode;
import com.oracle.truffle.lama.parser.LamaLanguageParser;
import com.oracle.truffle.lama.runtime.LamaContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@TruffleLanguage.Registration(id = LamaLanguage.ID, name = "Lama", defaultMimeType = LamaLanguage.MIME_TYPE, characterMimeTypes = LamaLanguage.MIME_TYPE, contextPolicy = ContextPolicy.SHARED, fileTypeDetectors = LamaFileDetector.class)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.StatementTag.class, StandardTags.RootTag.class, StandardTags.RootBodyTag.class, StandardTags.ExpressionTag.class, DebuggerTags.AlwaysHalt.class, StandardTags.ReadVariableTag.class, StandardTags.WriteVariableTag.class})
public final class LamaLanguage extends TruffleLanguage<LamaContext> {
    public static volatile int counter;

    public static final String ID = "lama";
    public static final String MIME_TYPE = "application/x-lama";
    private static final Source BUILTIN_SOURCE = Source.newBuilder(LamaLanguage.ID, "", "Lama builtin").build();

    private final Assumption singleContext = Truffle.getRuntime().createAssumption("Single Lama context.");

    private final Map<NodeFactory<? extends LamaBuiltinNode>, RootCallTarget> builtinTargets = new ConcurrentHashMap<>();
    private final Map<String, RootCallTarget> undefinedFunctions = new ConcurrentHashMap<>();

    public LamaLanguage() {
        counter++;
    }

    @Override
    protected LamaContext createContext(Env env) {
        return new LamaContext(this, env);
    }

    @Override
    protected boolean patchContext(LamaContext context, Env newEnv) {
        context.patchContext(newEnv);
        return true;
    }


    public static NodeInfo lookupNodeInfo(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        NodeInfo info = clazz.getAnnotation(NodeInfo.class);
        if (info != null) {
            return info;
        } else {
            return lookupNodeInfo(clazz.getSuperclass());
        }
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        RootNode evalMain = LamaLanguageParser.parseLama(this, source);
        return Truffle.getRuntime().createCallTarget(evalMain);
    }

    @Override
    protected void initializeMultipleContexts() {
        singleContext.invalidate();
    }


    @Override
    protected boolean isVisible(LamaContext context, Object value) {
        return !InteropLibrary.getFactory().getUncached(value).isNull(value);
    }


    private static final LanguageReference<LamaLanguage> REFERENCE = LanguageReference.create(LamaLanguage.class);

    public static LamaLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    private static final List<NodeFactory<? extends LamaBuiltinNode>> EXTERNAL_BUILTINS = Collections.synchronizedList(new ArrayList<>());

    public static void installBuiltin(NodeFactory<? extends LamaBuiltinNode> builtin) {
        EXTERNAL_BUILTINS.add(builtin);
    }

}
