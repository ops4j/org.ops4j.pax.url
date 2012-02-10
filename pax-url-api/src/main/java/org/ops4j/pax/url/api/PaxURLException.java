/*
 * Copyright 2012 Rebaze Technology. All Rights Reserved.
 */
package org.ops4j.pax.url.api;

/**
 * Created with IntelliJ IDEA.
 * User: tonit
 * Date: 2/10/12
 * Time: 6:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class PaxURLException extends RuntimeException {

    public PaxURLException( String s )
    {
        super( s );
    }

    public PaxURLException( String s, Exception e )
    {
        super( s, e );
    }

    public PaxURLException( Exception e )
    {
        super( e );
    }
}
