package net.torocraft.minecoprocessors.util;

public class GuiUtil
{
  public static String toBinary(Byte b)
  { return (b==null) ? null : String.format("%8s", Integer.toBinaryString(b & 0xff)).replace(' ', '0'); }

  public static String toHex(Byte b)
  { return (b==null) ? null : String.format("%02X", (b & 0xff)); }

  public static String toHex(Short b)
  { return (b==null) ? null : String.format("%04X", (b & 0xffff)); }
}
