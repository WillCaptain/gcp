package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Identifier;

import static org.twelve.gcp.common.Tool.cast;


public class ImportSpecifier extends ImportExportSpecifier{

    private final Identifier imported;
    private final Identifier local;

    public ImportSpecifier(OAST ast, Token imported, Token local) {
        super(ast, imported, local);
        this.imported = cast(this.get(0));
        this.local = cast(this.get(1));
    }

    public Identifier imported(){
        return this.imported;
    }

    public Identifier local(){
        return this.local;
    }
}
