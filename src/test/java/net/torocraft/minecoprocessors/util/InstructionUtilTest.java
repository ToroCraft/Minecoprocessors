package net.torocraft.minecoprocessors.util;

import java.util.ArrayList;
import java.util.List;
import mockit.Deencapsulation;
import net.torocraft.minecoprocessors.processor.InstructionCode;
import net.torocraft.minecoprocessors.processor.Register;
import org.junit.Assert;
import org.junit.Test;

public class InstructionUtilTest {

  // TODO update to JUnit asserts 
  @Test
  public void testIsLiteral() {
    testIsNotLiteral("abc");
    testIsNotLiteral("-0x0aF");
    testIsNotLiteral("0o18");
    testIsNotLiteral("1012b");
    testIsLiteral("15");
    testIsLiteral("-15");
    testIsLiteral("0x15");
    testIsLiteral("0o15");
    testIsLiteral("0o10");
    testIsLiteral("0x00015");
    testIsLiteral("0x0aF");
    testIsLiteral("101b");
    testIsLiteral("015d");
  }

  private static void testIsLiteral(String s) {
    Assert.assertTrue(Deencapsulation.invoke(InstructionUtil.class, "isLiteral", s));
  }
  private static void testIsNotLiteral(String s) {
    Assert.assertFalse(Deencapsulation.invoke(InstructionUtil.class, "isLiteral", s));
  }

  // TODO update to JUnit asserts 
  @Test
  public void testParseLiteral() {
//    try {
//      assert InstructionUtil.parseLiteral(null, " -120 ") == (byte) -120;
//      assert InstructionUtil.parseLiteral(null, " 120d") == (byte) 120;
//      assert InstructionUtil.parseLiteral(null, " -15 ") == (byte) -15;
//      assert InstructionUtil.parseLiteral(null, " 0o10 ") == (byte) 8;
//      assert InstructionUtil.parseLiteral(null, "0x0ff") == (byte) 0x0ff;
//      assert InstructionUtil.parseLiteral(null, "1111b") == (byte) 0x0f;
//      assert InstructionUtil.parseLiteral(null, "10000000b") == (byte) 0b10000000;
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testParseLiteralOversized() {
//    ParseException e = null;
//    try {
//      InstructionUtil.parseLiteral(null, "100000000b");
//    } catch (ParseException t) {
//      e = t;
//    }
//    assert e != null;
  }

  // TODO update to JUnit asserts 
  @Test
  public void testRemoveLabels() {
//    assert InstructionUtil.removeLabels("testfoo: mov a, 5 ; and comment; ignore me")
//        .equals("mov a, 5 ; and comment; ignore me");
//    assert InstructionUtil.removeLabels("testfoo:").equals("");
//    assert InstructionUtil.removeLabels(" testfoo: ").equals("");
  }

  // TODO update to JUnit asserts 
  @Test
  public void testRemoveComments() {
    try {
//      assert InstructionUtil.removeComments("test foo ; and comment; ignore me").equals("test foo");
//      assert InstructionUtil.removeComments(" ; and comment; ignore me").equals("");
      assert InstructionUtil.parseLine("  ;loop \t test_label  \t ", null, (short) 0) == null;
      assert InstructionUtil.parseLine(";loop", null, (short) 0) == null;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testLabelOperands() {
    try {
      List<Label> labels = new ArrayList<>();
      labels.add(new Label((short) 13, "TEST_LABEL"));
      byte[] instruction = InstructionUtil.parseLine("loop \t test_label  \t ", labels, (short) 0);
      assert instruction[0] == (byte) InstructionCode.LOOP.ordinal();
      assert instruction[1] == (byte) 0;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testStandardOperands() {
    try {
      byte[] instruction = InstructionUtil.parseLine("mov A, B ; test mov", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.MOV.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[2] == (byte) Register.B.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = InstructionUtil.parseLine("moV \t D  \t , d", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.MOV.ordinal();
      assert instruction[1] == (byte) Register.D.ordinal();
      assert instruction[2] == (byte) Register.D.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = InstructionUtil.parseLine("add a,b", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.ADD.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[2] == (byte) Register.B.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = InstructionUtil.parseLine("Sub a,25", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.SUB.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[2] == (byte) 25;
      assert instruction[3] == (byte) 0b00010000;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testSingleOperands() {
    try {
      byte[] instruction = InstructionUtil.parseLine("push A", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.PUSH.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = InstructionUtil.parseLine("push 35", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.PUSH.ordinal();
      assert instruction[1] == (byte) 35;
      assert instruction[3] == (byte) 0b00000001;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testNoOperands() {
    try {
      byte[] instruction = InstructionUtil.parseLine("nop", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.NOP.ordinal();
      instruction = InstructionUtil.parseLine("ret  \t", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.RET.ordinal();
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testCompileLine() {
    try {
      List<Label> labels = new ArrayList<>();
      labels.add(new Label((short) 56, "test"));

      byte[] instruction = InstructionUtil.parseLine("mov A, pr ; test mov", labels, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 0).equals("mov a, pr");

      instruction = InstructionUtil.parseLine("mov A, 36 ; test mov", labels, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 0).equals("mov a, 36");

      instruction = InstructionUtil.parseLine("push a ; test single op", labels, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 0).equals("push a");

      instruction = InstructionUtil.parseLine("push 89 ; test single op", null, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 0).equals("push 89");

      instruction = InstructionUtil.parseLine("push 89", labels, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 56).equals("test:\npush 89");

      instruction = InstructionUtil.parseLine("jmp test", labels, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 0).equals("jmp test");

      instruction = InstructionUtil.parseLine("ret", labels, (short) 0);
      assert InstructionUtil.compileLine(instruction, labels, (short) 0).equals("ret");

    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  // TODO update to JUnit asserts 
  @Test
  public void testParseFile() {
    try {
      String file = "";
      file += ";test program\n";
      file += "cmp a, b\n";
      file += "jmp end\n";
      file += "test:mov a, b\n";
      file += "test1: add a, 50\n";
      file += "test2:  loop test\n";
      file += "end:\n";
      file += "jmp test\n";
      List<Label> labels = new ArrayList<>();

      List<byte[]> instructions = InstructionUtil.parseFile(file, labels);

      assert instructions.size() == 6;
      assert labels.size() == 4;
      assert labels.get(0).name.equals("test");
      assert labels.get(0).address == (short) 2;

      assert labels.get(1).name.equals("test1");
      assert labels.get(1).address == (short) 3;

      assert labels.get(2).name.equals("test2");
      assert labels.get(2).address == (short) 4;

      assert InstructionCode.values()[instructions.get(0)[0]]
          .equals(InstructionCode.CMP);
      assert InstructionCode.values()[instructions.get(4)[0]]
          .equals(InstructionCode.LOOP);

      String reCompiled = InstructionUtil.compileFile(instructions, labels);

      String expected = "";
      expected += "cmp a, b\n";
      expected += "jmp end\n";
      expected += "test:\n";
      expected += "mov a, b\n";
      expected += "test1:\n";
      expected += "add a, 50\n";
      expected += "test2:\n";
      expected += "loop test\n";
      expected += "end:\n";
      expected += "jmp test";

      assert reCompiled.equals(expected);

    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }
}