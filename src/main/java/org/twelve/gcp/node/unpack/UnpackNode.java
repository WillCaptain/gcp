package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.node.expression.Identifier;

import java.util.ArrayList;
import java.util.List;

public abstract class UnpackNode extends Assignable {
    public UnpackNode(AST ast, Location loc) {
        super(ast, loc);
    }

    abstract public List<Identifier> identifiers();
}
