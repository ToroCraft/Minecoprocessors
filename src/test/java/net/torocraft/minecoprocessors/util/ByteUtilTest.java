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
    byte b = (byte) 0xfe;
    Assert.assertTrue(ByteUtil.getBit(b, 7));
    Assert.assertTrue(ByteUtil.getBit(b, 1));
    Assert.assertFalse(ByteUtil.getBit(b, 0));

    b = (byte) 0x7e;
    Assert.assertFalse(ByteUtil.getBit(b, 7));
  }

  @Test
  public void testGetByte() {
    short s = (short) 0xabcd;
    Assert.assertEquals((byte) 0xcd, ByteUtil.getByte(s, 0));
    Assert.assertEquals((byte) 0xab, ByteUtil.getByte(s, 1));
  }

  @Test
  public void testSetByte() {
    short s = (short) 0x0f00;
    Assert.assertEquals((short) 0x0fab, ByteUtil.setByte(s, (byte) 0xab, 0));
    Assert.assertEquals((short) 0x6900, ByteUtil.setByte(s, (byte) 0x69, 1));
  }

  @Test
  public void testGetShort() {
    long l = Long.parseLong("0123456789abcdef", 16);
    Assert.assertEquals((short) 0xcdef, ByteUtil.getShort(l, 0));
    Assert.assertEquals((short) 0x89ab, ByteUtil.getShort(l, 1));
    Assert.assertEquals((short) 0x4567, ByteUtil.getShort(l, 2));
    Assert.assertEquals((short) 0x0123, ByteUtil.getShort(l, 3));
  }

  @Test
  public void testSetShort() {
    long l = Long.parseLong("0123456789abcdef", 16);
    l = ByteUtil.setShort(l, (short) 0x0ffff, 1);
    Assert.assertEquals("1234567ffffcdef", Long.toString(l, 16));

    l = ByteUtil.setShort(l, (short) 0x01111, 3);
    Assert.assertEquals("11114567ffffcdef", Long.toString(l, 16));

    l = ByteUtil.setShort(l, (short) 0x0aaaa, 2);
    Assert.assertEquals("1111aaaaffffcdef", Long.toString(l, 16));

    l = ByteUtil.setShort(l, (short) 0x09999, 0);
    Assert.assertEquals("1111aaaaffff9999", Long.toString(l, 16));
  }

  @Test
  public void testByteInIntMethods() {
    int i = 0x12345678;
    Assert.assertEquals(0x78, ByteUtil.getByte(i, 0));
    Assert.assertEquals(0x56, ByteUtil.getByte(i, 1));
    Assert.assertEquals(0x34, ByteUtil.getByte(i, 2));
    Assert.assertEquals(0x12, ByteUtil.getByte(i, 3));

    i = ByteUtil.setByte(i, (byte) 0xee, 1);
    Assert.assertEquals(0x1234ee78, i);

    i = ByteUtil.setByte(i, (byte) 0xff, 3);
    Assert.assertEquals(0xff34ee78, i);

    i = ByteUtil.setByte(i, (byte) 0xcc, 2);
    Assert.assertEquals(0xffccee78, i);

    i = ByteUtil.setByte(i, (byte) 0xaa, 0);
    Assert.assertEquals(0xffcceeaa, i);
  }

  @Test
  public void testByteInLongMethods() {
    long l = Long.parseLong("0123456789abcdef", 16);
    Assert.assertEquals((byte) 0xef, ByteUtil.getByte(l, 0));
    Assert.assertEquals((byte) 0xcd, ByteUtil.getByte(l, 1));
    Assert.assertEquals((byte) 0xab, ByteUtil.getByte(l, 2));
    Assert.assertEquals((byte) 0x89, ByteUtil.getByte(l, 3));
    Assert.assertEquals((byte) 0x01, ByteUtil.getByte(l, 7));

    l = ByteUtil.setByte(l, (byte) 0x33, 1);
    Assert.assertEquals("123456789ab33ef", Long.toString(l, 16));

    l = ByteUtil.setByte(l, (byte) 0xee, 4);
    Assert.assertEquals("12345ee89ab33ef", Long.toString(l, 16));

    l = ByteUtil.setByte(l, (byte) 0xff, 7);
    Assert.assertEquals("ff2345ee89ab33ef", Long.toUnsignedString(l, 16));
  }

  @Test
  public void testBitMethods() {
    long l = Long.parseLong("0110010", 2);

    Assert.assertFalse(ByteUtil.getBit(l, 0));
    Assert.assertTrue(ByteUtil.getBit(l, 1));
    Assert.assertFalse(ByteUtil.getBit(l, 2));
    Assert.assertFalse(ByteUtil.getBit(l, 3));
    Assert.assertTrue(ByteUtil.getBit(l, 4));
    Assert.assertTrue(ByteUtil.getBit(l, 5));
    Assert.assertFalse(ByteUtil.getBit(l, 6));
    Assert.assertFalse(ByteUtil.getBit(l, 7));

    l = ByteUtil.setBit(l, true, 0);
    Assert.assertEquals("110011", Long.toBinaryString(l));

    l = ByteUtil.setBit(l, true, 2);
    Assert.assertEquals("110111", Long.toBinaryString(l));

    l = ByteUtil.setBit(l, false, 0);
    l = ByteUtil.setBit(l, false, 2);
    Assert.assertEquals("110010", Long.toBinaryString(l));
  }
}