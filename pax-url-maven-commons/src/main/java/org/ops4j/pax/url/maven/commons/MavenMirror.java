package org.ops4j.pax.url.maven.commons;


/**
 * 
 *         A download mirror for a given repository.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class MavenMirror
    implements java.io.Serializable, java.lang.Cloneable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/
	
	   /**
	    * Field id.
	    */
	   private String id = "default";

    /**
     * 
     *             The server ID of the repository being mirrored,
     * eg
     *             "central". This MUST NOT match the mirror id.
     *           
     */
    private String mirrorOf;

    /**
     * 
     *             The optional name that describes the mirror.
     *           
     */
    private String name;

    /**
     * The URL of the mirror repository.
     */
    private String url;

    /**
     * The layout of the mirror repository. Since Maven 3.
     */
    private String layout;

    /**
     * 
     *             The layouts of repositories being mirrored. This
     * value can be used to restrict the usage
     *             of the mirror to repositories with a matching
     * layout (apart from a matching id). Since Maven 3.
     *           
     */
    private String mirrorOfLayouts = "default,legacy";


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method clone.
     * 
     * @return Mirror
     */
    public MavenMirror clone()
    {
        try
        {
        	MavenMirror copy = (MavenMirror) super.clone();

            return copy;
        }
        catch ( java.lang.Exception ex )
        {
            throw (java.lang.RuntimeException) new java.lang.UnsupportedOperationException( getClass().getName()
                + " does not support clone()" ).initCause( ex );
        }
    } //-- Mirror clone()

    /**
     * Get the layout of the mirror repository. Since Maven 3.
     * 
     * @return String
     */
    public String getLayout()
    {
        return this.layout;
    } //-- String getLayout()

    /**
     * Get the server ID of the repository being mirrored, eg
     *             "central". This MUST NOT match the mirror id.
     * 
     * @return String
     */
    public String getMirrorOf()
    {
        return this.mirrorOf;
    } //-- String getMirrorOf()

    /**
     * Get the layouts of repositories being mirrored. This value
     * can be used to restrict the usage
     *             of the mirror to repositories with a matching
     * layout (apart from a matching id). Since Maven 3.
     * 
     * @return String
     */
    public String getMirrorOfLayouts()
    {
        return this.mirrorOfLayouts;
    } //-- String getMirrorOfLayouts()

    /**
     * Get the optional name that describes the mirror.
     * 
     * @return String
     */
    public String getName()
    {
        return this.name;
    } //-- String getName()

    /**
     * Get the URL of the mirror repository.
     * 
     * @return String
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl()

    /**
     * Set the layout of the mirror repository. Since Maven 3.
     * 
     * @param layout
     */
    public void setLayout( String layout )
    {
        this.layout = layout;
    } //-- void setLayout( String )

    /**
     * Set the server ID of the repository being mirrored, eg
     *             "central". This MUST NOT match the mirror id.
     * 
     * @param mirrorOf
     */
    public void setMirrorOf( String mirrorOf )
    {
        this.mirrorOf = mirrorOf;
    } //-- void setMirrorOf( String )

    /**
     * Set the layouts of repositories being mirrored. This value
     * can be used to restrict the usage
     *             of the mirror to repositories with a matching
     * layout (apart from a matching id). Since Maven 3.
     * 
     * @param mirrorOfLayouts
     */
    public void setMirrorOfLayouts( String mirrorOfLayouts )
    {
        this.mirrorOfLayouts = mirrorOfLayouts;
    } //-- void setMirrorOfLayouts( String )

    /**
     * Set the optional name that describes the mirror.
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName( String )

    /**
     * Set the URL of the mirror repository.
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl( String )

    
            

    public String toString()
    {
        StringBuilder sb = new StringBuilder( 128 );
        sb.append( "Mirror[" );
        sb.append( "id=" ).append( this.getId() );
        sb.append( ",mirrorOf=" ).append( mirrorOf );
        sb.append( ",url=" ).append( this.url );
        sb.append( ",name=" ).append( this.name );
        sb.append( "]" );
        return sb.toString();
    }
    
    //--------------------------/
    //- Class/Member Variables -/
   //--------------------------/



 
   /**
    * Get the id field.
    * 
    * @return String
    */
   public String getId()
   {
       return this.id;
   } //-- String getId()

   /**
    * Set the id field.
    * 
    * @param id
    */
   public void setId( String id )
   {
       this.id = id;
   } //-- void setId( String )


            
          
}