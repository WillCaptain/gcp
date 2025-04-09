package org.twelve.gcp.node.namespace;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.builtin.Module;

import java.util.List;

public class ModuleNode extends ONode {
    private final Identifier name;
    private NamespaceNode namespace = null;

    public ModuleNode(OAST ast, List<Token> source) {
        super(ast,null, new Module());
        Token module = source.remove(source.size() - 1);
        if(source.size()>0) {
            this.namespace = this.addNode(new NamespaceNode(ast, source));
        }
        this.name = this.addNode(new Identifier(ast,module));
    }

    @Override
    public String lexeme() {
        if(this.namespace==null) {
            return this.name.lexeme();
        }else {
            return this.namespace.lexeme()+"."+this.name.lexeme();
        }
    }

    public Identifier name(){
        return this.name;
    }

    public  NamespaceNode namespace(){
        return this.namespace;
    }
}
