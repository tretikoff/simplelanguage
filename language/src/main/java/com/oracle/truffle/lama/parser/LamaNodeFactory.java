package com.oracle.truffle.lama.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import com.oracle.truffle.lama.nodes.LamaStatementNode;
import com.oracle.truffle.lama.nodes.controlflow.LamaIfNode;
import com.oracle.truffle.lama.nodes.controlflow.LamaWhileNode;
import com.oracle.truffle.lama.nodes.expression.*;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class used by the Lama {@link Parser} to create nodes. The code is factored out of the
 * automatically generated parser to keep the attributed grammar of Lama small.
 */
public class LamaNodeFactory {

    /**
     * Local variable names that are visible in the current block. Variables are not visible outside
     * of their defining block, to prevent the usage of undefined variables. Because of that, we can
     * decide during parsing if a name references a local variable or is a function name.
     */
    static class LexicalScope {
        protected final LexicalScope outer;
        protected final Map<String, FrameSlot> locals;

        LexicalScope(LexicalScope outer) {
            this.outer = outer;
            this.locals = new HashMap<>();
            if (outer != null) {
                locals.putAll(outer.locals);
            }
        }

        public void clear() {
            locals.clear();
        }
    }


    /* State while parsing a function. */
    private int functionStartPos;
    private String functionName;
    private int functionBodyStartPos; // includes parameter list
    private int parameterCount;
    private FrameDescriptor frameDescriptor;

    private LexicalScope lexicalScope;

    public LamaStatementNode createWhile(Token whileToken, LamaExpressionNode conditionNode, LamaExpressionNode bodyNode) {
        if (conditionNode == null || bodyNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final int start = whileToken.getStartIndex();
        final int end = bodyNode.getSourceEndIndex();
        final LamaWhileNode whileNode = new LamaWhileNode(conditionNode, bodyNode);
        whileNode.setSourceSection(start, end - start);
        return whileNode;
    }

    public LamaStatementNode createIf(Token ifToken, LamaExpressionNode conditionNode, LamaExpressionNode thenPartNode, LamaExpressionNode elsePartNode) {
        if (conditionNode == null || thenPartNode == null) {
            return null;
        }

        conditionNode.addStatementTag();
        final int start = ifToken.getStartIndex();
        final int end = elsePartNode == null ? thenPartNode.getSourceEndIndex() : elsePartNode.getSourceEndIndex();
        final LamaIfNode ifNode = new LamaIfNode(conditionNode, thenPartNode, elsePartNode);
        ifNode.setSourceSection(start, end - start);
        return ifNode;
    }


    public void enterScope() {
        lexicalScope = new LexicalScope(lexicalScope);
    }

    public void endScope() {
        lexicalScope.clear();
        lexicalScope = lexicalScope.outer;
    }

    /**
     * Returns the corresponding subclass of {@link LamaExpressionNode} for binary expressions. </br>
     * These nodes are currently not instrumented.
     *
     * @param opToken   The operator of the binary expression
     * @param leftNode  The left node of the expression
     * @param rightNode The right node of the expression
     * @return A subclass of LamaExpressionNode using the given parameters based on the given opToken.
     * null if either leftNode or rightNode is null.
     */
    public LamaExpressionNode createBinary(Token opToken, LamaExpressionNode leftNode, LamaExpressionNode rightNode) {
        if (leftNode == null || rightNode == null) {
            return null;
        }
        final LamaExpressionNode result;
        switch (opToken.getText()) {
            case "+":
                result = LamaAddNodeGen.create(leftNode, rightNode);
                break;
            case "*":
                result = LamaMulNodeGen.create(leftNode, rightNode);
                break;
            case "/":
                result = LamaDivNodeGen.create(leftNode, rightNode);
                break;
            case "-":
                result = LamaSubNodeGen.create(leftNode, rightNode);
                break;
            case "<":
                result = LamaLessThanNodeGen.create(leftNode, rightNode);
                break;
            case "<=":
                result = LamaLessOrEqualNodeGen.create(leftNode, rightNode);
                break;
            case ">":
                result = LamaLogicalNotNodeGen.create(LamaLessOrEqualNodeGen.create(leftNode, rightNode));
                break;
            case ">=":
                result = LamaLogicalNotNodeGen.create(LamaLessThanNodeGen.create(leftNode, rightNode));
                break;
            case "==":
                result = LamaEqualNodeGen.create(leftNode, rightNode);
                break;
            case "!=":
                result = LamaLogicalNotNodeGen.create(LamaEqualNodeGen.create(leftNode, rightNode));
                break;
            case "&&":
                result = new LamaLogicalAndNode(leftNode, rightNode);
                break;
            case "||":
                result = new LamaLogicalOrNode(leftNode, rightNode);
                break;
            default:
                throw new RuntimeException("unexpected operation: " + opToken.getText());
        }

        int start = leftNode.getSourceCharIndex();
        int length = rightNode.getSourceEndIndex() - start;
        result.setSourceSection(start, length);
        result.addExpressionTag();

        return result;
    }

    public LamaExpressionNode createCall(LamaExpressionNode functionNode, List<LamaExpressionNode> parameterNodes, Token finalToken) {
        if (functionNode == null || containsNull(parameterNodes)) {
            return null;
        }

        final LamaExpressionNode result = new LamaInvokeNode(functionNode, parameterNodes.toArray(new LamaExpressionNode[parameterNodes.size()]));

        final int startPos = functionNode.getSourceCharIndex();
        final int endPos = finalToken.getStartIndex() + finalToken.getText().length();
        result.setSourceSection(startPos, endPos - startPos);
        result.addExpressionTag();

        return result;
    }

    public LamaExpressionNode createAssignment(LamaExpressionNode nameNode, LamaExpressionNode valueNode) {
        return createAssignment(nameNode, valueNode, null);
    }

    public LamaExpressionNode createAssignment(LamaExpressionNode nameNode, LamaExpressionNode valueNode, Integer argumentIndex) {
        if (nameNode == null || valueNode == null) {
            return null;
        }

        String name = (String) ((LamaStringLiteralNode) nameNode).executeGeneric(null);
        FrameSlot frameSlot = frameDescriptor.findOrAddFrameSlot(
                name,
                argumentIndex,
                FrameSlotKind.Illegal);
        FrameSlot existingSlot = lexicalScope.locals.put(name, frameSlot);
        boolean newVariable = existingSlot == null;
//        final LamaExpressionNode result = LamaWriteLocalVariableNodeGen.create(valueNode, frameSlot, nameNode, newVariable);
        final LamaExpressionNode result = null;

        if (valueNode.hasSource()) {
            final int start = nameNode.getSourceCharIndex();
            final int length = valueNode.getSourceEndIndex() - start;
            result.setSourceSection(start, length);
        }
        if (argumentIndex == null) {
            result.addExpressionTag();
        }

        return result;
    }

    /**
     * Returns a {@link LamaReadLocalVariableNode} if this read is a local variable or a
     * {@link LamaFunctionLiteralNode} if this read is global. In Lama, the only global names are
     * functions.
     *
     * @param nameNode The name of the variable/function being read
     * @return either:
     * <ul>
     * <li>A LamaReadLocalVariableNode representing the local variable being read.</li>
     * <li>A LamaFunctionLiteralNode representing the function definition.</li>
     * <li>null if nameNode is null.</li>
     * </ul>
     */
    public LamaExpressionNode createRead(LamaExpressionNode nameNode) {
        if (nameNode == null) {
            return null;
        }

        String name = (String)((LamaStringLiteralNode) nameNode).executeGeneric(null);
        final LamaExpressionNode result;
        final FrameSlot frameSlot = lexicalScope.locals.get(name);
//        if (frameSlot != null) {
//            /* Read of a local variable. */
//            result = LamaReadLocalVariableNodeGen.create(frameSlot);
//        } else {
        /* Read of a global name. In our language, the only global names are functions. */
        result = new LamaFunctionLiteralNode(name);
//        }
        result.setSourceSection(nameNode.getSourceCharIndex(), nameNode.getSourceLength());
        result.addExpressionTag();
        return result;
    }

    public LamaExpressionNode createStringLiteral(Token literalToken, boolean removeQuotes) {
        /* Remove the trailing and ending " */
        String literal = literalToken.getText();
        if (removeQuotes) {
            assert literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"");
            literal = literal.substring(1, literal.length() - 1);
        }

        final LamaStringLiteralNode result = new LamaStringLiteralNode(literal.intern());
        srcFromToken(result, literalToken);
        result.addExpressionTag();
        return result;
    }

    public LamaExpressionNode createNumericLiteral(Token literalToken, Boolean val) {
        LamaExpressionNode result;
        result = new LamaLongLiteralNode(val ? 1 : 0);
        srcFromToken(result, literalToken);
        result.addExpressionTag();
        return result;
    }

    public LamaExpressionNode createNumericLiteral(Token literalToken) {
        LamaExpressionNode result;
        result = new LamaLongLiteralNode(Long.parseLong(literalToken.getText()));
        srcFromToken(result, literalToken);
        result.addExpressionTag();
        return result;
    }

    public LamaExpressionNode createReadProperty(LamaExpressionNode receiverNode, LamaExpressionNode nameNode) {
        if (receiverNode == null || nameNode == null) {
            return null;
        }

//        final LamaExpressionNode result = LamaReadPropertyNodeGen.create(receiverNode, nameNode); // TODO
        final LamaExpressionNode result = null;

        final int startPos = receiverNode.getSourceCharIndex();
        final int endPos = nameNode.getSourceEndIndex();
        result.setSourceSection(startPos, endPos - startPos);
        result.addExpressionTag();

        return result;
    }

    public LamaExpressionNode createWriteProperty(LamaExpressionNode receiverNode, LamaExpressionNode nameNode, LamaExpressionNode valueNode) {
        if (receiverNode == null || nameNode == null || valueNode == null) {
            return null;
        }

        final LamaExpressionNode result = null; // TODO
//        final LamaExpressionNode result = LamaWritePropertyNodeGen.create(receiverNode, nameNode, valueNode);

        final int start = receiverNode.getSourceCharIndex();
        final int length = valueNode.getSourceEndIndex() - start;
        result.setSourceSection(start, length);
        result.addExpressionTag();

        return result;
    }

    /**
     * Creates source description of a single token.
     */
    private static void srcFromToken(LamaStatementNode node, Token token) {
        node.setSourceSection(token.getStartIndex(), token.getText().length());
    }

    /**
     * Checks whether a list contains a null.
     */
    private static boolean containsNull(List<?> list) {
        for (Object e : list) {
            if (e == null) {
                return true;
            }
        }
        return false;
    }

}
