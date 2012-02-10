/*
 * Copyright 2012 Rebaze Technology. All Rights Reserved.
 */
package org.ops4j.pax.url.api;

/**
 * Artifact Handlers are required to register services of this type.
 *
 */
public interface ArtifactProvider<T, Q> {

    ArtifactSource<T> resolve( Q query );
}
