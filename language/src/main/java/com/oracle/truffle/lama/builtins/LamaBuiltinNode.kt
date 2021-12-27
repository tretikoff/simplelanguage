package com.oracle.truffle.lama.builtins

import com.oracle.truffle.api.dsl.GenerateNodeFactory
import com.oracle.truffle.api.dsl.NodeChild
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.lama.LamaException
import com.oracle.truffle.lama.nodes.LamaExpressionNode

@NodeChild(value = "arguments", type = Array<LamaExpressionNode>::class)
@GenerateNodeFactory
abstract class LamaBuiltinNode : LamaExpressionNode() {
    override fun executeGeneric(frame: VirtualFrame?): Any {
        return try {
            execute(frame)
        } catch (e: UnsupportedSpecializationException) {
            throw LamaException.typeError(e.node, *e.suppliedValues)
        }
    }

    protected abstract fun execute(frame: VirtualFrame?): Any

    override fun executeLong(frame: VirtualFrame?): Long {
        return super.executeLong(frame)
    }

    override fun executeVoid(frame: VirtualFrame?) {
        super.executeVoid(frame)
    }
}