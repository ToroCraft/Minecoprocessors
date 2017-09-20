package net.torocraft.minecoprocessors.util;

import org.junit.Assert;
import org.junit.Test;

public class ByteUtilTest {

  @Test
  public void testSetBit() {
    Assert.assertEquals((byte) 0xfe, ByteUtil.setBit((byte) 0xff, false, 0));
    Assert.assertEquals((byte) 0x7e, ByteUtil.setBit((byte) 0xfe, false, 7));
    Assert.assertEquals((byte) 0xfe, ByteUtil.setBit((byte) 0x7e, true, 7));
    Assert.assertEquals((byte) 0xff, ByteUtil.setBit((byte) 0xfe, true, 0));
  }

  @Test
  public void testGetBit() {
    // TODO update to JUnit assert
    byte b = (byte) 0xfe;
    assert ByteUtil.getBit(b, 7);
    assert ByteUtil.getBit(b, 1);
    assert !ByteUtil.getBit(b, 0);

    b = (byte) 0x7e;
    assert !ByteUtil.getBit(b, 7);
  }

  @Test
  public void testGetByteInShort() {
    // TODO update to JUnit assert
    short s = (short) 0xabcd;
    assert ByteUtil.getByteInShort(s, 0) == (byte) 0xcd;
    assert ByteUtil.getByteInShort(s, 1) == (byte) 0xab;
  }

  @Test
  public void testSetByteInShort() {
    // TODO update to JUnit assert
    short s = (short) 0x0f00;
    assert ByteUtil.setByteInShort(s, (byte) 0xab, 0) == (short) 0x0fab;
    assert ByteUtil.setByteInShort(s, (byte) 0x69, 1) == (short) 0x6900;
  }

  @Test
  public void testGetShort() {
    // TODO update to JUnit assert
    long l = Long.parseLong("0123456789abcdef", 16);
    assert ByteUtil.getShort(l, 0) == (short) 0xcdef;
    assert ByteUtil.getShort(l, 1) == (short) 0x89ab;
    assert ByteUtil.getShort(l, 2) == (short) 0x4567;
    assert ByteUtil.getShort(l, 3) == (short) 0x0123;
  }

  @Test
  public void testSetShort() {
    // TODO update to JUnit assert
    long l = Long.parseLong("0123456789abcdef", 16);
    l = ByteUtil.setShort(l, (short) 0x0ffff, 1);
    assert Long.toString(l, 16).equals("1234567ffffcdef");

    l = ByteUtil.setShort(l, (short) 0x01111, 3);
    assert Long.toString(l, 16).equals("11114567ffffcdef");

    l = ByteUtil.setShort(l, (short) 0x0aaaa, 2);
    assert Long.toString(l, 16).equals("1111aaaaffffcdef");

    l = ByteUtil.setShort(l, (short) 0x09999, 0);
    assert Long.toString(l, 16).equals("1111aaaaffff9999");
  }

  @Test
  public void testByteMethods() {
    // TODO update to JUnit assert
    int i = 0x12345678;

    assert ByteUtil.getByte(i, 0) == 0x78;
    assert ByteUtil.getByte(i, 1) == 0x56;
    assert ByteUtil.getByte(i, 2) == 0x34;
    assert ByteUtil.getByte(i, 3) == 0x12;

    i = ByteUtil.setByte(i, (byte) 0xee, 1);
    assert i == 0x1234ee78;

    i = ByteUtil.setByte(i, (byte) 0xff, 3);
    assert i == 0xff34ee78;

    i = ByteUtil.setByte(i, (byte) 0xcc, 2);
    assert i == 0xffccee78;

    i = ByteUtil.setByte(i, (byte) 0xaa, 0);
    assert i == 0xffcceeaa;
  }

  @Test
  public void testByteInLongMethods() {
    // TODO update to JUnit assert
    long l = Long.parseLong("0123456789abcdef", 16);

    assert ByteUtil.getByteInLong(l, 0) == (byte) 0xef;
    assert ByteUtil.getByteInLong(l, 1) == (byte) 0xcd;
    assert ByteUtil.getByteInLong(l, 2) == (byte) 0xab;
    assert ByteUtil.getByteInLong(l, 3) == (byte) 0x89;
    assert ByteUtil.getByteInLong(l, 7) == (byte) 0x01;

    l = ByteUtil.setByteInLong(l, (byte) 0x33, 1);
    assert Long.toString(l, 16).equals("123456789ab33ef");

    l = ByteUtil.setByteInLong(l, (byte) 0xee, 4);
    assert Long.toString(l, 16).equals("12345ee89ab33ef");

    l = ByteUtil.setByteInLong(l, (byte) 0xff, 7);
    assert Long.toUnsignedString(l, 16).equals("ff2345ee89ab33ef");
  }

  @Test
  public void testBitInLongMethods() {
    // TODO update to JUnit assert
    long l = Long.parseLong("0110010", 2);

    assert !ByteUtil.getBitInLong(l, 0);
    assert ByteUtil.getBitInLong(l, 1);
    assert !ByteUtil.getBitInLong(l, 2);
    assert !ByteUtil.getBitInLong(l, 3);
    assert ByteUtil.getBitInLong(l, 4);
    assert ByteUtil.getBitInLong(l, 5);
    assert !ByteUtil.getBitInLong(l, 6);
    assert !ByteUtil.getBitInLong(l, 7);

    l = ByteUtil.setBitInLong(l, true, 0);
    assert Long.toBinaryString(l).equals("110011");

    l = ByteUtil.setBitInLong(l, true, 2);
    assert Long.toBinaryString(l).equals("110111");

    l = ByteUtil.setBitInLong(l, false, 0);
    l = ByteUtil.setBitInLong(l, false, 2);
    assert Long.toBinaryString(l).equals("110010");
  }
}