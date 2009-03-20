package org.codehaus.plexus.lang;

import java.util.Locale;

public interface Language
{
  public static String ROLE = Language.class.getName();

  /** look for Messages.properties in the clazz package by default */
  public static final String DEFAULT_NAME = "Messages";

	public String getMessage( String key, String... args )
	;

}