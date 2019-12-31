package net.torocraft.minecoprocessors.blocks;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class TileEntityMinecoprocessorTest {

  @Test
  public void testNameParser() {
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(Arrays.asList("")));
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(Arrays.asList(" ")));
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(Arrays.asList("asdf")));
    Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(Arrays.asList(";  ", " foo code ", " ", "")));
    Assert.assertEquals("test title", TileEntityMinecoprocessor.readNameFromHeader(Arrays.asList("; test title ", " foo code ", " ", "")));

    //TODO fix test or code
    //Assert.assertNull(TileEntityMinecoprocessor.readNameFromHeader(null));
  }
}