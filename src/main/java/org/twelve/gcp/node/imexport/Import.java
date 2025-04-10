package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.namespace.ModuleNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class Import extends Node {
    private final ModuleNode source;
    private final List<ImportSpecifier> specifiers = new ArrayList<>();

    public Import(AST ast, List<Pair<Token<String>,Token<String>>> vars, List<Token<String>> source) {
        super(ast, null);
        if(vars!=null) {
            for (Pair<Token<String>, Token<String>> var : vars) {
                this.specifiers.add(this.addNode(new ImportSpecifier(ast, var.key(), var.value())));
            }
        }
        this.source = this.addNode(new ModuleNode(ast,source));
    }

    public Import(AST ast, List<Token<String>> source) {
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
