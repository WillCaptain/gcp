package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Identifier;

import static org.twelve.gcp.common.Tool.cast;

public class ExportSpecifier extends ImportExportSpecifier {

    private final Identifier local;
    private final Identifier exported;

    public ExportSpecifier(AST ast, Token local, Token exported) {
        super(ast, local, exported);
        this.local = cast(this.get(0));
        this.exported = cast(this.get(1));
    }

    public Identifier local(){
        return this.local;
    }

    public Identifier exported(){
        return this.exported;
    }
}
