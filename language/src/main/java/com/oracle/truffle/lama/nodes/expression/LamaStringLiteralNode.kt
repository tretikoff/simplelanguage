package com.oracle.truffle.lama.nodes.expression

import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.nodes.NodeInfo

@NodeInfo(shortName = "const")
class LamaStringLiteralNode(private val value: String) : LamaExpressionNode() {
    override fun executeGeneric(frame: VirtualFrame): String {
        return value
    }
}