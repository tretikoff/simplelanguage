package com.oracle.truffle.lama.nodes

import com.oracle.truffle.api.instrumentation.GenerateWrapper
import com.oracle.truffle.api.instrumentation.InstrumentableNode
import com.oracle.truffle.lama.nodes.LamaStatementNode
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import java.lang.IllegalArgumentException
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag
import com.oracle.truffle.api.instrumentation.StandardTags.RootBodyTag
import com.oracle.truffle.api.instrumentation.ProbeNode
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode
import com.oracle.truffle.api.frame.VirtualFrame
import com.oracle.truffle.api.instrumentation.Tag
import com.oracle.truffle.api.nodes.Node
import com.oracle.truffle.api.nodes.NodeInfo
import com.oracle.truffle.api.source.SourceSection

@NodeInfo(language = "SL", description = "The abstract base node for all SL statements")
@GenerateWrapper
abstract class LamaStatementNode : Node(), InstrumentableNode {
    var sourceCharIndex = NO_SOURCE
        private set
    var sourceLength = 0
        private set
    private var hasStatementTag = false
    private var hasRootTag = false

    /*
     * The creation of source section can be implemented lazily by looking up the root node source
     * and then creating the source section object using the indices stored in the node. This avoids
     * the eager creation of source section objects during parsing and creates them only when they
     * are needed. Alternatively, if the language uses source sections to implement language
     * semantics, then it might be more efficient to eagerly create source sections and store it in
     * the AST.
     *
     * For more details see {@link InstrumentableNode}.
     */
    @TruffleBoundary
    override fun getSourceSection(): SourceSection? {
        if (sourceCharIndex == NO_SOURCE) {
            // AST node without source
            return null
        }
        val rootNode = rootNode
            ?: // not yet adopted yet
            return null
        val rootSourceSection = rootNode.sourceSection ?: return null
        val source = rootSourceSection.source
        return if (sourceCharIndex == UNAVAILABLE_SOURCE) {
            if (hasRootTag && !rootSourceSection.isAvailable) {
                rootSourceSection
            } else {
                source.createUnavailableSection()
            }
        } else {
            source.createSection(sourceCharIndex, sourceLength)
        }
    }

    fun hasSource(): Boolean {
        return sourceCharIndex != NO_SOURCE
    }

    override fun isInstrumentable(): Boolean {
        return hasSource()
    }

    val sourceEndIndex: Int
        get() = sourceCharIndex + sourceLength

    // invoked by the parser to set the source
    fun setSourceSection(charIndex: Int, length: Int) {
        assert(sourceCharIndex == NO_SOURCE) { "source must only be set once" }
        require(charIndex >= 0) { "charIndex < 0" }
        require(length >= 0) { "length < 0" }
        sourceCharIndex = charIndex
        sourceLength = length
    }

    fun setUnavailableSourceSection() {
        assert(sourceCharIndex == NO_SOURCE) { "source must only be set once" }
        sourceCharIndex = UNAVAILABLE_SOURCE
    }

    override fun hasTag(tag: Class<out Tag?>): Boolean {
        if (tag == StatementTag::class.java) {
            return hasStatementTag
        } else if (tag == RootTag::class.java || tag == RootBodyTag::class.java) {
            return hasRootTag
        }
        return false
    }

    override fun createWrapper(probe: ProbeNode): WrapperNode? {
//        return new LamaStatementNodeWrapper(this, probe);
        return null
    }

    /**
     * Execute this node as as statement, where no return value is necessary.
     */
    abstract fun executeVoid(frame: VirtualFrame?)

    /**
     * Marks this node as being a [StandardTags.StatementTag] for instrumentation purposes.
     */
    fun addStatementTag() {
        hasStatementTag = true
    }

    /**
     * Marks this node as being a [StandardTags.RootTag] and [StandardTags.RootBodyTag]
     * for instrumentation purposes.
     */
    fun addRootTag() {
        hasRootTag = true
    }

    override fun toString(): String {
        return formatSourceSection(this)
    }

    companion object {
        private const val NO_SOURCE = -1
        private const val UNAVAILABLE_SOURCE = -2

        /**
         * Formats a source section of a node in human readable form. If no source section could be
         * found it looks up the parent hierarchy until it finds a source section. Nodes where this was
         * required append a `'~'` at the end.
         *
         * @param node the node to format.
         * @return a formatted source section string
         */
        fun formatSourceSection(node: Node?): String {
            if (node == null) {
                return "<unknown>"
            }
            var section = node.sourceSection
            var estimated = false
            if (section == null) {
                section = node.encapsulatingSourceSection
                estimated = true
            }
            return if (section == null || section.source == null) {
                "<unknown source>"
            } else {
                val sourceName = section.source.name
                val startLine = section.startLine
                String.format("%s:%d%s", sourceName, startLine, if (estimated) "~" else "")
            }
        }
    }
}