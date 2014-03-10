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

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.swissbox.bnd.BndUtils;
import org.ops4j.pax.swissbox.bnd.OverwriteMode;
import org.ops4j.pax.url.war.ServiceConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Abstract url connection for wrap protocol handler.
 * Subclasses must provide the processing instructions.
 *
 * @author Alin Dreghiciu, Achim Nierbeck
 * @since 0.1.0, January 14, 2008
 */
abstract class AbstractConnection
    extends URLConnection
{

    /**
     * Service configuration.
     */
    private final Configuration m_configuration;
    
	/**
	 * DocumentBuilderFactory for parsing web.xml files
	 */
	private static DocumentBuilderFactory dbf = null;

    
    /**
     * The pattern blacklist to verify that the jar is "legal" within a web-context.
     */
    private static final Pattern[] blacklist = {
    						Pattern.compile("servlet\\.jar"),
    						Pattern.compile("servlet-[0-9]+(\\.[0-9])+\\.jar"),
    						Pattern.compile("servlet-api\\.jar"),
    						Pattern.compile("servlet-api-[0-9]+(\\.[0-9])+\\.jar"),
    						Pattern.compile("jasper\\.jar"),
    						Pattern.compile("jasper-[0-9]+(\\.[0-9])+\\.jar"),
    						Pattern.compile("jsp-api\\.jar"),
    						Pattern.compile("jsp-api-[0-9]+(\\.[0-9])+\\.jar")};

    /**
     * Creates a new connection.
     *
     * @param url           url to be handled; cannot be null.
     * @param configuration protocol configuration; cannot be null
     *
     * @throws MalformedURLException if url path is empty
     * @throws NullArgumentException if url or configuration is null
     */
    protected AbstractConnection( final URL url,
                                  final Configuration configuration )
        throws MalformedURLException
    {
        super( url );

        NullArgumentException.validateNotNull( url, "URL" );
        NullArgumentException.validateNotNull( configuration, "Configuration" );

        final String path = url.getPath();
        if( path == null || path.trim().length() == 0 )
        {
            throw new MalformedURLException( "Path cannot empty" );
        }

        m_configuration = configuration;
    }

    /**
     * Returns the input stream denoted by the url.
     *
     * @return the input stream for the resource denoted by url
     *
     * @throws java.io.IOException in case of an exception during accessing the resource
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream()
        throws IOException
    {
        connect();
        final Properties instructions = getInstructions();
        PreConditionException.validateNotNull( instructions, "Instructions" );

        // the instructions must always contain the war file
        final String warUri = instructions.getProperty( ServiceConstants.INSTR_WAR_URL );
        if( warUri == null || warUri.trim().length() == 0 )
        {
            throw new IOException(
                "Instructions file must contain a property named " + ServiceConstants.INSTR_WAR_URL
            );
        }

        generateClassPathInstruction( instructions );
        
        generateImportPackageFromWebXML( instructions );

        return createBundle(
                    URLUtils.prepareInputStream(new URL(warUri), m_configuration.getCertificateCheck()),
                    instructions,
                    warUri );
    }

	/**
     * Actually create the bundle based on the parsed instructions and  the given stream
     * @param warUri
     * @param instructions
     * @return
     * @throws IOException
     */
    protected InputStream createBundle(InputStream inputStream, Properties instructions, String warUri) throws IOException
    {
        return BndUtils.createBundle( inputStream, instructions, warUri );
    }
    
    /**
     * Actually create the bundle based on the parsed instructions and  the given stream
     * @param inputStream
     * @param instructions
     * @param warUri
     * @param overwriteMode
     * @return an input stream for the generated bundle
     * @throws IOException
     * 
     * @see BndUtils.createBundle
     */
    protected InputStream createBundle(InputStream inputStream, Properties instructions, String warUri, OverwriteMode overwriteMode) throws IOException
    {
        return BndUtils.createBundle( inputStream, instructions, warUri, overwriteMode );
    }

    /**
     * Returns the processing instructions.
     *
     * @return processing instructions
     *
     * @throws java.io.IOException if instructions file can not be returned
     */
    protected abstract Properties getInstructions()
        throws IOException;

    /**
     * Getter.
     *
     * @return configuration
     */
    protected Configuration getConfiguration()
    {
        return m_configuration;
    }

    /**
     * Generates the Bundle-ClassPath header by merging the Original classpath with:<br/>
     * .<br/>
     * WEB-INF/classes<br/>
     * all jars found in WEB-INF/lib
     *
     * @param instructions instructions
     *
     * @throws java.io.IOException re-thrown from extractJarListFromWar()
     */
    private static void generateClassPathInstruction( final Properties instructions )
        throws IOException
    {
        final List<String> bundleClassPath = new ArrayList<String>();
        // first take the bundle class path if present
        bundleClassPath.addAll( toList( instructions.getProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH ), "," ) );
        // then get the list of jars in WEB-INF/lib
        bundleClassPath.addAll( extractJarListFromWar( instructions.getProperty( ServiceConstants.INSTR_WAR_URL ) ) );
        // check if we have a "WEB-INF/classpath" entry
        if( !bundleClassPath.contains( "WEB-INF/classes" ) )
        {
            bundleClassPath.add( 0, "WEB-INF/classes" );
        }
        // check if we have a "." entry
        /* War archives do have the required classes at WEB-INF/classes "." is not allowed
        if( !bundleClassPath.contains( "." ) )
        {
            bundleClassPath.add( 0, "." );
        }
        */
        // set back the new bundle classpath
        instructions.setProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH, join( bundleClassPath, "," ) );
    }
    
    /**
     * Adds Package-Import for classes contained in the web.xml of the war. 
     * 
     * @param instructions - Properties containing the instructions for the manifest generation
     * @throws IOException 
     */
    private static void generateImportPackageFromWebXML(Properties instructions) throws IOException {
    	String warUri = instructions.getProperty( ServiceConstants.INSTR_WAR_URL );
    	JarFile jarFile = null;
    	List<String> webXmlImports = new ArrayList<String>();
        try
        {
            final JarURLConnection conn = (JarURLConnection) new URL( "jar:" + warUri + "!/" ).openConnection();
            conn.setUseCaches( false );
            jarFile = conn.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while( entries.hasMoreElements() )
            {
            	JarEntry entry = (JarEntry) entries.nextElement();
            	if ("WEB-INF/web.xml".equalsIgnoreCase(entry.getName())){
            		//Found the web.xml will try to get all "-class" attributes from it to import them
            		if (dbf == null) {
            			dbf = DocumentBuilderFactory.newInstance();
            			dbf.setNamespaceAware(true);
            			dbf.setValidating(false);
            			dbf.setAttribute("http://xml.org/sax/features/namespaces", true);
            	        dbf.setAttribute("http://xml.org/sax/features/validation", false);
            	        dbf.setAttribute("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            	        dbf.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            		}
            		DocumentBuilder db = dbf.newDocumentBuilder();

            		Document doc = db.parse(jarFile.getInputStream(entry));

            		NodeList childNodes = doc.getDocumentElement().getChildNodes();
            		parseChildNodes(webXmlImports, childNodes);

            		break;
            	}
            }

            StringBuffer buff = new StringBuffer(instructions.getProperty("Import-Package"));
            
            for (String importPackage : webXmlImports) {
            	if (buff.toString().contains(importPackage))
            		continue; //skip this one it's already included
            	buff.append(",");
            	buff.append(importPackage);
            	buff.append(";resolution:=optional");
            }

            instructions.setProperty("Import-Package", buff.toString());
        } catch( ClassCastException e ) {
            throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file", e );
        } catch (MalformedURLException e) {
        	throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file", e );
		} catch (IOException e) {
			throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file", e );
		} catch (ParserConfigurationException e) {
			throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file", e );
		} catch (SAXException e) {
			throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file", e );
		}
        finally {
            if( jarFile != null )
            {
                try
                {
                    jarFile.close();
                }
                catch( IOException ignore )
                {
                    // ignore
                }
            }
        }
                	
    }


	/**
	 * @param webXmlImports
	 * @param childNodes
	 */
	private static void parseChildNodes(List<String> webXmlImports, NodeList childNodes) {
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			NodeList subNodes = node.getChildNodes();
			if (subNodes != null)
				parseChildNodes(webXmlImports, subNodes);
			String nodeName = node.getNodeName();
			if (nodeName.contains("-class")) {
				//found a class attribute extract package
				String lookupClass = node.getTextContent();
				String packageName = lookupClass.substring(0, lookupClass.lastIndexOf(".")).trim();
				webXmlImports.add(packageName);
			}
		}
	}


    /**
     * Does nothing.
     */
    @Override
    public void connect()
    {
        // do nothing
    }

    /**
     * Extracts the list of jars from a war file. The list will contain all jars under WEB-INF/lib directory.
     *
     * @param warUri war file uri
     *
     * @return list of jars
     *
     * @throws java.io.IOException re-thrown from accessing urls or if the warUri does not refer to a jar
     */
    private static List<String> extractJarListFromWar( final String warUri )
        throws IOException
    {
        final List<String> list = new ArrayList<String>();
        JarFile jarFile = null;
        try
        {
            final JarURLConnection conn = (JarURLConnection) new URL( "jar:" + warUri + "!/" ).openConnection();
            conn.setUseCaches( false );
            jarFile = conn.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while( entries.hasMoreElements() )
            {
                JarEntry entry = (JarEntry) entries.nextElement();
                String name = entry.getName();
                if( !name.startsWith( "WEB-INF/lib/" ) )
                {
                    continue;
                }
                if( !name.endsWith( ".jar" ) )
                {
                    continue;
                }
                if ( !checkJarIsLegal(name) )
                {
                	continue;
                }
                list.add( name );
            }
        }
        catch( ClassCastException e )
        {
            throw new IOException( "Provided url [" + warUri + "] does not refer a valid war file" );
        }
        finally
        {
            if( jarFile != null )
            {
                try
                {
                    jarFile.close();
                }
                catch( IOException ignore )
                {
                    // ignore
                }
            }
        }
        return list;
    }

    /**
     * verifies that the given jar name is not contained
     * in the blacklist.
     * 
     * @param name of the jar which needs verification
     * @return 
     * 		true - if the jar is a legal jar </br> 
     * 		false - if the jar is not supposed to be in a war archive like servlet.jar
     */
    protected static boolean checkJarIsLegal(String name) {
    	boolean isMatched = false;
    	for (Pattern pattern : blacklist) {
			isMatched = pattern.matcher(name).find();
			if (isMatched) {
				break;
			}
		}

		return !isMatched;
	}

	/**
     * Splits a delimiter separated string into a list.
     *
     * @param separatedString string to be split
     * @param delimiter       delimiter
     *
     * @return list composed out of the string segments
     */
    protected static List<String> toList( final String separatedString, final String delimiter )
    {
        final List<String> list = new ArrayList<String>();
        if( separatedString != null )
        {
            list.addAll( Arrays.asList( separatedString.split( delimiter ) ) );
        }
        return list;
    }

    /**
     * Joins elements from a collection into a delimiter separated string.
     *
     * @param strings   collection of ellements
     * @param delimiter delimiter
     *
     * @return string composed from the collection elements delimited by the delimiter
     */
    protected static String join( final Collection<String> strings, final String delimiter )
    {
        final StringBuffer buffer = new StringBuffer();
        final Iterator<String> iter = strings.iterator();
        while( iter.hasNext() )
        {
            buffer.append( iter.next() );
            if( iter.hasNext() )
            {
                buffer.append( delimiter );
            }
        }
        return buffer.toString();
    }

}