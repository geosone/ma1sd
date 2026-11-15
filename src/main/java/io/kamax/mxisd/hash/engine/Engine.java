package io.kamax.mxisd.hash.engine;

public interface Engine {
    void updateHashes();

    String getPepper();

    /** Add or refresh the hash of a 3PID that was just bound, without waiting for the next rotation. */
    default void addMapping(io.kamax.mxisd.lookup.ThreePidMapping mapping) {
    }

    /** Drop the hash of a 3PID that was just unbound, without waiting for the next rotation. */
    default void removeMapping(io.kamax.mxisd.lookup.ThreePidMapping mapping) {
    }
}
