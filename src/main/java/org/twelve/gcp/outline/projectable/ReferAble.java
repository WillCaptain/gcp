package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;

import java.util.List;

public interface ReferAble {
    List<Reference> references();

    Outline project(List<OutlineWrapper> types);
}
