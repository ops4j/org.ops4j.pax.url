package org.ops4j.pax.url.mvnlive.internal;

import org.ops4j.util.property.PropertyResolver;

/**
 * @author Toni Menzel (tonit)
 * @since Jul 10, 2008
 */
public class ConfigurationImpl implements Configuration
{

    private Object m_settingsFileUrl;

    public ConfigurationImpl( PropertyResolver propertyResolver )
    {
        
    }

    public Object getSettingsFileUrl()
    {
        return m_settingsFileUrl;
    }

    public void setSettings( Settings settings )
    {
    }
}
