package net.torocraft.minecoprocessors.util;

public class ByteUtil {

  public static boolean getBit(long l, int position) {
    return (l & 1 << position) != 0;
  }

  public static byte setBit(byte b, boolean bit, int position) {
    if (bit) {
      return (byte) (b | 1 << position);
    }
    return (byte) (b & ~(1 << position));
  }

  public static long setBit(long l, boolean bit, int position) {
    if (bit) {
      return l | 1 << position;
    }
    return l & ~(1 << position);
  }

  public static byte getByte(long l, int position) {
    return (byte) (l >> 8 * position);
  }

  public static short setByte(short s, byte b, int position) {
    if (position > 1) {
      throw new IndexOutOfBoundsException("position of " + position);
    }
    int mask = ~(0xff << position * 8);
    int insert = b << position * 8;
    return (short) (s & mask | insert);
  }

  public static int setByte(int i, byte b, int position) {
    if (position > 3) {
      throw new IndexOutOfBoundsException("byte position of " + position);
    }
    int mask = ~(0xff << position * 8);
    int insert = b << position * 8;
    return i & mask | insert;
  }

  public static long setByte(long l, byte b, int position) {
    if (position > 7) {
      throw new IndexOutOfBoundsException("position of " + position);
    }
    long mask = ~(0xffL << position * 8);
    long insert = (long) b << position * 8;
    return l & mask | insert;
  }

  public static short getShort(long l, int position) {
    return (short) (l >> position * 16);
  }

  public static long setShort(long l, short b, int position) {
    if (position > 3) {
      throw new IndexOutOfBoundsException("short position of " + position);
    }
    long mask = ~(0xffffL << position * 16);
    long insert = (long) b << position * 16;
    return l & mask | insert;
  }

}
