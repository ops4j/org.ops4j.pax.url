/*
 * Copyright (C) 2013 Andrei Pozolotin
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
package org.ops4j.pax.url.mvn;

/**
 * Detect current Operating System.
 */
public enum OS
{

    LINUX( "linux" ), //
    MAC( "mac" ), //
    WINDOWS( "win" ), //
    ;

    private final String term;

    OS(String term)
    {
        this.term = term;
    }

    /**
     * Find current O/S.
     */
    public static OS current()
    {
        String name = System.getProperty( "os.name" ).toLowerCase();
        for( OS known : OS.values() )
        {
            if( name.contains( known.term ) )
            {
                return known;
            }
        }
        throw new UnsupportedOperationException( "Unknown O/S" );
    }

}
