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
package org.ops4j.pax.url.war.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ops4j.pax.swissbox.bnd.BndUtils;
import org.ops4j.pax.swissbox.bnd.OverwriteMode;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Url connection for webbundle protocol handler.
 *
 * @author Guillaume Nodet
 */
public class WebBundleConnection extends WarConnection {
	
	private static DocumentBuilderFactory dbf = null;

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
            JarEntry entry;
            List<String> webXmlImports = new ArrayList<String>();
            while ((entry = jis.getNextJarEntry()) != null) {
            	if ("WEB-INF/web.xml".equalsIgnoreCase(((JarEntry)entry).getName())){
            		//Found the web.xml will try to get all "-class" attributes from it to import them
            		if (dbf == null) {
	            		dbf = DocumentBuilderFactory.newInstance();
	            		dbf.setNamespaceAware(true);
            		}
            		DocumentBuilder db = dbf.newDocumentBuilder();
            		
            		Document doc = db.parse(jis);
            		
            		NodeList childNodes = doc.getDocumentElement().getChildNodes();
            		parseChildNodes(webXmlImports, childNodes);
            		
            		break;
            	}
            }
            
            //add extra ImportPackages from web.xml
            String importPackages = instructions.getProperty("Import-Package");

            for (String importPackage : webXmlImports) {
            	importPackage += ","+importPackage;
			}

            instructions.setProperty("Import-Package", importPackages);
            

        }
        catch (IOException e)
        {
            // Ignore
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
			// Ignore
			e.printStackTrace();
		} catch (SAXException e) {
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
        //The Context Path must always begin with a forward slash ( �/�).
        if(instructions.get("Web-ContextPath") != null) {
	        String ctxtPath = (String) instructions.get("Web-ContextPath");
	        if (!ctxtPath.startsWith("/")) {
	        	ctxtPath = "/"+ctxtPath;
	        	instructions.setProperty("Web-ContextPath", ctxtPath);
	        }
        }
        
        return super.createBundle(backupStream, instructions, warUri, OverwriteMode.MERGE);
    }

	/**
	 * @param webXmlImports
	 * @param childNodes
	 */
	private void parseChildNodes(List<String> webXmlImports, NodeList childNodes) {
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			NodeList subNodes = node.getChildNodes();
			if (subNodes != null)
				parseChildNodes(webXmlImports, subNodes);
			String nodeName = node.getNodeName();
			if (nodeName.contains("-class")) {
				//found a class attribute extract package
				String lookupClass = node.getTextContent();
				String packageName = lookupClass.substring(0, lookupClass.lastIndexOf("."));
				webXmlImports.add(packageName);
			}
		}
	}

}
