package org.ops4j.pax.url.mvn.internal;

import java.lang.reflect.Field;

import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class ConfigurableSettingsDecrypter extends DefaultSettingsDecrypter
{

    public void setSecurityDispatcher( SecDispatcher securityDispatcher )
    {
        try
        {
            Field field = DefaultSettingsDecrypter.class.getDeclaredField( "securityDispatcher" );
            field.setAccessible( true );
            field.set( this, securityDispatcher );
        }
        catch( Exception exc )
        {
            throw new IllegalStateException( exc );
        }
    }
}
