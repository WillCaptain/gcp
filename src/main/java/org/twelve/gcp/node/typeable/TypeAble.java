package org.twelve.gcp.node.typeable;

import org.twelve.gcp.ast.Location;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public interface TypeAble {
    Outline infer(Inferences inferences);

    Outline outline();

    Location loc();

    String lexeme();

    Outline inferOutline();
}
