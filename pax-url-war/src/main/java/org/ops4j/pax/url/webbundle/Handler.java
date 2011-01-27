/*
 * Copyright 2009 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.webbundle;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.ops4j.pax.url.war.internal.ConfigurationImpl;
import org.ops4j.pax.url.war.internal.WebBundleConnection;
import org.ops4j.util.property.PropertiesPropertyResolver;

/**
 * {@link java.net.URLStreamHandler} implementation for "wabbundle:" protocol.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 1.0.0, June 16, 2009
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
        final ConfigurationImpl config = new ConfigurationImpl(
            new PropertiesPropertyResolver( System.getProperties() )
        );
        return new WebBundleConnection( url, config );
   }

}