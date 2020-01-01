package net.torocraft.minecoprocessors.util;

import net.torocraft.minecoprocessors.util.GuiUtil;
import org.junit.Assert;
import org.junit.Test;

public class GuiUtilTest
{
  @Test
  public void toBinary() {
    Assert.assertEquals("00000000", GuiUtil.toBinary((byte) 0x0));
    Assert.assertEquals("11110000", GuiUtil.toBinary((byte) 0xf0));
    Assert.assertEquals("00001111", GuiUtil.toBinary((byte) 0x0f));
    Assert.assertEquals("0F", GuiUtil.toHex((byte) 0x0f));
    Assert.assertEquals("F0", GuiUtil.toHex((byte) 0xf0));
    Assert.assertEquals("0000", GuiUtil.toHex((short) 0x0));
    Assert.assertEquals("000F", GuiUtil.toHex((short) 0xf));
    Assert.assertEquals("FFFF", GuiUtil.toHex((short) -1));
  }
}
