package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.builtin.UNKNOWN;

public abstract class ImportExportSpecifier extends AbstractNode {
    public ImportExportSpecifier(Identifier a, Identifier b) {
        super(a.ast());
        Identifier origin = a;//new Identifier(ast, a);
        this.addNode(origin);
        if(b==null){//a without as b
            this.addNode(origin);// regard it: a as a
        }else {
//            this.addNode(new Identifier(ast, b, origin));//a as b
//            this.addNode(new Identifier(ast, b));//a as b
            this.addNode(b);//a as b
        }
    }

    @Override
    public String lexeme() {
        if(this.nodes().get(0)==this.nodes().get(1)){
            return this.nodes().getFirst().lexeme();
        }else{
            return this.nodes().get(0)+" as "+this.nodes().get(1);
        }
    }

    @Override
    public boolean inferred() {
        return !(this.outline() instanceof UNKNOWN);
    }
}
