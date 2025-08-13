package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Return;

public class ReturnStatement extends Statement {
    private final Expression expression;

    public ReturnStatement(Expression expression) {
        this(expression.ast(), expression);
//        super(expression.ast());
//        this.expression = this.addNode(expression);
    }

    public ReturnStatement(AST ast) {
        this(ast, null);
//        super(ast);
//        this.expression = null;
    }

    private ReturnStatement(AST ast, Expression expression) {
        super(ast);
        this.outline = ast.Unknown;
        if (expression == null) {
            this.expression = null;
        } else {
            this.expression = this.addNode(expression);
        }
    }

//    @Override
//    public Outline outline() {
//        return this.expression.outline();
//    }

    public Expression expression() {
        return this.expression;
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
