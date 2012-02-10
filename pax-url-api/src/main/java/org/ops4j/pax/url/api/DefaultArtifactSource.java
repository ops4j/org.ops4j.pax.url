/*
 * Copyright 2012 Rebaze Technology. All Rights Reserved.
 */
package org.ops4j.pax.url.api;

/**
 * Created with IntelliJ IDEA.
 * User: tonit
 * Date: 2/10/12
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultArtifactSource<T> implements ArtifactSource<T> {

    final private T m_value;

    public DefaultArtifactSource( T value )
    {
        m_value = value;
    }

    public T get()
    {
        return m_value;
    }
}
