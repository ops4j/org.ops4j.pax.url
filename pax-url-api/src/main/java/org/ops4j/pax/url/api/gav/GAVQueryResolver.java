/*
 * Copyright 2012 Rebaze Technology. All Rights Reserved.
 */
package org.ops4j.pax.url.api.gav;

import org.ops4j.pax.url.api.PaxURLException;
import org.ops4j.pax.url.api.QueryResolver;

import static org.ops4j.pax.url.api.gav.GAV.*;

/**
 * Created with IntelliJ IDEA.
 * User: tonit
 * Date: 2/10/12
 * Time: 6:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class GAVQueryResolver implements QueryResolver<GAV> {

    public GAV fromString( String s )
    {
        // TODO: Do propper parsing ;)
        String[] split = s.split( "/" );
        if( split.length > 2 ) {
            return gav().groupId( split[ 0 ] ).artifactId( split[ 1 ] ).version( split[ 2 ] );
        }
        else {
            throw new PaxURLException("Sorry, cannot parse " + s + " properly, yet!");
        }
    }

    public String asURL( GAV gav )
    {
        return gav.groupId() + "/" + gav.artifactId() + "/" + gav.version();
    }
}
