package org.twelve.gcp.ast;

import java.io.Serializable;

public interface Location extends Serializable {
    long start();

    long end();

}
