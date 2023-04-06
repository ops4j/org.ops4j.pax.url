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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.Ops4jException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.net.URLUtils;
import org.ops4j.pax.swissbox.bnd.BndUtils;
import org.ops4j.pax.swissbox.bnd.OverwriteMode;
import org.ops4j.pax.url.war.ServiceConstants;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger( BndUtils.class );

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

        String manifestVersion = instructions.getProperty(Constants.BUNDLE_MANIFESTVERSION );
        if (manifestVersion != null && !"2".equals(manifestVersion)) {
            throw new IllegalArgumentException("Can't support " + Constants.BUNDLE_MANIFESTVERSION
                    + ": " + manifestVersion);
        }

        generateClassPathInstruction( instructions );
        
        generateImportPackageFromWebXML( instructions );

        return createBundle(
                    URLUtils.prepareInputStream(new URL(warUri), !m_configuration.getCertificateCheck()),
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
     * @param jarInputStream
     * @param instructions
     * @param jarInfo
     * @param overwriteMode
     * @return an input stream for the generated bundle
     * @throws IOException
     * 
     * @see {@code org.ops4j.pax.swissbox.bnd.BndUtils#createBundle()}
     */
    protected InputStream createBundle(InputStream jarInputStream, Properties instructions, String jarInfo, OverwriteMode overwriteMode) throws IOException
    {
        // a copy from pax-swissbox-bnd, because we have to get rid of signing attributes

        NullArgumentException.validateNotNull( jarInputStream, "Jar URL" );
        NullArgumentException.validateNotNull( instructions, "Instructions" );
        NullArgumentException.validateNotEmpty( jarInfo, "Jar info" );

        LOG.debug( "Creating bundle for [" + jarInfo + "]" );
        LOG.debug( "Overwrite mode: " + overwriteMode );
        LOG.trace( "Using instructions " + instructions );

        final Jar jar = new Jar( "dot", jarInputStream );
        Manifest manifest = null;
        try
        {
            manifest = jar.getManifest();
        }
        catch ( Exception e )
        {
            jar.close();
            throw new Ops4jException( e );
        }


        // Make the jar a bundle if it is not already a bundle
        if( manifest == null
                || OverwriteMode.KEEP != overwriteMode
                || ( manifest.getMainAttributes().getValue( Analyzer.EXPORT_PACKAGE ) == null
                && manifest.getMainAttributes().getValue( Analyzer.IMPORT_PACKAGE ) == null )
        )
        {
            // Do not use instructions as default for properties because it looks like BND uses the props
            // via some other means then getProperty() and so the instructions will not be used at all
            // So, just copy instructions to properties
            final Properties properties = new Properties();
            properties.putAll( instructions );

            properties.put( "Generated-By-Ops4j-Pax-From", jarInfo );

            final Analyzer analyzer = new Analyzer();
            analyzer.setJar( jar );
            analyzer.setProperties( properties );
            if( manifest != null && OverwriteMode.MERGE == overwriteMode )
            {
                analyzer.mergeManifest( manifest );
            }
            checkMandatoryProperties( analyzer, jar, jarInfo );
            try
            {
                Manifest newManifest = analyzer.calcManifest();
                for (Map.Entry<String, Attributes> e : newManifest.getEntries().entrySet()) {
                    Attributes attrs = e.getValue();
                    for (Object k : attrs.keySet()) {
                        String key = k.toString();
                        if (key.matches("^[a-zA-Z0-9-]+-Digest(-[a-zA-Z0-9]+)?")) {
                            attrs.remove(k);
                        }
                    }
                }

                jar.setManifest( newManifest );
            }
            catch ( Exception e )
            {
                jar.close();
                throw new Ops4jException( e );
            }
        }

        return createInputStream( jar );
    }

    /**
     * Creates an piped input stream for the wrapped jar.
     * This is done in a thread so we can return quickly.
     *
     * @param jar the wrapped jar
     *
     * @return an input stream for the wrapped jar
     *
     * @throws java.io.IOException re-thrown
     */
    private static PipedInputStream createInputStream( final Jar jar )
            throws IOException
    {
        final CloseAwarePipedInputStream pin = new CloseAwarePipedInputStream();
        final PipedOutputStream pout = new PipedOutputStream( pin );

        new Thread()
        {
            public void run()
            {
                try
                {
                    jar.write( pout );
                }
                catch( Exception e )
                {
                    if (pin.closed)
                    {
                        // logging the message at DEBUG logging instead
                        // -- reading thread probably stopped reading
                        LOG.debug( "Bundle cannot be generated, pipe closed by reader", e );
                    }
                    else {
                        LOG.warn( "Bundle cannot be generated", e );
                    }
                }
                finally
                {
                    try
                    {
                        jar.close();
                        pout.close();
                    }
                    catch( IOException ignore )
                    {
                        // if we get here something is very wrong
                        LOG.error( "Bundle cannot be generated", ignore );
                    }
                }
            }
        }.start();

        return pin;
    }

    /**
     * Check if manadatory properties are present, otherwise generate default.
     *
     * @param analyzer     bnd analyzer
     * @param jar          bnd jar
     * @param symbolicName bundle symbolic name
     */
    private static void checkMandatoryProperties( final Analyzer analyzer,
            final Jar jar,
            final String symbolicName )
    {
        final String importPackage = analyzer.getProperty( Analyzer.IMPORT_PACKAGE );
        if( importPackage == null || importPackage.trim().length() == 0 )
        {
            analyzer.setProperty( Analyzer.IMPORT_PACKAGE, "*;resolution:=optional" );
        }
        final String exportPackage = analyzer.getProperty( Analyzer.EXPORT_PACKAGE );
        if( exportPackage == null || exportPackage.trim().length() == 0 )
        {
            analyzer.setProperty( Analyzer.EXPORT_PACKAGE, "*" );
        }
        final String localSymbolicName = analyzer.getProperty( Analyzer.BUNDLE_SYMBOLICNAME, symbolicName );
        analyzer.setProperty( Analyzer.BUNDLE_SYMBOLICNAME, localSymbolicName.replaceAll( "[^a-zA-Z_0-9.-]", "_" ) );
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
        // according to 128.4.5 WAR Manifest Processing, we need to deduct Bundle-ClassPath ONLY if it's not
        // specified.
        // first take the bundle class path if present
        bundleClassPath.addAll( toList( instructions.getProperty( ServiceConstants.INSTR_BUNDLE_CLASSPATH ), "," ) );
        boolean needsDefault = bundleClassPath.isEmpty();
        if (needsDefault) {
            // only now add the defaults - even if original Bundle-ClassPath doesn't contain e.g., /WEB-INF/classes
            bundleClassPath.add("WEB-INF/classes");
            // then get the list of jars in WEB-INF/lib - but also sanitazed list of entries referenced from those
            // jars' Class-Path header (non-OSGi)
            bundleClassPath.addAll( extractJarListFromWar( instructions.getProperty( ServiceConstants.INSTR_WAR_URL ) ) );
        }

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
                        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

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
        // record all JARs inside the WAR - even if outside WEB-INF/lib, because they may be referenced
        // from other JARs
        Set<String> webInfLibJars = new HashSet<>();
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
                while( name.startsWith("/") )
                {
                    name = name.substring(1);
                }
                if( !name.endsWith( ".jar" ) )
                {
                    continue;
                }
                if ( !checkJarIsLegal(name) )
                {
                    continue;
                }
                if( name.startsWith( "WEB-INF/lib/" ) )
                {
                    webInfLibJars.add(name);
                }
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

        // now recursively process all WEB-INF/lib/*.jar to check their Class-Path - and if there are any valid
        // jars referenced, process them too
        Queue<String> toProcess = new LinkedList<>();
        Set<String> processed = new HashSet<>();
        toProcess.addAll(webInfLibJars);
        processed.addAll(webInfLibJars);
        while (!toProcess.isEmpty()) {
            String jarName = toProcess.remove();

            // never starts with "/" and jarName is always relative to the root of the bundle
            final JarURLConnection conn = (JarURLConnection) new URL("jar:" + warUri + "!/" + jarName).openConnection();
            conn.setUseCaches(false);
            try (JarFile jf = conn.getJarFile()) {
                ZipEntry entry = jf.getEntry(jarName);
                try (InputStream is = jf.getInputStream(entry)) {
                    // existence verified
                    list.add(jarName);
                    JarInputStream embeddedJar = new JarInputStream(is);
                    String cp = embeddedJar.getManifest().getMainAttributes().getValue("Class-Path");
                    String[] cpTab = cp.split("\\s*,\\s*");
                    Path root = Paths.get("/", jarName).getParent();
                    for (String elem : cpTab) {
                        if (!elem.startsWith("/")) {
                            // relativize
                            Path newJar = root.resolve(elem).normalize();
                            elem = newJar.toString();
                        }
                        while (elem.startsWith("/")) {
                            elem = elem.substring(1);
                        }
                        if (processed.add(elem)) {
                            toProcess.add(elem);
                        }
                    }
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
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

    /**
     * PipedInputStream implementation that keeps track of whether it has been closed or not.
     */
    private static final class CloseAwarePipedInputStream extends PipedInputStream
    {
        private boolean closed = false;

        public void close() throws IOException
        {
            closed = true;
            super.close();
        }
    }

}
