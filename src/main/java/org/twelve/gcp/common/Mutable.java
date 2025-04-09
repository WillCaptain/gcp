package org.twelve.gcp.common;

public enum Mutable {
    True {
        @Override
        public Boolean toBool() {
            return true;
        }
    },False {
        @Override
        public Boolean toBool() {
            return false;
        }
    },Unknown {
        @Override
        public Boolean toBool() {
            return false;
        }
    };

    public abstract Boolean toBool();
}
