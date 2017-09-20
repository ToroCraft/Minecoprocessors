package net.torocraft.minecoprocessors.blocks;

import org.junit.Assert;
import org.junit.Test;

public class TileEntityMinecoprocessorTest {

  @Test
  public void testNameParser() {
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(""));
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(" "));
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader("asdf"));
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(";  \n foo code \n \n"));
    Assert.assertEquals("test title", TileEntityMinecoprocessor.readNameFromHeader("; test title \n foo code \n \n"));

    //TODO fix test or code
    //Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(null));
  }
}