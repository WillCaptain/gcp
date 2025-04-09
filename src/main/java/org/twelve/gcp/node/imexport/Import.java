package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.namespace.ModuleNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class Import extends ONode {
    private final ModuleNode source;
    private final List<ImportSpecifier> specifiers = new ArrayList<>();

    public Import(OAST ast, List<Pair<Token,Token>> vars, List<Token> source) {
        super(ast, null);
        if(vars!=null) {
            for (Pair<Token, Token> var : vars) {
                this.specifiers.add(this.addNode(new ImportSpecifier(ast, var.getKey(), var.getValue())));
            }
        }
        this.source = this.addNode(new ModuleNode(ast,source));
    }

    public Import(OAST ast, List<Token> source) {
        this(ast,  null,source);
    }

    public List<ImportSpecifier> specifiers(){
        return this.specifiers;
    }

    public ModuleNode source(){
        return this.source;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        sb.append("import ");
        for (ImportSpecifier specifier : this.specifiers) {
            sb.append(specifier.lexeme()+", ");
        }
        sb.replace(sb.length()-2,sb.length()," from "+this.source.toString()+";");
        return sb.toString();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
