package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import static org.twelve.gcp.common.Tool.cast;


public class ImportSpecifier extends ImportExportSpecifier {

    public ImportSpecifier(AST ast, Token<String> imported, Token<String> local) {
        super(ast, imported, local);
    }

    public Identifier module() {
        return ((Import) this.parent()).source().name();
    }

    public Identifier imported() {
        return cast(this.get(0));
    }

    public Identifier local() {
        return cast(this.get(1));
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
