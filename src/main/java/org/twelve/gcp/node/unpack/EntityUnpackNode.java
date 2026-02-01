package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class  EntityUnpackNode extends UnpackNode {
    protected List<Field> fields = new ArrayList<>();

    public EntityUnpackNode(AST ast) {
        super(ast, null);
        this.outline = ast.unknown(this);
    }

    public void addField(Identifier id, Identifier as) {
        this.fields.add(new EntityField(this.addNode(id), this.addNode(as)));
    }

    public void addField(Identifier id, UnpackNode nest) {
        this.fields.add(new NestField(id, this.addNode(nest)));
    }

    public void addField(Identifier id) {
        this.fields.add(new EntityField(this.addNode(id), null));
    }

    public List<Field> fields(){
        return this.fields;
    }

    @Override
    public Outline accept(Inferences inferences) {
        super.accept(inferences);
        return inferences.visit(this);
    }

    /*@Override
    public Entity outline() {
        Entity entity = Entity.from(this);
        for (Field field : this.fields) {
            entity.addMember(field.field().name(),field.outline(), Modifier.PUBLIC, false, field.field());
        }
        return entity;
    }*/

    @Override
    public String toString() {
        return "{" + fields.stream().map(Object::toString).collect(Collectors.joining(", ")) + "}";
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline inferred) {
        if(inferred instanceof Entity) {
            Entity entity = cast(inferred);
            for (Field field : this.fields) {
                Optional<EntityMember> member = entity.members().stream().filter(m -> m.name().equals(field.field().name())).findFirst();
                if (member.isPresent()) {
                    field.assign(env, member.get().outline());
                } else {
                    GCPErrorReporter.report(field.field(), GCPErrCode.OUTLINE_MISMATCH, field.field().name() + " doesn't exist in entity");
                }
            }
            return;
        }
        if(inferred instanceof Generic){
            for (Identifier id : this.identifiers()) {
                id.assign(env,Generic.from(id,null));
            }
            return;
        }
        GCPErrorReporter.report(this, GCPErrCode.OUTLINE_MISMATCH,this+" is not an entity");
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

