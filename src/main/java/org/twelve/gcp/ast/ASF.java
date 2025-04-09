package org.twelve.gcp.ast;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.inference.OutlineInferences;
import org.twelve.gcp.outlineenv.GlobalSymbolEnvironment;
import org.twelve.gcp.outlineenv.GlobalScope;

import java.util.ArrayList;
import java.util.List;

public class ASF {

    private List<OAST> asts = new ArrayList<>();
    private Inferences inferences = new OutlineInferences();
    private GlobalSymbolEnvironment globalSymbolEnvironment = new GlobalSymbolEnvironment();

    public OAST newAST() {
        OAST ast = new OAST(this.inferences, this);
        this.asts.add(ast);
        return ast;
    }

    public GlobalSymbolEnvironment globalEnv() {
        return this.globalSymbolEnvironment;
    }

    public void infer() {
        //deal with namespaces
        this.asts.forEach(ast -> {
            GlobalScope scope = this.globalSymbolEnvironment.createNamespace(ast.namespace().lexeme()+"."+ast.name());
            scope.attachModule(ast.infer());
        });
        //infer a few times
        int times = 4;
        while (!this.fullyInferred()) {
            for (OAST ast : this.asts) {
                ast.infer();
            }
            if (--times == 0) {
                //ErrorReporter.report(GCPErrCode.POSSIBLE_ENDLESS_LOOP);
                return;
            }
        }
        //最终为unknown的outline添加error
        for (OAST ast : this.asts) {
            ast.markUnknowns();
        }
    }

    private boolean fullyInferred() {
        return !this.asts.stream().anyMatch(a -> !a.inferred());
    }

    public OAST get(String name) {
        return this.asts.stream().filter(a->a.name().equals(name)).findFirst().get();
    }
}
