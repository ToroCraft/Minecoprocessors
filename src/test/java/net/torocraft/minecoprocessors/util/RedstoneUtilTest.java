package net.torocraft.minecoprocessors.util;

import mockit.Deencapsulation;
import net.minecraft.util.EnumFacing;
import org.junit.Assert;
import org.junit.Test;

public class RedstoneUtilTest {

  @Test
  public void testPortToPower() {
    Assert.assertEquals(0, RedstoneUtil.portToPower((byte) 0x00));
    Assert.assertEquals(0, RedstoneUtil.portToPower((byte) 0xf0));
    Assert.assertEquals(1, RedstoneUtil.portToPower((byte) 0x11));
    Assert.assertEquals(1, RedstoneUtil.portToPower((byte) 0xf1));
    Assert.assertEquals(15, RedstoneUtil.portToPower((byte) 0xff));
    Assert.assertEquals(15, RedstoneUtil.portToPower((byte) 0x0f));
  }

  @Test
  public void testPowerToPort() {
    Assert.assertEquals((byte) 0x00, RedstoneUtil.powerToPort(0));
    Assert.assertEquals((byte) 0x01, RedstoneUtil.powerToPort(1));
    Assert.assertEquals((byte) 0x0e, RedstoneUtil.powerToPort(14));
    Assert.assertEquals((byte) 0x0f, RedstoneUtil.powerToPort(15));
  }

  @Test
  public void testConvertPortIndexToFacing() {
    int f = 0;
    int b = 1;
    int l = 2;
    int r = 3;
    Assert.assertEquals(EnumFacing.NORTH, RedstoneUtil.convertPortIndexToFacing(EnumFacing.NORTH, f));
    Assert.assertEquals(EnumFacing.EAST, RedstoneUtil.convertPortIndexToFacing(EnumFacing.NORTH, r));
    Assert.assertEquals(EnumFacing.SOUTH, RedstoneUtil.convertPortIndexToFacing(EnumFacing.NORTH, b));
    Assert.assertEquals(EnumFacing.EAST, RedstoneUtil.convertPortIndexToFacing(EnumFacing.EAST, f));
    Assert.assertEquals(EnumFacing.WEST, RedstoneUtil.convertPortIndexToFacing(EnumFacing.EAST, b));
    Assert.assertEquals(EnumFacing.NORTH, RedstoneUtil.convertPortIndexToFacing(EnumFacing.EAST, l));
    Assert.assertEquals(EnumFacing.NORTH, RedstoneUtil.convertPortIndexToFacing(EnumFacing.SOUTH, b));
    Assert.assertEquals(EnumFacing.NORTH, RedstoneUtil.convertPortIndexToFacing(EnumFacing.WEST, r));
  }

  @Test
  public void testConvertFacingToPortIndex() {
    int f = 0;
    int b = 1;
    int l = 2;
    int r = 3;
    Assert.assertEquals(f, RedstoneUtil.convertFacingToPortIndex(EnumFacing.NORTH, EnumFacing.NORTH));
    Assert.assertEquals(b, RedstoneUtil.convertFacingToPortIndex(EnumFacing.NORTH, EnumFacing.SOUTH));
    Assert.assertEquals(r, RedstoneUtil.convertFacingToPortIndex(EnumFacing.EAST, EnumFacing.SOUTH));
    Assert.assertEquals(r, RedstoneUtil.convertFacingToPortIndex(EnumFacing.WEST, EnumFacing.NORTH));
    Assert.assertEquals(l, RedstoneUtil.convertFacingToPortIndex(EnumFacing.SOUTH, EnumFacing.EAST));
  }

  @Test
  public void testRotateFacing() {
    Assert.assertEquals(EnumFacing.EAST, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", EnumFacing.NORTH, -3));
    Assert.assertEquals(EnumFacing.NORTH, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", EnumFacing.NORTH, 0));
    Assert.assertEquals(EnumFacing.EAST, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", EnumFacing.EAST, 0));
    Assert.assertEquals(EnumFacing.EAST, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", EnumFacing.WEST, -2));
  }
}