package com.oracle.truffle.lama.nodes

import com.oracle.truffle.api.dsl.TypeSystemReference
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.instrumentation.*
import com.oracle.truffle.api.nodes.NodeInfo

@TypeSystemReference(LamaTypes::class)
@NodeInfo(description = "The abstract base node for all expressions")
@GenerateWrapper
abstract class LamaExpressionNode : LamaStatementNode() {
    private var hasExpressionTag = false

    abstract fun executeGeneric(frame: VirtualFrame?): Any?

    override fun executeVoid(frame: VirtualFrame?) {
        executeGeneric(frame)
    }

    override fun createWrapper(probe: ProbeNode): InstrumentableNode.WrapperNode {
        return LamaExpressionNodeWrapper(this, probe)
    }

    override fun hasTag(tag: Class<out Tag?>): Boolean {
        return if (tag == StandardTags.ExpressionTag::class.java) {
            hasExpressionTag
        } else super.hasTag(tag)
    }

    fun addExpressionTag() {
        hasExpressionTag = true
    }

    open fun executeLong(frame: VirtualFrame?): Long {
        return LamaTypesGen.expectLong(executeGeneric(frame))
    }
}