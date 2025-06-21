package org.twelve.gcp.ast;

import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.inference.OutlineInferences;
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
    private Inferences inferences = new OutlineInferences();  // Inference rules/utilities
    private GlobalSymbolEnvironment globalSymbolEnvironment = new GlobalSymbolEnvironment();  // Shared symbol table
    private int leftTimes = CONSTANTS.MAX_INFER_TIMES;

    /**
     * Creates a new AST and adds it to the forest.
     * @return The newly created AST, linked to this ASF's inference and symbol environment.
     */
    public AST newAST() {
        AST ast = new AST(this.inferences, this);
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
        // Initialize namespaces and first inference pass
        this.asts.forEach(ast -> {
            String namespaceKey = ast.namespace().lexeme() + "." + ast.name();
            GlobalScope scope = this.globalSymbolEnvironment.createNamespace(namespaceKey);
            scope.attachModule(ast.infer());  // First inference attempt
        });

        // Fixed-point iteration (up to 2 times)
        while (!this.inferred()) {
            for (AST ast : this.asts) {
                ast.infer();  // Refine inferences
            }
            if (this.leftTimes == 0) {
                // ErrorReporter.report(GCPErrCode.POSSIBLE_ENDLESS_LOOP);
                break;  // Prevents infinite loops for circular dependencies
            }
            this.leftTimes--;
        }

        return this.asts.stream().allMatch(a->a.inferred());
        // Flag unresolved nodes as errors
        //this.asts.forEach(AST::markUnknowns);
    }

    /**
     * Checks if all ASTs in the forest have fully resolved inferences.
     */
    public boolean inferred() {
        return this.asts.stream().allMatch(AST::inferred);
    }

    /**
     * Retrieves an AST by its name (throws NoSuchElementException if not found).
     */
    public AST get(String name) {
        return this.asts.stream()
                .filter(a -> a.name().equals(name))
                .findFirst()
                .get();  // Risk: Throws if absent. Consider .orElse(null) for graceful handling.
    }

    public boolean isLastInfer() {
        return this.leftTimes==1;
    }
}