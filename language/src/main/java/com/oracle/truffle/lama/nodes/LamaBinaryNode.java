package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild("leftNode")
@NodeChild("rightNode")
public abstract class LamaBinaryNode extends LamaExpressionNode {}
