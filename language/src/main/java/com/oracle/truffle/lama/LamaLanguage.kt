package com.oracle.truffle.lama

import com.oracle.truffle.api.CallTarget
import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.TruffleLanguage
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy
import com.oracle.truffle.api.TruffleLanguage.Registration
import com.oracle.truffle.api.debug.DebuggerTags
import com.oracle.truffle.api.instrumentation.ProvidedTags
import com.oracle.truffle.api.instrumentation.StandardTags.*
import com.oracle.truffle.api.interop.InteropLibrary
import com.oracle.truffle.api.nodes.Node
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.api.nodes.RootNode
import com.oracle.truffle.lama.runtime.LamaContext

@Registration(
    id = LamaLanguage.ID,
    name = "Lama",
    defaultMimeType = LamaLanguage.MIME_TYPE,
    characterMimeTypes = [LamaLanguage.MIME_TYPE],
    contextPolicy = ContextPolicy.SHARED,
    fileTypeDetectors = [SLFileDetector::class]
)
@ProvidedTags(
    CallTag::class,
    StatementTag::class,
    RootTag::class,
    RootBodyTag::class,
    ExpressionTag::class,
    DebuggerTags.AlwaysHalt::class,
    ReadVariableTag::class,
    WriteVariableTag::class
)
class LamaLanguage : TruffleLanguage<LamaContext>() {
    private val singleContext = Truffle.getRuntime().createAssumption("Single lama context.")

    override fun createContext(env: Env): LamaContext {
        return LamaContext(this, env)
    }

    override fun patchContext(context: LamaContext, newEnv: Env): Boolean {
        context.patchContext(newEnv)
        return true
    }

    override fun parse(request: ParsingRequest): CallTarget {
//        val evalMain: RootNode = SimpleLanguageParser.parseSL(this, request.source) as RootNode
        val evalMain: RootNode? = null
        return Truffle.getRuntime().createCallTarget(evalMain)
    }

    override fun initializeMultipleContexts() {
        singleContext.invalidate()
    }

    fun isSingleContext(): Boolean {
        return singleContext.isValid
    }

    override fun isVisible(context: LamaContext, value: Any): Boolean {
        return !InteropLibrary.getFactory().getUncached(value).isNull(value)
    }

    companion object {
        const val ID = "lama"
        const val MIME_TYPE = "application/x-lama"

        @Volatile
        var counter = 0
        fun lookupNodeInfo(clazz: Class<*>?): NodeInfo? {
            if (clazz == null) {
                return null
            }
            val info = clazz.getAnnotation(NodeInfo::class.java)
            return info ?: lookupNodeInfo(clazz.superclass)
        }

        private val REFERENCE: LanguageReference<LamaLanguage> = LanguageReference.create(LamaLanguage::class.java)
        operator fun get(node: Node?): LamaLanguage {
            return REFERENCE[node]
        }
    }

    init {
        counter++
    }
}