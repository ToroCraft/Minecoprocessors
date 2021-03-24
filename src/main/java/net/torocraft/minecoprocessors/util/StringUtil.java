package net.torocraft.minecoprocessors.util;

import java.util.regex.Pattern;

public class StringUtil
{
  public static String toBinary(byte b)
  { return String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(' ', '0'); }

  public static String toHex(byte b)
  { return String.format("%02X", (b & 0xff)); }

  public static String toHex(short b)
  { return String.format("%04X", (b & 0xffff)); }

  public static String toHex(int b)
  { return String.format("%08X", b); }

  private static final Pattern PATTERN_LINES = Pattern.compile("\r?\n");

  public static String[] splitLines(String lines)
  { return PATTERN_LINES.split(lines); }

}
