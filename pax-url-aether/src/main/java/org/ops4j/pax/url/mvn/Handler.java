/*
 * Copyright 2009 Alin Dreghiciu.
 * Copyright 2014 Guillaume Nodet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.ops4j.pax.url.mvn.internal.Connection;

/**
 * {@link URLStreamHandler} implementation for "mvn:" protocol.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @author Toni Menzel
 * @author Guillaume Nodet

 * @since 1.3.0, March 28, 2011 (usable since)
 */
public class Handler
    extends URLStreamHandler
{

    /**
     * {@inheritDoc}
     */
    @Override
    protected URLConnection openConnection( final URL url )
        throws IOException
    {
        MavenResolver resolver = MavenResolvers.createMavenResolver(null, ServiceConstants.PID);
        return new Connection( url, resolver );
    }
}