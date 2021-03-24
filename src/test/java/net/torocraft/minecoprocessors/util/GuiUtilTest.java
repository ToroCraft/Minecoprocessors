package net.torocraft.minecoprocessors.util;

import net.torocraft.minecoprocessors.util.StringUtil;
import org.junit.Assert;
import org.junit.Test;

public class GuiUtilTest
{
  @Test
  public void toBinary() {
    Assert.assertEquals("00000000", StringUtil.toBinary((byte) 0x0));
    Assert.assertEquals("11110000", StringUtil.toBinary((byte) 0xf0));
    Assert.assertEquals("00001111", StringUtil.toBinary((byte) 0x0f));
    Assert.assertEquals("0F", StringUtil.toHex((byte) 0x0f));
    Assert.assertEquals("F0", StringUtil.toHex((byte) 0xf0));
    Assert.assertEquals("0000", StringUtil.toHex((short) 0x0));
    Assert.assertEquals("000F", StringUtil.toHex((short) 0xf));
    Assert.assertEquals("FFFF", StringUtil.toHex((short) -1));
  }
}
