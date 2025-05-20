package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

public class ExportSpecifier extends ImportExportSpecifier {

    public ExportSpecifier(AST ast, Identifier local, Identifier exported) {
        super(ast, local, exported);
    }

    public Identifier local(){
        return cast(this.get(0));
    }

    public Identifier exported(){
        return cast(this.get(1));
    }
    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
