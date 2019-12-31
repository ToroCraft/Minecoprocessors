package net.torocraft.minecoprocessors.util;

import mockit.Deencapsulation;
import net.minecraft.util.Direction;
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
    Assert.assertEquals(Direction.NORTH, RedstoneUtil.convertPortIndexToFacing(Direction.NORTH, f));
    Assert.assertEquals(Direction.EAST, RedstoneUtil.convertPortIndexToFacing(Direction.NORTH, r));
    Assert.assertEquals(Direction.SOUTH, RedstoneUtil.convertPortIndexToFacing(Direction.NORTH, b));
    Assert.assertEquals(Direction.EAST, RedstoneUtil.convertPortIndexToFacing(Direction.EAST, f));
    Assert.assertEquals(Direction.WEST, RedstoneUtil.convertPortIndexToFacing(Direction.EAST, b));
    Assert.assertEquals(Direction.NORTH, RedstoneUtil.convertPortIndexToFacing(Direction.EAST, l));
    Assert.assertEquals(Direction.NORTH, RedstoneUtil.convertPortIndexToFacing(Direction.SOUTH, b));
    Assert.assertEquals(Direction.NORTH, RedstoneUtil.convertPortIndexToFacing(Direction.WEST, r));
  }

  @Test
  public void testConvertFacingToPortIndex() {
    int f = 0;
    int b = 1;
    int l = 2;
    int r = 3;
    Assert.assertEquals(f, RedstoneUtil.convertFacingToPortIndex(Direction.NORTH, Direction.NORTH));
    Assert.assertEquals(b, RedstoneUtil.convertFacingToPortIndex(Direction.NORTH, Direction.SOUTH));
    Assert.assertEquals(r, RedstoneUtil.convertFacingToPortIndex(Direction.EAST, Direction.SOUTH));
    Assert.assertEquals(r, RedstoneUtil.convertFacingToPortIndex(Direction.WEST, Direction.NORTH));
    Assert.assertEquals(l, RedstoneUtil.convertFacingToPortIndex(Direction.SOUTH, Direction.EAST));
  }

  @Test
  public void testRotateFacing() {
    Assert.assertEquals(Direction.EAST, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", Direction.NORTH, -3));
    Assert.assertEquals(Direction.NORTH, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", Direction.NORTH, 0));
    Assert.assertEquals(Direction.EAST, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", Direction.EAST, 0));
    Assert.assertEquals(Direction.EAST, Deencapsulation.invoke(RedstoneUtil.class, "rotateFacing", Direction.WEST, -2));
  }
}