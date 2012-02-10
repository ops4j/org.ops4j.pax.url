/*
 * Copyright 2012 Rebaze Technology. All Rights Reserved.
 */
package org.ops4j.pax.url.api;

/**
 *
 */
public interface QueryResolver<T> {

    T fromString(String s);

    String asURL(T type);

}
