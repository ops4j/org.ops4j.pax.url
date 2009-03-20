package org.codehaus.plexus.lang;

import java.util.Locale;

/**
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 * @plexus.component role="org.codehaus.plexus.lang.Language"
 */
public class DefaultLanguage
    implements Language
{

    private String bundleName;
    private Locale locale;
    //	private ResourceBundle rb;
    private DefaultI18N i18n = new DefaultI18N();

    //-------------------------------------------------------------------------------------
    public DefaultLanguage()
    {
    }

    //-------------------------------------------------------------------------------------
    public DefaultLanguage( Class clazz )
    {
        this.bundleName = clazz.getPackage().getName() + "." + DEFAULT_NAME;
        i18n.initialize();
    }

    //-------------------------------------------------------------------------------------
    public DefaultLanguage( Class clazz, Locale locale )
    {
        this( clazz );
        this.locale = locale;
//		rb = ResourceBundle.getBundle( clazz.getPackage().getName()+"."+DEFAULT_NAME, locale, clazz.getClassLoader() );
    }

    //-------------------------------------------------------------------------------------
    public String getMessage( String key, String... args )
    {

        if( args == null || args.length == 0 )
        {
            return i18n.getString( bundleName, locale, key );
        }

        return i18n.format( bundleName, locale, key, args );
    }
    //-------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------
}
