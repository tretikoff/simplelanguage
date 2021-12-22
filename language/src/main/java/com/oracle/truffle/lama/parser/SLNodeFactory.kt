package com.oracle.truffle.lama.parser

import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.frame.FrameSlot
import com.oracle.truffle.api.frame.FrameSlotKind
import com.oracle.truffle.lama.nodes.LamaExpressionNode
import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.lama.nodes.controlflow.LamaIfNode
import com.oracle.truffle.lama.nodes.controlflow.LamaWhileNode
import com.oracle.truffle.lama.nodes.expression.*
import com.oracle.truffle.lama.nodes.local.LamaReadArgumentNode
import com.oracle.truffle.lama.nodes.local.LamaReadLocalVariableNode
import com.oracle.truffle.lama.nodes.controlflow.SLBlockNode
import org.antlr.v4.runtime.Token

/**
 * Helper class used by the SL [Parser] to create nodes. The code is factored out of the
 * automatically generated parser to keep the attributed grammar of SL small.
 */
class SLNodeFactory {
    /**
     * Local variable names that are visible in the current block. Variables are not visible outside
     * of their defining block, to prevent the usage of undefined variables. Because of that, we can
     * decide during parsing if a name references a local variable or is a function name.
     */
    internal class LexicalScope(val outer: LexicalScope?) {
        val locals: MutableMap<String, FrameSlot>

        init {
            locals = HashMap()
            if (outer != null) {
                locals.putAll(outer.locals)
            }
        }
    }

    /* State while parsing a function. */
    private var functionStartPos = 0
    private var functionName: String? = null
    private var functionBodyStartPos // includes parameter list
            = 0
    private var parameterCount = 0
    private var frameDescriptor: FrameDescriptor? = null
    private var methodNodes: MutableList<LamaStatementNode?>? = null

    /* State while parsing a block. */
    private var lexicalScope: LexicalScope? = null
    fun startFunction(nameToken: Token, bodyStartToken: Token) {
        assert(functionStartPos == 0)
        assert(functionName == null)
        assert(functionBodyStartPos == 0)
        assert(parameterCount == 0)
        assert(frameDescriptor == null)
        assert(lexicalScope == null)
        functionStartPos = nameToken.startIndex
        functionName = nameToken.text
        functionBodyStartPos = bodyStartToken.startIndex
        frameDescriptor = FrameDescriptor()
        methodNodes = ArrayList()
        startBlock()
    }

    fun addFormalParameter(nameToken: Token) {
        /*
         * Method parameters are assigned to local variables at the beginning of the method. This
         * ensures that accesses to parameters are specialized the same way as local variables are
         * specialized.
         */
        val readArg = LamaReadArgumentNode(parameterCount)
        readArg.setSourceSection(nameToken.startIndex, nameToken.text.length)
        val assignment = createAssignment(createStringLiteral(nameToken, false), readArg, parameterCount)
        methodNodes!!.add(assignment)
        parameterCount++
    }

    fun startBlock() {
        lexicalScope = LexicalScope(lexicalScope)
    }

    fun finishBlock(bodyNodes: List<LamaStatementNode>, startPos: Int, length: Int): LamaStatementNode? {
        return finishBlock(bodyNodes, 0, startPos, length)
    }

    fun finishBlock(
        bodyNodes: List<LamaStatementNode>,
        skipCount: Int,
        startPos: Int,
        length: Int
    ): LamaStatementNode? {
        lexicalScope = lexicalScope!!.outer
        if (containsNull(bodyNodes)) {
            return null
        }
        val flattenedNodes: MutableList<LamaStatementNode> = ArrayList(bodyNodes.size)
        flattenBlocks(bodyNodes, flattenedNodes)
        val n = flattenedNodes.size
        for (i in skipCount until n) {
            val statement = flattenedNodes[i]
            if (statement.hasSource() && !isHaltInCondition(statement)) {
                statement.addStatementTag()
            }
        }
        val blockNode = SLBlockNode(flattenedNodes.toTypedArray())
        blockNode.setSourceSection(startPos, length)
        return blockNode
    }

    private fun flattenBlocks(bodyNodes: Iterable<LamaStatementNode>, flattenedNodes: MutableList<LamaStatementNode>) {
        for (n in bodyNodes) {
            if (n is SLBlockNode) {
                flattenBlocks(n.statements, flattenedNodes)
            } else {
                flattenedNodes.add(n)
            }
        }
    }

    /**
     * Returns an [LamaWhileNode] for the given parameters.
     *
     * @param whileToken    The token containing the while node's info
     * @param conditionNode The conditional node for this while loop
     * @param bodyNode      The body of the while loop
     * @return A SLWhileNode built using the given parameters. null if either conditionNode or
     * bodyNode is null.
     */
    fun createWhile(
        whileToken: Token,
        conditionNode: LamaExpressionNode,
        bodyNode: LamaExpressionNode
    ): LamaStatementNode? {
        if (conditionNode == null || bodyNode == null) {
            return null
        }
        conditionNode.addStatementTag()
        val start = whileToken.startIndex
        val end = bodyNode.sourceEndIndex
        val whileNode = LamaWhileNode(conditionNode, bodyNode)
        whileNode.setSourceSection(start, end - start)
        return whileNode
    }

    fun createIf(
        ifToken: Token,
        conditionNode: LamaExpressionNode?,
        thenPartNode: LamaExpressionNode?,
        elsePartNode: LamaExpressionNode?
    ): LamaStatementNode? {
        if (conditionNode == null || thenPartNode == null) {
            return null
        }
        conditionNode.addStatementTag()
        val start = ifToken.startIndex
        val end = elsePartNode?.sourceEndIndex ?: thenPartNode.sourceEndIndex
        val ifNode = LamaIfNode(conditionNode, thenPartNode, elsePartNode)
        ifNode.setSourceSection(start, end - start)
        return ifNode
    }

    fun createBinary(
        opToken: Token,
        leftNode: LamaExpressionNode?,
        rightNode: LamaExpressionNode?
    ): LamaExpressionNode? {
        if (leftNode == null || rightNode == null) {
            return null
        }
        val result: LamaExpressionNode = when (opToken.text) {
            "+" -> LamaAddNodeGen.create(leftNode, rightNode)
            "*" -> LamaMulNodeGen.create(leftNode, rightNode)
            "/" -> LamaDivNodeGen.create(leftNode, rightNode)
            "-" -> LamaSubNodeGen.create(leftNode, rightNode)
            "<" -> LamaLessThanNodeGen.create(leftNode, rightNode)
            "<=" -> LamaLessOrEqualNodeGen.create(leftNode, rightNode)
            ">" -> LamaLogicalNotNodeGen.create(LamaLessOrEqualNodeGen.create(leftNode, rightNode))
            ">=" -> LamaLogicalNotNodeGen.create(LamaLessThanNodeGen.create(leftNode, rightNode))
            "==" -> LamaEqualNodeGen.create(leftNode, rightNode)
            "!=" -> LamaLogicalNotNodeGen.create(LamaEqualNodeGen.create(leftNode, rightNode))
            "&&" -> LamaLogicalAndNode(leftNode, rightNode)
            "!!" -> LamaLogicalOrNode(leftNode, rightNode)
            else -> throw RuntimeException("unexpected operation: " + opToken.text)
        }
        val start = leftNode.sourceCharIndex
        val length = rightNode.sourceEndIndex - start
        result.setSourceSection(start, length)
        result.addExpressionTag()
        return result
    }

    /**
     * Returns an [LamaInvokeNode] for the given parameters.
     *
     * @param functionNode   The function being called
     * @param parameterNodes The parameters of the function call
     * @param finalToken     A token used to determine the end of the sourceSelection for this call
     * @return An SLInvokeNode for the given parameters. null if functionNode or any of the
     * parameterNodes are null.
     */
    fun createCall(
        functionNode: LamaExpressionNode?,
        parameterNodes: List<LamaExpressionNode>,
        finalToken: Token
    ): LamaExpressionNode? {
        if (functionNode == null || containsNull(parameterNodes)) {
            return null
        }
        val result: LamaExpressionNode = LamaInvokeNode(functionNode, parameterNodes.toTypedArray())
        val startPos = functionNode.sourceCharIndex
        val endPos = finalToken.startIndex + finalToken.text.length
        result.setSourceSection(startPos, endPos - startPos)
        result.addExpressionTag()
        return result
    }

    @JvmOverloads
    fun createAssignment(
        nameNode: LamaExpressionNode?,
        valueNode: LamaExpressionNode?,
        argumentIndex: Int? = null
    ): LamaExpressionNode? {
        if (nameNode == null || valueNode == null) {
            return null
        }
        val name: String = (nameNode as LamaStringLiteralNode).executeGeneric(null) as String
        val frameSlot = frameDescriptor!!.findOrAddFrameSlot(
            name,
            argumentIndex,
            FrameSlotKind.Illegal
        )
        val existingSlot = lexicalScope!!.locals.put(name, frameSlot)
        val newVariable = existingSlot == null
        val result: LamaExpressionNode =
            LamaWriteNodeGen.create(valueNode, frameSlot, nameNode, newVariable)
        if (valueNode.hasSource()) {
            val start = nameNode.sourceCharIndex
            val length = valueNode.sourceEndIndex - start
            result.setSourceSection(start, length)
        }
        if (argumentIndex == null) {
            result.addExpressionTag()
        }
        return result
    }

    fun createRead(nameNode: LamaExpressionNode?): LamaExpressionNode? {
        if (nameNode == null) {
            return null
        }
        val name: String = (nameNode as LamaStringLiteralNode).executeGeneric(null) as String
        val result: LamaExpressionNode
        val frameSlot = lexicalScope!!.locals[name]
        result = if (frameSlot != null) {
            LamaReadNodeGen.create(frameSlot)
        } else {
            LamaFunctionLiteralNode(name)
        }
        result.setSourceSection(nameNode.sourceCharIndex, nameNode.sourceLength)
        result.addExpressionTag()
        return result
    }

    fun createStringLiteral(literalToken: Token, removeQuotes: Boolean): LamaExpressionNode {
        var literal = literalToken.text
        if (removeQuotes) {
            assert(literal.length >= 2 && literal.startsWith("\"") && literal.endsWith("\""))
            literal = literal.substring(1, literal.length - 1)
        }
        val result = LamaStringLiteralNode(literal.intern())
        srcFromToken(result, literalToken)
        result.addExpressionTag()
        return result
    }

    fun createNumericLiteral(literalToken: Token): LamaExpressionNode {
        val result: LamaExpressionNode
        result = LamaLongLiteralNode(literalToken.text.toLong())
        srcFromToken(result, literalToken)
        result.addExpressionTag()
        return result
    }

    companion object {
        private fun isHaltInCondition(statement: LamaStatementNode): Boolean {
            return statement is LamaIfNode || statement is LamaWhileNode
        }

        private fun srcFromToken(node: LamaStatementNode, token: Token) {
            node.setSourceSection(token.startIndex, token.text.length)
        }

        private fun containsNull(list: List<*>): Boolean {
            for (e in list) {
                if (e == null) {
                    return true
                }
            }
            return false
        }
    }
}