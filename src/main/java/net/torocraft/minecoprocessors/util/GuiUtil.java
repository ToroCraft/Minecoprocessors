package net.torocraft.minecoprocessors.util;

public class GuiUtil
{
  public static String toBinary(byte b)
  { return String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(' ', '0'); }

  public static String toHex(byte b)
  { return String.format("%02X", (b & 0xff)); }

  public static String toHex(short b)
  { return String.format("%04X", (b & 0xffff)); }
}
