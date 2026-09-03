package com.mdl.platform.health;

/**
 * Checks whether the database is reachable.
 * Separated from the controller so tests can mock this interface.
 */
public interface DatabaseHealthService {

    boolean isDatabaseUp();
}
