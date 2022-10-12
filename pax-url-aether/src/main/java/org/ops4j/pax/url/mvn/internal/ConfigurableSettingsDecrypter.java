package org.ops4j.pax.url.mvn.internal;

import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

public class ConfigurableSettingsDecrypter extends DefaultSettingsDecrypter
{

    public ConfigurableSettingsDecrypter(SecDispatcher secDispatcher) {
        super(secDispatcher);
    }

}
