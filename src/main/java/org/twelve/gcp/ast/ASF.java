package org.twelve.gcp.ast;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPRuntimeException;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.inference.OutlineInferencer;
import org.twelve.gcp.interpreter.OutlineInterpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.outlineenv.GlobalSymbolEnvironment;
import org.twelve.gcp.outlineenv.GlobalScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Syntax Forest (ASF) - A root container for multiple ASTs (Abstract Syntax Trees).
 * Manages a global symbol environment to enable cross-AST variable imports/exports.
 * Performs iterative type inference across all ASTs until resolution or a max limit is reached.
 *
 * Design Notes:
 * - Acts as a coordinator for modular compilation (e.g., multi-file projects).
 * - Uses a shared GlobalSymbolEnvironment to resolve inter-module dependencies.
 * - Implements a fixed-point inference algorithm (iterative until stable).
 *
 * @author huizi 2025
 */
public class ASF {
    private List<AST> asts = new ArrayList<>();  // All ASTs in this forest
    private Inferencer inferencer = new OutlineInferencer();  // Inference rules/utilities
    private GlobalSymbolEnvironment globalSymbolEnvironment = new GlobalSymbolEnvironment();  // Shared symbol table
    private int leftTimes = CONSTANTS.MAX_INFER_TIMES;

    /**
     * Creates a new AST and adds it to the forest.
     * @return The newly created AST, linked to this ASF's inference and symbol environment.
     */
    public AST newAST() {
        AST ast = new AST(this.inferencer, this);
        this.asts.add(ast);
        return ast;
    }

    /**
     * Returns the global symbol environment for cross-AST symbol resolution.
     */
    public GlobalSymbolEnvironment globalEnv() {
        return this.globalSymbolEnvironment;
    }

    /**
     * Performs iterative type inference across all ASTs:
     * 1. Initializes namespaces for each AST.
     * 2. Runs inference in rounds (max 4 iterations) until no further progress is made.
     * 3. Marks unresolved nodes as errors.
     */
    public boolean infer() {
        // First inference pass + namespace registration
        this.asts.forEach(ast -> {
            String namespaceKey = ast.namespace().lexeme() + "." + ast.name();
            GlobalScope scope = this.globalSymbolEnvironment.createNamespace(namespaceKey);
            scope.attachModule(ast.infer());
        });

        // Fixed-point iteration – semantics of leftTimes / isLastInfer() are
        // preserved exactly so that inference rules that call isLastInfer() keep
        // working correctly.
        while (!this.inferred()) {
            for (AST ast : this.asts) ast.infer();
            if (this.leftTimes == 0) break;
            this.leftTimes--;
        }

        // With AbstractNode.fullyInferred caching, this final check is now O(n) with
        // most nodes being O(1) cache hits, so the redundancy cost is minimal.
        return this.asts.stream().allMatch(AST::inferred);
    }

    /**
     * Checks if all ASTs in the forest have fully resolved inferences.
     */
    public boolean inferred() {
        return this.asts.stream().allMatch(AST::inferred);
    }

    /**
     * 按名称查找 AST。若不存在，抛出带有明确模块名的 {@link GCPRuntimeException}，
     * 而不是 JDK 的 {@link java.util.NoSuchElementException}，方便调用方定位问题。
     */
    public AST get(String name) {
        return this.asts.stream()
                .filter(a -> a.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new GCPRuntimeException(GCPErrCode.MODULE_NOT_DEFINED, "Module not found: " + name));
    }

    public boolean isLastInfer() {
        return this.leftTimes==1;
    }

    /**
     * Interprets all ASTs in this forest using a default {@link OutlineInterpreter}.
     * <p>
     * Symmetric counterpart to {@link #infer()}:
     * <pre>
     *   asf.infer();       // type inference pass
     *   asf.interpret();   // execution pass
     * </pre>
     * For custom configuration (e.g. {@code registerConstructor}), use
     * {@code new OutlineInterpreter().run(asf)} directly.
     *
     * @return the value produced by the last statement of the last AST module.
     */
    public Value interpret() {
        return new OutlineInterpreter().run(this);
    }

    /** Returns all ASTs in the forest in insertion order. */
    public List<AST> asts() {
        return java.util.Collections.unmodifiableList(this.asts);
    }
}