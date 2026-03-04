package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.LazyModuleSymbol;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Module implements Outline {

    private Map<String, Outline> symbols = new HashMap<>();
    private Map<String, List<FirstOrderFunction>> functions = new HashMap<>();
    private final AST ast;

    public Module(AST ast) {
        this.ast = ast;
    }

    @Override
    public boolean is(Outline another) {
        return false;
    }

    @Override
    public long id() {
        return CONSTANTS.MODULE_INDEX;
    }

    @Override
    public AST ast() {
        return this.ast;
    }

    @Override
    public AbstractNode node() {
        return null;
    }

    public void defineSymbol(String name, Outline outline) {
        this.symbols.put(name, outline);
    }

    /** Returns {@code true} if this module has already exported a symbol with the given name. */
    public boolean hasSymbol(String name) {
        return this.symbols.containsKey(name);
    }

    /**
     * Returns the exported symbol outline, or {@code null} if the symbol has not been
     * exported yet.  Unlike {@link #lookup}, this method never reports an error.
     */
    public Outline getSymbol(String name) {
        return this.symbols.get(name);
    }

    /**
     * Looks up an exported symbol with lazy-resolution support for mutual imports.
     * <ul>
     *   <li>Symbol found → return it immediately.</li>
     *   <li>Symbol not found, not the last inference pass → return a
     *       {@link LazyModuleSymbol} placeholder that will resolve once the
     *       source module exports the symbol in a later round.</li>
     *   <li>Symbol not found, last inference pass → report
     *       {@link GCPErrCode#VARIABLE_NOT_DEFINED} and return {@code null}.</li>
     * </ul>
     */
    public Outline lazyLookup(Identifier symbol, boolean isLastPass) {
        String key = symbol.name();
        if (this.symbols.containsKey(key)) {
            return this.symbols.get(key);
        }
        if (isLastPass) {
            GCPErrorReporter.report(symbol, GCPErrCode.VARIABLE_NOT_DEFINED);
            return null;
        }
        return new LazyModuleSymbol(this, symbol);
    }

    /** Original eager lookup – kept for backward compatibility (e.g. star imports). */
    public Outline lookup(Identifier symbol) {
        String key = symbol.name();
        if (this.symbols.containsKey(key)) {
            return this.symbols.get(key);
        } else {
            GCPErrorReporter.report(symbol, GCPErrCode.VARIABLE_NOT_DEFINED);
            return null;
        }
    }
}
