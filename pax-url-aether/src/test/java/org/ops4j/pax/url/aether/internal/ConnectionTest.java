package org.ops4j.pax.url.aether.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;

public class ConnectionTest
{
    private static Logger LOG = LoggerFactory.getLogger(ConnectionTest.class);

    @Test
    public void resolveArtifactWithClassifier()
            throws DependencyCollectionException, ArtifactResolutionException, IOException
    {
        MavenConfiguration config = EasyMock.createMock(MavenConfiguration.class);
        EasyMock.expect(config.getRepositories()).andReturn(new ArrayList<MavenRepositoryURL>()
        {{
                add(new MavenRepositoryURL("http://repo1.maven.org/maven2/"));
            }});
        EasyMock.expect(config.getLocalRepository()).andReturn(new MavenRepositoryURL("file:" + getCache()));
        EasyMock.replay(config);

        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
        Connection connection = new Connection(new URL("aether:org.ops4j.pax.web/features/1.0.1/xml/features"), config);
        InputStream is = connection.getInputStream();
        Assert.assertNotNull(is);
    }

    private String getCache()
            throws IOException
    {
        File base = new File("target");
        base.mkdir();
        File f = File.createTempFile("aethertest", ".dir", base);
        f.delete();
        f.mkdirs();
        LOG.info("Caching" + " to " + f.getAbsolutePath());
        return f.getAbsolutePath();
    }
}
