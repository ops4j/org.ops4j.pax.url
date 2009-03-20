package org.codehaus.plexus.lang;

/*
 * Copyright 2001-2007 Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @plexus.component role="org.codehaus.plexus.lang.I18N"
 */
public class DefaultI18N
    implements I18N
{

    private static final Object[] NO_ARGS = new Object[0];

    private HashMap bundles;

    private String[] bundleNames;

    private String defaultBundleName;

    private Locale defaultLocale = Locale.getDefault();

    private String defaultLanguage = Locale.getDefault().getLanguage();

    private String defaultCountry = Locale.getDefault().getCountry();

    private boolean devMode;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public String getDefaultLanguage()
    {
        return defaultLanguage;
    }

    public String getDefaultCountry()
    {
        return defaultCountry;
    }

    public String getDefaultBundleName()
    {
        return defaultBundleName;
    }

    public String[] getBundleNames()
    {
        return (String[]) bundleNames.clone();
    }

    public ResourceBundle getBundle()
    {
        return getBundle( getDefaultBundleName(), (Locale) null );
    }

    public ResourceBundle getBundle( String bundleName )
    {
        return getBundle( bundleName, (Locale) null );
    }

    /**
     * This method returns a ResourceBundle given the bundle name and
     * the Locale information supplied in the HTTP "Accept-Language"
     * header.
     *
     * @param bundleName     Name of bundle.
     * @param languageHeader A String with the language header.
     *
     * @return A localized ResourceBundle.
     */
    public ResourceBundle getBundle( String bundleName, String languageHeader )
    {
        return getBundle( bundleName, getLocale( languageHeader ) );
    }

    /**
     * This method returns a ResourceBundle for the given bundle name
     * and the given Locale.
     *
     * @param bundleName Name of bundle (or <code>null</code> for the
     *                   default bundle).
     * @param locale     The locale (or <code>null</code> for the locale
     *                   indicated by the default language and country).
     *
     * @return A localized ResourceBundle.
     */
    public ResourceBundle getBundle( String bundleName, Locale locale )
    {
        // Assure usable inputs.
        bundleName = ( bundleName == null ? getDefaultBundleName() : bundleName.trim() );

        // ----------------------------------------------------------------------
        // A hack to make sure the properties files are always checked
        // ----------------------------------------------------------------------

        if( devMode )
        {
            try
            {
                Class klass = ResourceBundle.getBundle( bundleName ).getClass().getSuperclass();

                Field field = klass.getDeclaredField( "cacheList" );

                field.setAccessible( true );

//                SoftCache cache = (SoftCache) field.get( null );
//
//                cache.clear();

                Object cache = field.get( null );

                cache.getClass().getDeclaredMethod( "clear", null ).invoke( cache, null );

                field.setAccessible( false );
            }
            catch( Exception e )
            {
                // Intentional
            }
        }

        if( locale == null )
        {
            locale = getLocale( null );
        }

        // Find/retrieve/cache bundle.
        ResourceBundle rb;

        HashMap bundlesByLocale = (HashMap) bundles.get( bundleName );

        if( bundlesByLocale != null )
        {
            // Cache of bundles by locale for the named bundle exists.
            // Check the cache for a bundle corresponding to locale.
            rb = (ResourceBundle) bundlesByLocale.get( locale );

            if( rb == null )
            {
                // Not yet cached.
                rb = cacheBundle( bundleName, locale );
            }
        }
        else
        {
            rb = cacheBundle( bundleName, locale );
        }

        return rb;
    }

    /**
     * @see I18N#getLocale(String)
     */
    public Locale getLocale( String header )
    {
        if( !isEmpty( header ) )
        {
            I18NTokenizer tok = new I18NTokenizer( header );

            if( tok.hasNext() )
            {
                return (Locale) tok.next();
            }
        }

        // Couldn't parse locale.
        return defaultLocale;
    }

    private boolean isEmpty( String string )
    {
        return string == null || string.trim().length() == 0;
    }

    public String getString( String key )
    {
        return getString( key, null );
    }

    public String getString( String key, Locale locale )
    {
        return getString( getDefaultBundleName(), locale, key );
    }

    /**
     * @throws MissingResourceException Specified key cannot be matched.
     * @see I18N#getString(String, Locale, String)
     */
    public String getString( String bundleName, Locale locale, String key )
    {
        String value;

        if( locale == null )
        {
            locale = getLocale( null );
        }

        // Look for text in requested bundle.
        ResourceBundle rb = getBundle( bundleName, locale );

        value = getStringOrNull( rb, key );

        // Look for text in list of default bundles.
        if( value == null && bundleNames.length > 0 )
        {
            String name;
            for( int i = 0; i < bundleNames.length; i++ )
            {
                name = bundleNames[ i ];

                if( !name.equals( bundleName ) )
                {
                    rb = getBundle( name, locale );

                    value = getStringOrNull( rb, key );

                    if( value != null )
                    {
                        locale = rb.getLocale();

                        break;
                    }
                }
            }
        }

        if( value == null )
        {
            String loc = locale.toString();

            String mesg =
                "Noticed missing resource: " + "bundleName=" + bundleName + ", locale=" + loc + ", key=" + key;

            // Just send back the key, we don't need to throw an exception.

            value = key;
        }

        return value;
    }

    public String format( String key, Object arg1 )
    {
        return format( defaultBundleName, defaultLocale, key, new Object[]{ arg1 } );
    }

    public String format( String key, Object arg1, Object arg2 )
    {
        return format( defaultBundleName, defaultLocale, key, new Object[]{ arg1, arg2 } );
    }

    /**
     * @see I18N#format(String, Locale, String, Object)
     */
    public String format( String bundleName,
                          Locale locale,
                          String key,
                          Object arg1 )
    {
        return format( bundleName, locale, key, new Object[]{ arg1 } );
    }

    /**
     * @see I18N#format(String, Locale, String, Object, Object)
     */
    public String format( String bundleName,
                          Locale locale,
                          String key,
                          Object arg1,
                          Object arg2 )
    {
        return format( bundleName, locale, key, new Object[]{ arg1, arg2 } );
    }

    /**
     * Looks up the value for <code>key</code> in the
     * <code>ResourceBundle</code> referenced by
     * <code>bundleName</code>, then formats that value for the
     * specified <code>Locale</code> using <code>args</code>.
     *
     * @return Localized, formatted text identified by
     *         <code>key</code>.
     */
    public String format( String bundleName, Locale locale, String key, Object[] args )
    {
        if( locale == null )
        {
            // When formatting Date objects and such, MessageFormat
            // cannot have a null Locale.
            locale = getLocale( null );
        }

        String value = getString( bundleName, locale, key );
        if( args == null )
        {
            args = NO_ARGS;
        }

        // FIXME: after switching to JDK 1.4, it will be possible to clean
        // this up by providing the Locale along with the string in the
        // constructor to MessageFormat.  Until 1.4, the following workaround
        // is required for constructing the format with the appropriate locale:
        MessageFormat messageFormat = new MessageFormat( "" );
        messageFormat.setLocale( locale );
        messageFormat.applyPattern( value );

        return messageFormat.format( args );
    }

    /**
     * Called the first time the Service is used.
     */
    public void initialize()
    {
        bundles = new HashMap();

        defaultLocale = new Locale( defaultLanguage, defaultCountry );

        initializeBundleNames();

        if( "true".equals( System.getProperty( "PLEXUS_DEV_MODE" ) ) )
        {
            devMode = true;
        }
    }

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    protected void initializeBundleNames()
    {
        //System.err.println("cfg=" + getConfiguration());
        if( defaultBundleName != null && defaultBundleName.length() > 0 )
        {
            // Using old-style single bundle name property.
            if( bundleNames == null || bundleNames.length <= 0 )
            {
                bundleNames = new String[]{ defaultBundleName };
            }
            else
            {
                // Prepend "default" bundle name.
                String[] array = new String[bundleNames.length + 1];
                array[ 0 ] = defaultBundleName;
                System.arraycopy( bundleNames, 0, array, 1, bundleNames.length );
                bundleNames = array;
            }
        }
        if( bundleNames == null )
        {
            bundleNames = new String[0];
        }
    }

    /**
     * Caches the named bundle for fast lookups.  This operation is
     * relatively expesive in terms of memory use, but is optimized
     * for run-time speed in the usual case.
     *
     * @throws MissingResourceException Bundle not found.
     */
    private synchronized ResourceBundle cacheBundle( String bundleName, Locale locale )
        throws MissingResourceException
    {
        HashMap bundlesByLocale = (HashMap) bundles.get( bundleName );

        ResourceBundle rb = ( bundlesByLocale == null ? null : (ResourceBundle) bundlesByLocale.get( locale ) );

        if( rb == null )
        {
            bundlesByLocale = ( bundlesByLocale == null ? new HashMap( 3 ) : new HashMap( bundlesByLocale ) );

            try
            {
                rb = ResourceBundle.getBundle( bundleName, locale );
            }
            catch( MissingResourceException e )
            {
                rb = findBundleByLocale( bundleName, locale, bundlesByLocale );

                if( rb == null )
                {
                    throw (MissingResourceException) e.fillInStackTrace();
                }
            }

            if( rb != null )
            {
                // Cache bundle.
                bundlesByLocale.put( rb.getLocale(), rb );

                HashMap bundlesByName = new HashMap( bundles );

                bundlesByName.put( bundleName, bundlesByLocale );

                this.bundles = bundlesByName;
            }
        }

        return rb;
    }

    /**
     * <p>Retrieves the bundle most closely matching first against the
     * supplied inputs, then against the defaults.</p>
     * <p/>
     * <p>Use case: some clients send a HTTP Accept-Language header
     * with a value of only the language to use
     * (i.e. "Accept-Language: en"), and neglect to include a country.
     * When there is no bundle for the requested language, this method
     * can be called to try the default country (checking internally
     * to assure the requested criteria matches the default to avoid
     * disconnects between language and country).</p>
     * <p/>
     * <p>Since we're really just guessing at possible bundles to use,
     * we don't ever throw <code>MissingResourceException</code>.</p>
     */
    private ResourceBundle findBundleByLocale( String bundleName,
                                               Locale locale,
                                               Map bundlesByLocale )
    {
        ResourceBundle rb = null;

        if( isEmpty( locale.getCountry() ) &&
            defaultLanguage.equals( locale.getLanguage() ) )
        {
            /*
            category.debug("Requested language '" + locale.getLanguage() +
                           "' matches default: Attempting to guess bundle " +
                           "using default country '" + defaultCountry + '\'');
            */
            Locale withDefaultCountry = new Locale( locale.getLanguage(),
                                                    defaultCountry
            );
            rb = (ResourceBundle) bundlesByLocale.get( withDefaultCountry );
            if( rb == null )
            {
                rb = getBundleIgnoreException( bundleName, withDefaultCountry );
            }
        }
        else if( isEmpty( locale.getLanguage() ) &&
                 defaultCountry.equals( locale.getCountry() ) )
        {
            Locale withDefaultLanguage = new Locale( defaultLanguage,
                                                     locale.getCountry()
            );
            rb = (ResourceBundle) bundlesByLocale.get( withDefaultLanguage );
            if( rb == null )
            {
                rb = getBundleIgnoreException( bundleName, withDefaultLanguage );
            }
        }

        if( rb == null && !defaultLocale.equals( locale ) )
        {
            rb = getBundleIgnoreException( bundleName, defaultLocale );
        }

        return rb;
    }

    /**
     * Retrieves the bundle using the
     * <code>ResourceBundle.getBundle(String, Locale)</code> method,
     * returning <code>null</code> instead of throwing
     * <code>MissingResourceException</code>.
     */
    private ResourceBundle getBundleIgnoreException( String bundleName, Locale locale )
    {
        try
        {
            return ResourceBundle.getBundle( bundleName, locale );
        }
        catch( MissingResourceException ignored )
        {
            return null;
        }
    }

    /**
     * Gets localized text from a bundle if it's there.  Otherwise,
     * returns <code>null</code> (ignoring a possible
     * <code>MissingResourceException</code>).
     */
    protected final String getStringOrNull( ResourceBundle rb, String key )
    {
        if( rb != null )
        {
            try
            {
                return rb.getString( key );
            }
            catch( MissingResourceException ignored )
            {
                // intentional
            }
        }
        return null;
    }

}
