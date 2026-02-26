package org.twelve.gcp.node.imexport;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;


public class ImportSpecifier extends ImportExportSpecifier {

    public ImportSpecifier(Identifier imported, Identifier local) {
        super(imported, local);
    }

    public Identifier module() {
        return ((Import) this.parent()).source().name();
    }

    public Identifier imported() {
        return cast(this.get(0));
    }

    public Identifier local() {
        return cast(this.get(1));
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
}
