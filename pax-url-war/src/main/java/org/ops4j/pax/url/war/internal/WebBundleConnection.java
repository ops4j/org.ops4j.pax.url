/*
 * Copyright 2008 Alin Dreghiciu, Achim Nierbeck.
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
package org.ops4j.pax.url.war.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.ops4j.pax.swissbox.bnd.BndUtils;
import org.ops4j.pax.swissbox.bnd.OverwriteMode;
import org.osgi.framework.Constants;

/**
 * Url connection for webbundle protocol handler.
 *
 * @author Guillaume Nodet
 */
public class WebBundleConnection extends WarConnection {
	
    public WebBundleConnection(URL url, Configuration config) throws MalformedURLException
    {
        super(url, config);
    }

    @Override
    protected InputStream createBundle(InputStream inputStream, Properties instructions, String warUri) throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(inputStream, 64 * 1024);
        BufferedInputStream backupStream = new BufferedInputStream(inputStream, 64 * 1024);
        bis.mark(64 * 1024);
        boolean isBundle = false;
        try
        {
            JarInputStream jis = new JarInputStream( bis );
            Manifest man = jis.getManifest();
            if (man.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null)
            {
                isBundle = true;
            }

        }
        catch (IOException e)
        {
            // Ignore
            e.printStackTrace();
        }
        finally
        {
        	if (bis.markSupported()) {
        		try {
        			bis.reset();
        		} catch (IOException ignore) {
        			//Ignore since buffer is already resetted
        		}
        	}
        }
        if (isBundle)
        {
            final Properties originalInstructions = BndUtils.parseInstructions(getURL().getQuery());
            if (originalInstructions.size() > 1
                    || originalInstructions.size() == 1 && !originalInstructions.containsKey("Web-ContextPath"))
            {
                throw new MalformedURLException("The webbundle URL handler can not be used with bundles");
            }
        }
        
        //OSGi-Spec 128.3.1 WAB Definition
        //The Context Path must always begin with a forward slash ( ?/?).
        if(instructions.get("Web-ContextPath") != null) {
	        String ctxtPath = (String) instructions.get("Web-ContextPath");
	        if (!ctxtPath.startsWith("/")) {
	        	ctxtPath = "/"+ctxtPath;
	        	instructions.setProperty("Web-ContextPath", ctxtPath);
	        }
        }
        
        return super.createBundle(backupStream, instructions, warUri, OverwriteMode.MERGE);
    }

}
