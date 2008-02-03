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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.obr.RepositoryAdmin;
import org.ops4j.pax.swissbox.tracker.ReplaceableService;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.obr.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;

/**
 * Bundle activator for obr: protocol handler.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, February 01, 2008
 */
public final class Activator
    implements BundleActivator
{

    /**
     * Delegate.
     */
    private HandlerActivator<Configuration> m_activatorDelegate;
    /**
     * OBR Repository Admin in use. Valid only after start method has been called.
     */
    private RepositoryAdmin m_repositoryAdmin;
    /**
     * Bundle context. Valid only after start method has been called.
     */
    private BundleContext m_bundleContext;

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, ConnectionFactory)
     */
    public Activator()
    {
        m_activatorDelegate = new HandlerActivator<Configuration>(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<Configuration>()
            {

                /**
                 * @see org.ops4j.pax.url.commons.handler.ConnectionFactory#createConection(BundleContext, URL, Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final Configuration config )
                    throws MalformedURLException
                {
                    return new Connection( url,
                                           config,
                                           m_repositoryAdmin,
                                           new FilterValidator()
                                           {
                                               /**
                                                * Validates filter syntax by creating an OSGi filter. If an
                                                * @see FilterValidator#validate(String)
                                                */
                                               public boolean validate( final String filter )
                                               {
                                                   try
                                                   {
                                                       m_bundleContext.createFilter( filter );
                                                       return true;
                                                   }
                                                   catch( InvalidSyntaxException e )
                                                   {
                                                       return false;
                                                   }
                                               }
                                           }
                    );
                }

                /**
                 * @see org.ops4j.pax.url.commons.handler.ConnectionFactory#createConfiguration(org.ops4j.util.property.PropertyResolver)
                 */
                public Configuration createConfiguration( final PropertyResolver propertyResolver )
                {
                    return new ConfigurationImpl( propertyResolver );
                }

            }
        );
    }

    /**
     * Delegates to HandlerActivator.
     *
     * @see org.osgi.framework.BundleActivator#start(BundleContext)
     */
    public void start( final BundleContext bundleContext )
        throws Exception
    {
        m_bundleContext = bundleContext;
        final ReplaceableService<RepositoryAdmin> replaceableService =
            new ReplaceableService<RepositoryAdmin>( bundleContext, RepositoryAdmin.class );
        replaceableService.start();
        m_repositoryAdmin = replaceableService.getService();
        m_activatorDelegate.start( bundleContext );
    }

    /**
     * Delegates to HandlerActivator.
     *
     * @see org.osgi.framework.BundleActivator#stop(BundleContext)
     */
    public void stop( final BundleContext bundleContext )
        throws Exception
    {
        if( m_activatorDelegate != null )
        {
            m_activatorDelegate.stop( bundleContext );
            m_activatorDelegate = null;
        }
        m_bundleContext = null;
    }

}