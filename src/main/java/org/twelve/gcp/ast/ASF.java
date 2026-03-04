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
import java.util.Map;

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
     * <ol>
     *   <li><b>Phase 1 – Pre-registration</b>: every module's empty {@code Module} shell is
     *       registered in the global symbol environment <em>before</em> any inference begins.
     *       Because {@link org.twelve.gcp.outlineenv.LocalSymbolEnvironment} creates the
     *       {@code Module} in its constructor, {@code ast.symbolEnv().module()} returns the
     *       same object that {@code ast.infer()} will later populate via {@code exportSymbol}.
     *       Pre-registering it breaks the chicken-and-egg deadlock of mutual imports.</li>
     *   <li><b>Phase 2 – First inference pass</b>: all ASTs run inference; exports fill the
     *       pre-registered modules.  Symbols from not-yet-inferred modules become
     *       {@link org.twelve.gcp.outline.decorators.LazyModuleSymbol} placeholders.</li>
     *   <li><b>Phase 3 – Fixed-point iteration</b>: repeated until all nodes are resolved or
     *       the round limit is reached.  Lazy placeholders resolve as modules get populated.</li>
     * </ol>
     */
    public boolean infer() {
        // Phase 1: pre-register all module shells so ImportInference can always find them,
        // even when the source module has not been inferred yet (mutual-import support).
        this.asts.forEach(ast -> {
            String namespaceKey = ast.namespace().lexeme() + "." + ast.name();
            GlobalScope scope = this.globalSymbolEnvironment.createNamespace(namespaceKey);
            scope.attachModule(ast.symbolEnv().module());
        });

        // Phase 2: first inference pass – populates the pre-registered modules via exports.
        this.asts.forEach(AST::infer);

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

    /**
     * Collects every {@link org.twelve.gcp.exception.GCPError} from all ASTs
     * in this forest in a single flat list, ordered by AST insertion order.
     */
    public List<org.twelve.gcp.exception.GCPError> allErrors() {
        return this.asts.stream()
                .flatMap(a -> a.errors().stream())
                .toList();
    }

    /**
     * Returns {@code true} if any AST in this forest has reported at least one error.
     */
    public boolean hasErrors() {
        return this.asts.stream().anyMatch(a -> !a.errors().isEmpty());
    }

    /**
     * JavaDoc-like metadata for all modules. Returns a map with key {@code "modules"}
     * containing a list of per-module metadata (name, namespace, imports, exports,
     * variables, functions, descriptions from comments). Suitable for JSON export.
     */
    public Map<String, Object> meta() {
        List<Map<String, Object>> modules = new ArrayList<>();
        for (AST ast : this.asts) {
            modules.add(ast.meta());
        }
        return Map.of("modules", modules);
    }

    /**
     * Prints all diagnostics to {@link System#err} in a compact, human-readable
     * format.  Each line shows the AST module name, the error description, the
     * source snippet, and the location.
     *
     * <p>Suitable for CLI tools and test output; IDEs should use {@link #allErrors()}
     * and render diagnostics in their own UI.
     */
    public void printDiagnostics() {
        for (AST ast : this.asts) {
            if (ast.errors().isEmpty()) continue;
            System.err.println("── " + ast.name() + " ──────────────────────");
            for (org.twelve.gcp.exception.GCPError e : ast.errors()) {
                System.err.println("  " + e);
            }
        }
    }
}