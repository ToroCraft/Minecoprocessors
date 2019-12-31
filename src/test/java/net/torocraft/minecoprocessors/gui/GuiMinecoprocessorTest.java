package net.torocraft.minecoprocessors.gui;

import org.junit.Assert;
import org.junit.Test;

public class GuiMinecoprocessorTest {

  @Test
  public void toBinary() {
    Assert.assertEquals("11110000", GuiMinecoprocessor.toBinary((byte) 0xf0));
    Assert.assertEquals("00000000", GuiMinecoprocessor.toBinary((byte) 0x0));
  }

}