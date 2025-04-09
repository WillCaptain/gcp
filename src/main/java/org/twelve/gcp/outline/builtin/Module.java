package org.twelve.gcp.outline.builtin;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Module implements Outline {

    private Map<String, Outline> symbols = new HashMap<>();
    private Map<String, List<FirstOrderFunction>> functions = new HashMap<>();

    @Override
    public boolean is(Outline another) {
        return false;
    }

    @Override
    public long id() {
        return CONSTANTS.MODULE_INDEX;
    }

    @Override
    public Node node() {
        return null;
    }

    public void defineSymbol(String name, Outline outline) {
        this.symbols.put(name, outline);
    }

    public Outline lookup(Identifier symbol) {
        String key = symbol.token();
        if (this.symbols.containsKey(key)) {
            return this.symbols.get(key);
        } else {
            ErrorReporter.report(symbol, GCPErrCode.VARIABLE_NOT_DEFINED);
            return null;
        }
    }
}
