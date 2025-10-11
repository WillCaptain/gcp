package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class EntityUnpackNode extends UnpackNode {
    private List<Field> fields = new ArrayList<>();

    public EntityUnpackNode(AST ast) {
        super(ast, null);
    }

    public void addField(Identifier id, Identifier as) {
        this.fields.add(new IdField(this.addNode(id), as));
    }

    public void addField(Identifier id, UnpackNode nest) {
        this.fields.add(new NestField(this.addNode(id), this.addNode(nest)));
    }

    public void addField(Identifier id) {
        this.fields.add(new IdField(id, null));
    }


    @Override
    public String toString() {
        return "{" + fields.stream().map(Object::toString).collect(Collectors.joining(", ")) + "}";
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if(!(inferred instanceof Entity)){
            GCPErrorReporter.report(this, GCPErrCode.OUTLINE_MISMATCH,this+" is not an entity");
            return;
        }
        Entity entity = cast(inferred);
        for (Field field : this.fields) {
            Optional<EntityMember> member = entity.members().stream().filter(m -> m.name().equals(field.field().name())).findFirst();
            if(member.isPresent()){
                field.assign(env,member.get().outline());
            }else{
                GCPErrorReporter.report(field.field(), GCPErrCode.OUTLINE_MISMATCH,field.field().name()+" doesn't exist in entity");
            }
        }
    }

    @Override
    public List<Identifier> identifiers() {
        List<Identifier> ids = new ArrayList<>();
        for (Field field : this.fields) {
            ids.addAll(field.identifiers());
        }
        return ids;
    }


}

abstract class Field {
    private final Identifier field;

    Field(Identifier field) {
        this.field = field;
    }

    public Identifier field() {
        return this.field;
    }
    public abstract List<Identifier> identifiers();

    public abstract void assign(LocalSymbolEnvironment env, Outline outline);
}

class IdField extends Field {
    private final Identifier as;

    IdField(Identifier field, Identifier as) {
        super(field);
        this.as = as == null ? field : as;
    }

    public Identifier as() {
        return this.as;
    }

    @Override
    public String toString() {
        if (this.field().equals(this.as)) {
            return this.field().toString();
        } else {
            return this.field().toString() + " as " + this.as.toString();
        }
    }

    @Override
    public List<Identifier> identifiers() {
        List<Identifier> ids = new ArrayList<>();
        ids.add(this.as);
        return ids;
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline outline) {
        this.as.assign(env,outline);
    }
}

class NestField extends Field {

    private final UnpackNode nest;

    NestField(Identifier field, UnpackNode nest) {
        super(field);
        this.nest = nest;
    }

    public UnpackNode nest() {
        return this.nest;
    }

    @Override
    public String toString() {
        return this.field().toString() + ": " + this.nest.toString();
    }

    @Override
    public List<Identifier> identifiers() {
        return this.nest().identifiers();
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline outline) {
        this.nest.assign(env,outline);
    }
}