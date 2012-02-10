/*
 * Copyright 2012 Rebaze Technology. All Rights Reserved.
 */
package org.ops4j.pax.url.api.gav;

/**
 *
 */
public class GAV {

    private String m_artifact;
    private String m_group;

    private String m_version;
    private String m_classifier;
    private String m_extension;

    private static final String DEFAULT_VERSION = "1";
    private static final String DEFAULT_CLASSIFIER = "jar";

    public static GAV gav()
    {
        return new GAV();
    }

    public GAV artifactId( String s )
    {
        m_artifact = s;
        return this;
    }

    public String artifactId()
    {
        return m_artifact;
    }

    public GAV groupId( String s )
    {
        m_group = s;
        return this;
    }

    public String groupId()
    {
        return m_group;
    }

    public GAV version( String s )
    {
        m_version = s;
        return this;
    }

    public String version()
    {
        return m_version;
    }

    public String classifier()
    {
        return m_classifier;
    }

    public String extension()
    {
        return m_extension;
    }
}
