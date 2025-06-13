package org.twelve.gcp.node.expression.accessor;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

/**
 * including array accessor and list accessor
 * map accessor will be put in member accessor
 * a[index]
 */
public class ArrayAccessor extends Accessor {
    private final Expression array;
    private final Expression index;

    public ArrayAccessor(AST ast, Expression array, Expression index ) {
        super(ast);
        this.array = this.addNode(array);
        this.index = this.addNode(index);

    }

    @Override
    public String lexeme() {
        return this.array.lexeme()+"["+this.index.lexeme()+"]";
    }

    public Expression array(){
        return this.array;
    }

    public Expression index(){
        return this.index;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
