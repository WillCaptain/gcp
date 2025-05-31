package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.statement.MemberNode;

import java.util.List;

public class SymbolNode extends EntityNode {


    public SymbolNode(List<MemberNode> members, Node base, Location loc) {
        super(members, base, loc);
    }
}
