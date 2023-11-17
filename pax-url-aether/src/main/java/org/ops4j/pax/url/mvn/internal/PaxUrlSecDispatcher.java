/*
 * Copyright 2008 Sonatype, Inc. All rights reserved.
 * Copyright 2014 Harald Wellmann (modified for Pax URL, see end of file).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.mvn.internal;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * @plexus.component role-hint="default"
 * @author Oleg Gusakov</a>
 */
@SuppressWarnings("all")
public class PaxUrlSecDispatcher
implements SecDispatcher
{
    public static final String SYSTEM_PROPERTY_SEC_LOCATION = "settings.security";
    
    public static final String TYPE_ATTR = "type";

    public static final char ATTR_START = '[';

    public static final char ATTR_STOP  = ']';

    /**
     * DefaultHandler
     * 
     * @plexus.requirement
     */
    protected PlexusCipher _cipher;

    /**
     * All available dispatchers
     * 
     * @plexus.requirement role="org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor"
     */
    protected Map<String, PasswordDecryptor> _decryptors;

    /**
     * 
     * @plexus.configuration default-value="~/.settings-security.xml"
     */
    protected String _configurationFile = "~/.settings-security.xml";

    // ---------------------------------------------------------------
    public String decrypt( String str )
        throws SecDispatcherException
    {
        if( ! isEncryptedString( str ) )
            return str;
        
        String bare = null;
        
        try
        {
            bare = _cipher.unDecorate( str );
        }
        catch ( PlexusCipherException e1 )
        {
            throw new SecDispatcherException( e1 );
        }
        
        try
        {
            Map<String, String> attr = stripAttributes( bare );
            
            String res = null;

            SettingsSecurity sec = getSec();
            
            if( attr == null || attr.get( "type" ) == null )
            {
                String master = getMaster( sec );
                
                res = _cipher.decrypt( bare, master );
            }
            else
            {
                String type = attr.get( TYPE_ATTR );
                
                if( _decryptors == null )
                    throw new SecDispatcherException( "plexus container did not supply any required dispatchers - cannot lookup "+type );
                
                Map<?, ?> conf = SecUtil.getConfig( sec, type );
                
                PasswordDecryptor dispatcher = _decryptors.get( type );
                
                if( dispatcher == null )
                    throw new SecDispatcherException( "no dispatcher for hint "+type );
                
                String pass = attr == null ? bare : strip( bare );
                
                return dispatcher.decrypt( pass, attr, conf );
            }
            
            return res;
        }
        catch ( Exception e )
        {
            throw new SecDispatcherException(e);
        }
    }
    
    private String strip( String str )
    {
        int pos = str.indexOf( ATTR_STOP );
        
        if( pos == str.length() )
            return null;
        
        if( pos != -1 )
            return str.substring( pos+1 );
        
        return str;
    }
    
    private Map<String, String> stripAttributes( String str )
    {
        int start = str.indexOf( ATTR_START );
        int stop = str.indexOf( ATTR_STOP );
        if ( start != -1 && stop != -1 && stop > start )
        {
            if( stop == start+1 )
                return null;
            
            String attrs = str.substring( start+1, stop ).trim();
            
            if( attrs == null || attrs.length() < 1 )
                return null;
            
            Map<String, String> res = null;
            
            StringTokenizer st = new StringTokenizer( attrs, ", " );
            
            while( st.hasMoreTokens() )
            {
                if( res == null )
                    res = new HashMap<String, String>( st.countTokens() );
                
                String pair = st.nextToken();
                
                int pos = pair.indexOf( '=' );
                
                if( pos == -1 )
                    continue;
                
                String key = pair.substring( 0, pos ).trim();

                if( pos == pair.length() )
                {
                    res.put( key, null );
                    continue;
                }
                
                String val = pair.substring( pos+1 );
                
                res.put(  key, val.trim() );
            }
            
            return res;
        }
        
        return null;
    }
    //----------------------------------------------------------------------------
    private boolean isEncryptedString( String str )
    {
        if( str == null )
            return false;

        return _cipher.isEncryptedString( str );
    }
    //----------------------------------------------------------------------------
    private SettingsSecurity getSec()
    throws SecDispatcherException
    {
        String location = System.getProperty( SYSTEM_PROPERTY_SEC_LOCATION
                                              , getConfigurationFile()
                                            );
        String realLocation = location.charAt( 0 ) == '~' 
            ? System.getProperty( "user.home" ) + location.substring( 1 )
            : location
            ;
        
        SettingsSecurity sec = SecUtil.read( realLocation, true );
        
        if( sec == null )
            throw new SecDispatcherException( "cannot retrieve master password. Please check that "+realLocation+" exists and has data" );
        
        return sec;
    }
    //----------------------------------------------------------------------------
    private String getMaster( SettingsSecurity sec )
    throws SecDispatcherException
    {
        String master = sec.getMaster();
        
        if( master == null )
            throw new SecDispatcherException( "master password is not set" );
        
        try
        {
            return _cipher.decryptDecorated( master, SYSTEM_PROPERTY_SEC_LOCATION );
        }
        catch ( PlexusCipherException e )
        {
            throw new SecDispatcherException(e);
        }
    }
    //---------------------------------------------------------------
    public String getConfigurationFile()
    {
        return _configurationFile;
    }

    public void setConfigurationFile( String file )
    {
        _configurationFile = file;
    }
    //----------------------------------------------------------------------------
    // ***************************************************************
    /**
     * Encrytion helper
     * @throws IOException 
     */

    //---------------------------------------------------------------
    private static boolean propertyExists( String [] values, String [] av )
    {
        if( values != null )
        {
            for( int i=0; i< values.length; i++ )
            {
                String p = System.getProperty( values[i] );
                
                if( p != null )
                    return true;
            }
        
            if( av != null )
                for( int i=0; i< values.length; i++ )
                    for( int j=0; j< av.length; j++ )
                    {
                        if( ("--"+values[i]).equals( av[j] ) )
                            return true;
                    }
        }
        
        return false;
    }
    
    private static final void usage()
    {
        System.out.println("usage: java -jar ...jar [-m|-p]\n-m: encrypt master password\n-p: encrypt password");
    }
    //---------------------------------------------------------------
    public static void main( String[] args )
    throws Exception
    {
        if( args == null || args.length < 1 )
        {
            usage();
            return;
        }
        
        if( "-m".equals( args[0] ) || propertyExists( SYSTEM_PROPERTY_MASTER_PASSWORD, args ) ) 
            show( true );
        else if( "-p".equals( args[0] ) || propertyExists( SYSTEM_PROPERTY_SERVER_PASSWORD, args ) )
            show( false );
        else
            usage();
    }
    //---------------------------------------------------------------
    private static void show( boolean showMaster )
    throws Exception
    {
        if( showMaster )
            System.out.print("\nsettings master password\n");
        else
            System.out.print("\nsettings server password\n");
        
        System.out.print("enter password: ");
        
        BufferedReader r = new BufferedReader( new InputStreamReader( System.in ) );
        
        String pass = r.readLine();
        
        System.out.println("\n");
        
        DefaultPlexusCipher dc = new DefaultPlexusCipher();
        PaxUrlSecDispatcher dd = new PaxUrlSecDispatcher();
        dd._cipher = dc;
        
        if( showMaster )
            System.out.println( dc.encryptAndDecorate( pass, PaxUrlSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION ) );
        else
        {
            SettingsSecurity sec = dd.getSec();
            System.out.println( dc.encryptAndDecorate( pass, dd.getMaster(sec) ) );
        }
    }
    //---------------------------------------------------------------
    //---------------------------------------------------------------
    
    // hwellmann: added getter and setter to avoid using Plexus Container
    
    public void setCipher( PlexusCipher cipher )
    {
        _cipher = cipher;
    }

    public PlexusCipher getCipher()
    {
        return _cipher;
    }
    
}
