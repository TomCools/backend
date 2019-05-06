package org.col.common.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Charsets;

/**
 * Common routines to access classpath resources via the system class loader.
 * If a character encoding is need UTF8 is expected!
 */
public class Resources {
  private final static Pattern TAB_PAT = Pattern.compile("\t");
  
  /**
   * @return stream of lines from a classpath resource file encoded in UTF8
   */
  public static InputStream stream(String resourceName) {
    return ClassLoader.getSystemResourceAsStream(resourceName);
  }
  
  public static boolean exists(String resourceName) {
    URL url = ClassLoader.getSystemResource(resourceName);
    return url != null;
  }

  /**
   * @return stream of lines from a classpath resource file encoded in UTF8
   */
  public static BufferedReader reader(String resourceName) {
    return new BufferedReader(new InputStreamReader(stream(resourceName), Charsets.UTF_8));
  }
  
  /**
   * @return stream of lines from a classpath resource file encoded in UTF8
   */
  public static Stream<String> lines(String resourceName) {
    return reader(resourceName).lines();
  }
  
  public static Stream<String[]> tabRows(String resourceName) {
    return lines(resourceName).map(TAB_PAT::split);
  }
  
  /**
   * Intended for tests only!!!
   * This relies on resources being available as files.
   * Don't use this in a real webapplication with containers!
   *
   * @param resourceName the resource name for the system classloader
   */
  public static File toFile(String resourceName) {
    return new File(ClassLoader.getSystemResource(resourceName).getFile());
  }
  
}
