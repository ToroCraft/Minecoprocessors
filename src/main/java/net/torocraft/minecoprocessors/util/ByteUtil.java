package net.torocraft.minecoprocessors.util;

public class ByteUtil {

  public static boolean getBit(byte b, int position) {
    return ((b >> position) & 1) != 0;
  }

  public static byte setBit(byte b, boolean bit, int position) {
    if (bit) {
      return (byte) (b | (1 << position));
    }
    return (byte) (b & ~(1 << position));
  }

  public static boolean getBitInLong(long l, int position) {
    return ((l >> position) & 1) != 0;
  }

  public static long setBitInLong(long l, boolean bit, int position) {
    if (bit) {
      return l | (1 << position);
    }
    return l & ~(1 << position);
  }

  public static byte getByte(int i, int position) {
    return (byte) ((i >> (8 * position)) & 0xff);
  }

  public static int setByte(int i, byte b, int position) {
    if (position > 3) {
      throw new IndexOutOfBoundsException("byte position of " + position);
    }
    int mask = ~(0xff << position * 8);
    i = (mask & i);
    int insert = (int) (b << position * 8) & ~mask;
    return i | insert;
  }

  public static byte getByteInShort(short s, int position) {
    return (byte) ((s >> (8 * position)) & 0xff);
  }

  public static short setByteInShort(short s, byte b, int position) {
    if (position > 7) {
      throw new IndexOutOfBoundsException("position of " + position);
    }
    long mask = ~(Long.parseLong("ff", 16) << position * 8);
    s = (short) (mask & s);
    long insert = ((long) b << position * 8) & ~mask;
    return (short) (s | insert);
  }

  public static byte getByteInLong(long l, int position) {
    return (byte) ((l >> (8 * position)) & 0xff);
  }

  public static long setByteInLong(long l, byte b, int position) {
    if (position > 7) {
      throw new IndexOutOfBoundsException("position of " + position);
    }
    long mask = ~(Long.parseLong("ff", 16) << position * 8);
    l = (mask & l);
    long insert = ((long) b << position * 8) & ~mask;
    return l | insert;
  }

  public static short getShort(long i, int position) {
    return (short) ((i >> (16 * position)) & 0xffff);
  }

  public static long setShort(long i, short b, int position) {
    if (position > 3) {
      throw new IndexOutOfBoundsException("short position of " + position);
    }
    long mask = ~(Long.parseLong("ffff", 16) << (position * 16));
    i = (mask & i);
    long insert = (long) ((long) b << position * 16) & ~mask;
    return i | insert;
  }

}
