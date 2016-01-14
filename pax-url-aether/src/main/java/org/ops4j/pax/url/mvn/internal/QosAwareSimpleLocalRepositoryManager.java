package org.ops4j.pax.url.mvn.internal;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.*;

import java.io.File;

public class QosAwareSimpleLocalRepositoryManager implements LocalRepositoryManager {
    LocalRepositoryManager delegate;

    public QosAwareSimpleLocalRepositoryManager(RepositorySystemSession session, LocalRepository repository){
        SimpleLocalRepositoryManagerFactory factory = new SimpleLocalRepositoryManagerFactory();
        try {
            delegate = factory.newInstance(session, repository);
        } catch (NoLocalRepositoryManagerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request )
    {
        String path = getPathForArtifact( request.getArtifact(), false );
        File file = new File( getRepository().getBasedir(), path );

        LocalArtifactResult result = new LocalArtifactResult( request );
        if ( file.isFile() )
        {
            result.setFile( file );
            result.setAvailable( true );
        }

        if(request.getRepositories().size() != 0 ){
            for (RemoteRepository repo : request.getRepositories()){
                if(RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(session.getUpdatePolicy()) || RepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(repo.getPolicy(false).getUpdatePolicy())){
                    result.setAvailable( false );
                    result.setFile(null);
                    break;
                }
            }
        }

        return result;
    }

    String getPathForArtifact( Artifact artifact, boolean local )
    {
        StringBuilder path = new StringBuilder( 128 );

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '/' );

        path.append( artifact.getBaseVersion() ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '-' );
        if ( local )
        {
            path.append( artifact.getBaseVersion() );
        }
        else
        {
            path.append( artifact.getVersion() );
        }

        if ( artifact.getClassifier().length() > 0 )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        if ( artifact.getExtension().length() > 0 )
        {
            path.append( '.' ).append( artifact.getExtension() );
        }

        return path.toString();
    }


    @Override
    public LocalRepository getRepository() {
        return delegate.getRepository();
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        return delegate.getPathForLocalArtifact(artifact);
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteArtifact(artifact, repository, context);
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        return delegate.getPathForLocalMetadata(metadata);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteMetadata(metadata, repository, context);
    }


    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        delegate.add(session, request);
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        return delegate.find(session, request);
    }

    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        delegate.add(session,request);
    }
}


