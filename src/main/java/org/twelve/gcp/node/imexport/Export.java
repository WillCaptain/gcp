package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class Export extends Node {
    private final List<ExportSpecifier> specifiers = new ArrayList<>();

    public Export(AST ast, List<Pair<Identifier, Identifier>> vars) {
        super(ast, null);
        for (Pair<Identifier, Identifier> var : vars) {
            this.specifiers.add(this.addNode(new ExportSpecifier(ast, var.key(), var.value())));
        }
    }

    public List<ExportSpecifier> specifiers() {
        return this.specifiers;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        sb.append("export ");
        for (ExportSpecifier specifier : this.specifiers) {
            sb.append(specifier.lexeme()+", ");
        }
        sb.replace(sb.length()-2,sb.length(),";");
        return sb.toString();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
