package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Identifier;

public abstract class ImportExportSpecifier extends ONode {
    public ImportExportSpecifier(OAST ast, Token a, Token b) {
        super(ast);
        Identifier origin = new Identifier(ast, a);
        this.addNode(origin);
        if(b==null){//a without as b
            this.addNode(origin);// regard it: a as a
        }else {
//            this.addNode(new Identifier(ast, b, origin));//a as b
            this.addNode(new Identifier(ast, b));//a as b
        }
    }

    @Override
    public String lexeme() {
        if(this.nodes().get(0)==this.nodes().get(1)){
            return this.nodes().get(0).lexeme();
        }else{
            return this.nodes().get(0)+" as "+this.nodes().get(1);
        }
    }
}
