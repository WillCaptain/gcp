package org.twelve.gcp.node.imexport;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.namespace.ModuleNode;
import org.twelve.gcp.outline.Outline;

import java.util.ArrayList;
import java.util.List;

public class Import extends Node {
    private final ModuleNode source;
    private final List<ImportSpecifier> specifiers = new ArrayList<>();

    public Import(List<Pair<Identifier,Identifier>> vars, List<Identifier> source) {
        super(source.getFirst().ast(), null);
        if(vars!=null) {
            for (Pair<Identifier, Identifier> var : vars) {
                this.specifiers.add(this.addNode(new ImportSpecifier(var.key(), var.value())));
            }
        }
        this.source = this.addNode(new ModuleNode(source));
    }

    public Import(List<Identifier> source) {
        this(null,source);
    }

    public List<ImportSpecifier> specifiers(){
        return this.specifiers;
    }

    public ModuleNode source(){
        return this.source;
    }

    @Override
    public Location loc() {
        Location loc = super.loc();
        return new SimpleLocation(loc.start()-7,loc.end()+2);
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
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
