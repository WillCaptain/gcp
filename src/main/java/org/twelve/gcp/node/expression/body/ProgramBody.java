package org.twelve.gcp.node.expression.body;

import com.sun.xml.ws.developer.Serialization;
import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.node.imexport.Export;
import org.twelve.gcp.node.imexport.Import;

import java.util.List;
import java.util.stream.Collectors;

public class ProgramBody extends Body {
    public ProgramBody(OAST ast) {
        super(ast);
    }

    @Serialization
    public List<Import> imports() {
        return this.nodes().stream()
                .filter(n -> n instanceof Import)
                .map(n -> (Import) n)
                .collect(Collectors.toList());
    }

    @Serialization
    public List<Statement> statements() {
        return this.nodes().stream()
                .filter(n -> n instanceof Statement)
                .map(n -> (Statement) n)
                .collect(Collectors.toList());
    }

    @Serialization
    public List<Export> exports() {
        return this.nodes().stream()
                .filter(n -> n instanceof Export)
                .map(n -> (Export) n)
                .collect(Collectors.toList());
    }

    public Import addImport(Import imports) {
        return this.addNode(imports);
    }

    public Export addExport(Export export) {
        return this.addNode(export);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        List<Import> imports = this.imports();
        if (imports.size() > 0) {
            sb.append(imports.stream().reduce("", (acc, i) -> acc + i.lexeme() + "\n\n", (s, s2) -> s + s2));
        }
        for (Statement statement : this.statements()) {
            sb.append(statement.lexeme()+"\n");
        }
//        sb.append("\n");

        List<Export> exports = this.exports();
        if (exports.size() > 0) {
            sb.append(this.exports().stream().reduce("", (acc, i) -> acc + i.lexeme() + "\n", (s, s2) -> s + s2)+"\n");
        }
        return sb.substring(0,sb.length()-1);
    }

    public <T extends Statement> T addStatement(T statement) {
        List<Export> exports = this.exports();
        int index = this.nodes().size() - exports.size();
        this.addNode(index, statement);
        return statement;
    }
}
