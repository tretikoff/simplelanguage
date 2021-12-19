package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.api.interop.InteropLibrary
import com.oracle.truffle.api.nodes.ExplodeLoop
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.CompilerAsserts
import com.oracle.truffle.api.interop.ArityException
import com.oracle.truffle.lama.runtime.SLUndefinedNameException
import com.oracle.truffle.api.interop.UnsupportedTypeException
import com.oracle.truffle.api.interop.UnsupportedMessageException
import com.oracle.truffle.api.instrumentation.StandardTags.CallTag
import com.oracle.truffle.api.instrumentation.Tag
import com.oracle.truffle.api.nodes.NodeInfo

@NodeInfo(shortName = "invoke")
class LamaInvokeNode(
    @field:Child private val functionNode: LamaExpressionNode,
    @field:Children private val argumentNodes: Array<LamaExpressionNode>
) : LamaExpressionNode() {
    @Child
    private val library: InteropLibrary = InteropLibrary.getFactory().createDispatched(3)

    @ExplodeLoop
    override fun executeGeneric(frame: VirtualFrame): Any {
        val function = functionNode.executeGeneric(frame)

        CompilerAsserts.compilationConstant<Any>(argumentNodes.size)
        val argumentValues = arrayOfNulls<Any>(argumentNodes.size)
        for (i in argumentNodes.indices) {
            argumentValues[i] = argumentNodes[i].executeGeneric(frame)
        }
        return try {
            library.execute(function, *argumentValues)
        } catch (e: ArityException) {
            throw SLUndefinedNameException.undefinedFunction(this, function)
        } catch (e: UnsupportedTypeException) {
            throw SLUndefinedNameException.undefinedFunction(this, function)
        } catch (e: UnsupportedMessageException) {
            throw SLUndefinedNameException.undefinedFunction(this, function)
        }
    }

    override fun hasTag(tag: Class<out Tag?>): Boolean {
        return if (tag == CallTag::class.java) {
            true
        } else super.hasTag(tag)
    }

}