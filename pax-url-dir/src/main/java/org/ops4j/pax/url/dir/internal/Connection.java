/*
 * Copyright 2008 Toni Menzel.
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
package org.ops4j.pax.url.dir.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.url.dir.internal.bundle.BundleBuilder;
import org.ops4j.pax.url.dir.workspace.FileTailImpl;
import org.ops4j.pax.url.dir.internal.bundle.ResourceWriter;

/**
 * Accepts URLs like
 * dir:/Users/tonit/devel/pax/testing/$anchor=com.foo.Boo
 * dir:/Users/tonit/devel/pax/testing/$anchor=com.foo.Boo,Bundle-SymbolicName=HelloWorld
 * dir:.$anchor=com.foo.Boo,Bundle-SymbolicName=HelloWorld
 *
 *
 * And even
 * * dir:mytest
 * which uses the relative dir mytest (from current one) without an anchor.
 *
 * Why anchors ?
 * Sometimes you don't know the real class folder.
 * So you want to let the url handler discover it.
 * for example:
 * dir:org/ops4j/pax/Foo.class::
 *
 * @author Toni Menzel (tonit)
 * @since Dec 10, 2008
 */
public class Connection extends URLConnection
{

    private Configuration m_config;
    private Parser m_parser;

    public Connection( URL url, Configuration config )
    {
        super( url );
        NullArgumentException.validateNotNull( url, "url should be provided" );
        NullArgumentException.validateNotNull( config, "config should be provided" );

        m_config = config;
        try
        {
            m_parser = new Parser( url.toExternalForm() );
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( "Given URL [" + url.getPath() + "] is invalid", e );
        }

    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
        Properties p = m_parser.getOptions();
        p.put( "Dynamic-Import", "*" );

        return new BundleBuilder( p,
                                  new ResourceWriter(
                                      new FileTailImpl( m_parser.getDirectory(), m_parser.getTailExpr() )
                                          .getParentOfTail()
                                  )
        )
            .build();


    }

    public void connect()
        throws IOException
    {

    }
}
