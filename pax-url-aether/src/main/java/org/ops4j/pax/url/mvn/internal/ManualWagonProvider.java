/*
 * Copyright (C) 2010 Toni Menzel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.providers.http.LightweightHttpsWagon;
import org.eclipse.aether.transport.wagon.WagonProvider;

/**
 * Simplistic wagon provider
 */
public class ManualWagonProvider implements WagonProvider
{

    private int timeout;

    public ManualWagonProvider( int timeout )
    {
        this.timeout = timeout;
    }

    public Wagon lookup( String roleHint ) throws Exception
    {
        if( "file".equals( roleHint ) )
        {
            return new FileWagon();
        }
        else if( "http".equals( roleHint ) )
        {
            LightweightHttpWagon lightweightHttpWagon = new LightweightHttpWagon();
            lightweightHttpWagon.setTimeout( timeout );
            lightweightHttpWagon.setAuthenticator( new LightweightHttpWagonAuthenticator() );
            return lightweightHttpWagon;
        }
        else if( "https".equals( roleHint ) )
        {
            LightweightHttpsWagon lightweightHttpWagon = new LightweightHttpsWagon();
            lightweightHttpWagon.setTimeout( timeout );
            lightweightHttpWagon.setAuthenticator( new LightweightHttpWagonAuthenticator() );
            return lightweightHttpWagon;
        }

        return null;
    }

    public void release( Wagon wagon )
    {
    }
}
