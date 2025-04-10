package org.twelve.gcp.node.base;

import com.sun.xml.ws.developer.Serialization;
import org.twelve.gcp.ast.*;
import org.twelve.gcp.node.expression.body.ProgramBody;
import org.twelve.gcp.node.namespace.NamespaceNode;

import java.util.ArrayList;
import java.util.List;

public class Program extends Node {
    private NamespaceNode namespace;
    private ProgramBody body;
    private Token<String> moduleName = null;

    public Program(AST ast) {
        super(ast);
        this.namespace = this.addNode(new NamespaceNode(ast,new ArrayList<>()));
        this.body = this.addNode(new ProgramBody(ast));
    }

//    public NamespaceNode setNamespace(List<Token> nameSpace,Location loc){
//        this.namespace = this.addNode(new NamespaceNode(this.ast(),nameSpace,loc));
//        return namespace;
//    }

    public NamespaceNode setNamespace(List<Token<String>> names){
        this.moduleName = names.remove(names.size()-1);
        this.namespace = this.replaceNode(this.namespace,new NamespaceNode(this.ast(),names));
        return this.namespace;
    }

    @Serialization
    public NamespaceNode namespace(){
        return this.namespace;
    }

    @Serialization
    public ProgramBody body(){
        return body;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        String namespace = this.namespace.lexeme();
        if(namespace!=null && !namespace.trim().equals("")) {
            sb.append("module " + this.namespace.lexeme()+"\n\n");
        }
        sb.append(this.body().lexeme());
        return sb.toString();
    }

    @Override
    public Long scope() {
        return this.body().scope();
    }

    public String moduleName(){
        if(this.moduleName==null){
            return "";
        }else {
            return this.moduleName.lexeme();
        }
    }
}
