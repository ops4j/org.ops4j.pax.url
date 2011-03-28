package org.ops4j.pax.url.aether.internal;

import java.io.File;
import java.util.List;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

/**
 * Created by IntelliJ IDEA.
 * User: tonit
 * Date: 3/27/11
 * Time: 12:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class GracefulWagon implements Wagon {

    public void get( String s, File file )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getIfNewer( String s, File file, long l )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void put( File file, String s )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void putDirectory( File file, String s )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean resourceExists( String s )
        throws TransferFailedException, AuthorizationException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getFileList( String s )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean supportsDirectoryCopy()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Repository getRepository()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void connect( Repository repository )
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void connect( Repository repository, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void connect( Repository repository, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void disconnect()
        throws ConnectionException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTimeout( int i )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getTimeout()
    {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addSessionListener( SessionListener sessionListener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeSessionListener( SessionListener sessionListener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasSessionListener( SessionListener sessionListener )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addTransferListener( TransferListener transferListener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeTransferListener( TransferListener transferListener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasTransferListener( TransferListener transferListener )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isInteractive()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setInteractive( boolean b )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
