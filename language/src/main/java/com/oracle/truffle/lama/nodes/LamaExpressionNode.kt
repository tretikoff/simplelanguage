package com.oracle.truffle.lama.nodes

import com.oracle.truffle.api.dsl.TypeSystemReference
import com.oracle.truffle.lama.nodes.SLTypes
import com.oracle.truffle.api.instrumentation.GenerateWrapper
import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.instrumentation.ProbeNode
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode
import com.oracle.truffle.lama.nodes.LamaExpressionNodeWrapper
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag
import com.oracle.truffle.api.instrumentation.Tag
import com.oracle.truffle.api.nodes.NodeInfo
import kotlin.Throws
import com.oracle.truffle.api.nodes.UnexpectedResultException
import com.oracle.truffle.lama.nodes.SLTypesGen

/**
 * Base class for all SL nodes that produce a value and therefore benefit from type specialization.
 * The annotation [TypeSystemReference] specifies the SL types. Specifying it here defines the
 * type system for all subclasses.
 */
@TypeSystemReference(SLTypes::class)
@NodeInfo(description = "The abstract base node for all expressions")
@GenerateWrapper
abstract class LamaExpressionNode : LamaStatementNode() {
    private var hasExpressionTag = false

    /**
     * The execute method when no specialization is possible. This is the most general case,
     * therefore it must be provided by all subclasses.
     */
    abstract fun executeGeneric(frame: VirtualFrame?): Any?

    /**
     * When we use an expression at places where a [statement][LamaStatementNode] is already
     * sufficient, the return value is just discarded.
     */
    override fun executeVoid(frame: VirtualFrame) {
        executeGeneric(frame)
    }

    override fun createWrapper(probe: ProbeNode): WrapperNode {
        return LamaExpressionNodeWrapper(this, probe)
    }

    override fun hasTag(tag: Class<out Tag?>): Boolean {
        return if (tag == ExpressionTag::class.java) {
            hasExpressionTag
        } else super.hasTag(tag)
    }

    /**
     * Marks this node as being a [StandardTags.ExpressionTag] for instrumentation purposes.
     */
    fun addExpressionTag() {
        hasExpressionTag = true
    }

    /*
     * Execute methods for specialized types. They all follow the same pattern: they call the
     * generic execution method and then expect a result of their return type. Type-specialized
     * subclasses overwrite the appropriate methods.
     */
    @Throws(UnexpectedResultException::class)
    open fun executeLong(frame: VirtualFrame?): Long {
        return SLTypesGen.expectLong(executeGeneric(frame))
    }

    @Throws(UnexpectedResultException::class)
    open fun executeBoolean(frame: VirtualFrame?): Boolean {
        return SLTypesGen.expectBoolean(executeGeneric(frame))
    }
}