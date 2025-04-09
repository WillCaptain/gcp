package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.statement.MemberNode;

import java.util.List;

public class SymbolNode extends EntityNode {


    public SymbolNode(OAST ast, List<MemberNode> members, ONode base, Location loc) {
        super(ast, members, base, loc);
    }
}
