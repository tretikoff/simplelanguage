package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode

@NodeInfo(shortName = "const")
class LamaArrayLiteralNode(private val values: Array<LamaExpressionNode>) : LamaExpressionNode() {
    override fun executeGeneric(frame: VirtualFrame?): Array<Any?> {
        return values.map { it.executeGeneric(frame) }.toTypedArray()
    }
}