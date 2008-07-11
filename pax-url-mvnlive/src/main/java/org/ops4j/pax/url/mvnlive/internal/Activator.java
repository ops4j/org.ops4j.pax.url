package org.ops4j.pax.url.mvnlive.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.url.commons.handler.ConnectionFactory;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.ops4j.pax.url.mvnlive.ServiceConstants;
import org.ops4j.util.property.PropertyResolver;

/**
 * @author Toni Menzel (tonit)
 * @since Jul 10, 2008
 */
public class Activator extends HandlerActivator<Configuration>
{

    /**
     * @see HandlerActivator#HandlerActivator(String[], String, org.ops4j.pax.url.commons.handler.ConnectionFactory)
     */
    public Activator()
    {
        super(
            new String[]{ ServiceConstants.PROTOCOL },
            ServiceConstants.PID,
            new ConnectionFactory<Configuration>()
            {

                /**
                 * @see ConnectionFactory#createConection(org.osgi.framework.BundleContext , java.net.URL , Object)
                 */
                public URLConnection createConection( final BundleContext bundleContext,
                                                      final URL url,
                                                      final Configuration config )
                    throws MalformedURLException
                {
                    return new Connection( url, config );
                }

                /**
                 * @see ConnectionFactory#createConfiguration(org.ops4j.util.property.PropertyResolver)
                 */
                public Configuration createConfiguration( final PropertyResolver propertyResolver )
                {
                    final ConfigurationImpl config = new ConfigurationImpl( propertyResolver );
                    config.setSettings( new SettingsImpl( config.getSettingsFileUrl() ) );
                    return config;
                }


            }
        );
    }
}