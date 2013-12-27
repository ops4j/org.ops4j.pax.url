package org.ops4j.pax.url.mvn.internal;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

public class ConfigurableSecDispatcher extends DefaultSecDispatcher
{

    public void setCipher( PlexusCipher cipher )
    {
        _cipher = cipher;
    }

    public PlexusCipher getCipher()
    {
        return _cipher;
    }

}
