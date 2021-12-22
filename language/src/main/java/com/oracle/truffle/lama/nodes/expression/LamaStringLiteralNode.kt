package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.lama.nodes.LamaExpressionNode

@NodeInfo(shortName = "const")
class LamaStringLiteralNode(private val value: String) : LamaExpressionNode() {
    override fun executeGeneric(frame: VirtualFrame?): Any {
        return value
    }
}