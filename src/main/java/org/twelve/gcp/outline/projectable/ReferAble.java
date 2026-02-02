package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;

import java.util.List;

/**
 * projector for references
 */
public interface ReferAble {
    List<Reference> references();

    Outline project(List<OutlineWrapper> types);
    Outline copy();
}
