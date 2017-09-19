package net.torocraft.minecoprocessors.util;

import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
import net.torocraft.minecoprocessors.processor.Processor;

/**
 * for Java assertions to work the this VM argument is required: -ea
 */

@SuppressWarnings("unused")
public class TestRunner {

  public static void main(String[] args) {
    ByteUtil.test();
    InstructionUtil.test();
    testProcessor();
    BlockMinecoprocessor.test();
    //runProcessor();
    runFaultRet();
    testNameParser();
    System.out.println("pass!");
  }

  private static void testNameParser() {
    assert "test title".equals(TileEntityMinecoprocessor.readNameFromHeader("; test title \n foo code \n \n"));
    assert TileEntityMinecoprocessor.readNameFromHeader(";  \n foo code \n \n") == null;
    assert TileEntityMinecoprocessor.readNameFromHeader(null) == null;
    assert TileEntityMinecoprocessor.readNameFromHeader("") == null;
    assert TileEntityMinecoprocessor.readNameFromHeader(" ") == null;
    assert TileEntityMinecoprocessor.readNameFromHeader("asdf") == null;
  }

  private static void testProcessor() {
    new Processor().test();
  }

  private static void runProcessor() {
    Processor p = new Processor();

    String program = "";
    program += "mov c, 10 \n";
    program += "start: \n";
    program += "sub c, 1 \n";
    program += "jnz start \n";
    program += "mov e, 100 \n";
    p.load(program);

    for (int i = 0; i < 100; i++) {

      p.tick();

      if (p.isFault()) {
        break;
      }
    }

    System.out.println(p.pinchDump());

  }

  private static void runFaultRet() {
    Processor p = new Processor();

    String program = "";
    program += "mov c, 10 \n";
    program += "start: \n";
    program += "sub c, 1 \n";
    program += "jnz start \n";
    program += "ret \n";
    p.load(program);

    for (int i = 0; i < 100; i++) {
      p.tick();
      if (p.isFault()) {
        break;
      }
    }

    assert p.isFault();
  }
}
