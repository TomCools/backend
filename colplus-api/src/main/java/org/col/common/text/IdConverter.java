package org.col.common.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdConverter {
  private static final Logger LOG = LoggerFactory.getLogger(IdConverter.class);
  public static final IdConverter HEX = new IdConverter("0123456789ABCDEF");
  public static final IdConverter LATIN36 = new IdConverter("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
  public static final IdConverter BASE64 = new IdConverter("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
  
  private final char[] chars;
  private final int radix;
  
  public IdConverter(String chars) {
    this.chars = chars.toCharArray();
    radix = chars.length();
    LOG.debug("Created new IdConverter with base {} and chars {}", radix, chars);
  }
  
  public String encode(int id) {
    int strLen = (int) Math.ceil(logb(id, radix));
    byte[] bytes = new byte[strLen];
  
    int idx = strLen;
    while (id > 0) {
      int quot = id % radix;
      bytes[--idx] = (byte) quot;
      id = id / radix;
    }
    return encode(bytes);
  }
  
  public int decode(String id) {
    int num = 0;
    
    return num;
  }
  
  /**
   * Converts each byte as a single character.
   * Only the lower bits are used, upper ones above radix ignored.
   */
  private String encode(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length);
    for (byte idx : bytes) {
      sb.append(chars[idx]);
    }
    return sb.toString();
  }
  
  public static double logb( int a, int b ) {
    return Math.log(a) / Math.log(b);
  }
}
