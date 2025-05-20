package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.outline.Outline;

import java.util.List;
import java.util.Map;

public interface ReferAble {
    List<Reference> references();

    Outline project(List<Outline> types);
}
