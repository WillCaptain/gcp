package org.twelve.gcp.node.expression.identifier;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Token;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class SymbolIdentifier extends Identifier {
    public SymbolIdentifier(AST ast, Token<String> token) {
        super(ast, token);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
    }


    public Outline merge(Outline outline, Inferencer inferencer) {
        return outline;
    }
}
