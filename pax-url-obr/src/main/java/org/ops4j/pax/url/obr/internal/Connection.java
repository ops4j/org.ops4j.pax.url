/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.obr.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;

/**
 * Url connection for obr protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, February 01, 2008
 */
class Connection
    extends URLConnection
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Connection.class );

    /**
     * Parsed url.
     */
    private Parser m_parser;
    /**
     * Service configuration.
     */
    private final Configuration m_configuration;
    
    private final ReplaceableService<RepositoryAdmin> m_replaceableService;

    /**
     * Creates a new connection.
     *
     * @param url             the url; cannot be null.
     * @param configuration   service configuration; cannot be null
     * @param repositoryAdmin obr repository admin to use
     * @param filterValidator filter syntax validator
     *
     * @throws java.net.MalformedURLException in case of a malformed url
     * @throws NullArgumentException          if any of the arguments is null
     */
    public Connection( final URL url,
                       final Configuration configuration,
                       final ReplaceableService<RepositoryAdmin> replaceableService,
                       final FilterValidator filterValidator )
        throws MalformedURLException
    {
        super( url );

        NullArgumentException.validateNotNull( url, "URL cannot be null" );
        NullArgumentException.validateNotNull( configuration, "Service configuration" );
        NullArgumentException.validateNotNull( replaceableService, "No Replaceable Service available" );

        m_configuration = configuration;
        m_replaceableService = replaceableService;
        m_parser = new Parser( url.getPath(), filterValidator );
    }


    /**
     * Returns an input stream for the discovered bundle.
     *
     * @return the input stream for the discovered bundle
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        LOG.debug( "Discover resources for filter [" + m_parser.getFilter() + "]" );
        
        m_replaceableService.start();
        RepositoryAdmin repositoryAdmin = m_replaceableService.getService();

        if(repositoryAdmin == null)
        {
        	throw new IllegalStateException("No RepositoryAdmin Service available" );
        }        
        
        final Resource[] resources = repositoryAdmin.discoverResources( m_parser.getFilter() );
        if( resources.length == 0 )
        {
            throw new IOException( "No resource found for provided filter [" + m_parser.getFilter() + "]" );
        }
        if( LOG.isTraceEnabled() )
        {
            LOG.trace( "Found resources:" );
            for( Resource resource : resources )
            {
                final StringBuilder builder = new StringBuilder()
                    .append( "id=" ).append( resource.getId() )
                    .append( ",sn=" ).append( resource.getSymbolicName() )
                    .append( ",v=" ).append( resource.getVersion() )
                    .append( ",url=" ).append( resource.getURL() );
                LOG.trace( "  " + builder.toString() );
            }
        }
        return URLUtils.prepareInputStream( resources[ 0 ].getURL(), !m_configuration.getCertificateCheck() );
    }

    /**
     * Does nothing.
     */
    @Override
    public void connect()
    {
        // do nothing
    }

}