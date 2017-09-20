package net.torocraft.minecoprocessors.util;

import java.util.ArrayList;
import java.util.List;
import mockit.Deencapsulation;
import net.torocraft.minecoprocessors.processor.InstructionCode;
import net.torocraft.minecoprocessors.processor.Register;
import org.junit.Assert;
import org.junit.Test;

public class InstructionUtilTest {

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

  @Test
  public void testParseLiteral() throws ParseException {
    Assert.assertEquals((byte) -120, InstructionUtil.parseLiteral(null, " -120 "));
    Assert.assertEquals((byte) 120, InstructionUtil.parseLiteral(null, " 120d"));
    Assert.assertEquals((byte) -15, InstructionUtil.parseLiteral(null, " -15 "));
    Assert.assertEquals((byte) 8, InstructionUtil.parseLiteral(null, " 0o10 "));
    Assert.assertEquals((byte) 0x0ff, InstructionUtil.parseLiteral(null, "0x0ff"));
    Assert.assertEquals((byte) 0x0f, InstructionUtil.parseLiteral(null, "1111b"));
    Assert.assertEquals((byte) 0b10000000, InstructionUtil.parseLiteral(null, "10000000b"));
  }

  @Test
  public void testParseLiteralOverSized() {
    ParseException e = null;
    try {
      InstructionUtil.parseLiteral(null, "100000000b");
    } catch (ParseException t) {
      e = t;
    }
    Assert.assertNotNull(e);
  }

  @Test
  public void testRemoveLabels() {
    Assert.assertEquals("mov a, 5 ; and comment; ignore me", InstructionUtil.removeLabels("testfoo: mov a, 5 ; and comment; ignore me"));
    Assert.assertEquals("", InstructionUtil.removeLabels("testfoo:"));
    Assert.assertEquals("", InstructionUtil.removeLabels(" testfoo: "));
  }

  @Test
  public void testRemoveComments() throws ParseException {
    Assert.assertEquals("test foo", InstructionUtil.removeComments("test foo ; and comment; ignore me"));
    Assert.assertEquals("", InstructionUtil.removeComments(" ; and comment; ignore me"));
    Assert.assertNull(InstructionUtil.parseLine("  ;loop \t test_label  \t ", null, (short) 0));
    Assert.assertNull(InstructionUtil.parseLine(";loop", null, (short) 0));
  }

  @Test
  public void testLabelOperands() throws ParseException {
    List<Label> labels = new ArrayList<>();
    labels.add(new Label((short) 13, "TEST_LABEL"));
    byte[] instruction = InstructionUtil.parseLine("loop \t test_label  \t ", labels, (short) 0);

    Assert.assertEquals((byte) InstructionCode.LOOP.ordinal(), instruction[0]);
    Assert.assertEquals((byte) 0, instruction[1]);
  }

  @Test
  public void testStandardOperands() throws ParseException {
    byte[] instruction = InstructionUtil.parseLine("mov A, B ; test mov", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.MOV.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) Register.B.ordinal(), instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("moV \t D  \t , d", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.MOV.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.D.ordinal(), instruction[1]);
    Assert.assertEquals((byte) Register.D.ordinal(), instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("add a,b", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.ADD.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) Register.B.ordinal(), instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("Sub a,25", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.SUB.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) 25, instruction[2]);
    Assert.assertEquals((byte) 0b00010000, instruction[3]);
  }

  @Test
  public void testSingleOperands() throws ParseException {
    byte[] instruction = InstructionUtil.parseLine("push A", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.PUSH.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("push 35", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.PUSH.ordinal(), instruction[0]);
    Assert.assertEquals((byte) 35, instruction[1]);
    Assert.assertEquals((byte) 0b00000001, instruction[3]);
  }

  @Test
  public void testNoOperands() throws ParseException {
    byte[] instruction = InstructionUtil.parseLine("nop", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.NOP.ordinal(), instruction[0]);
    instruction = InstructionUtil.parseLine("ret  \t", null, (short) 0);
    Assert.assertEquals((byte) InstructionCode.RET.ordinal(), instruction[0]);
  }

  @Test
  public void testCompileLine() throws ParseException {
    List<Label> labels = new ArrayList<>();
    labels.add(new Label((short) 56, "test"));

    byte[] instruction = InstructionUtil.parseLine("mov A, pr ; test mov", labels, (short) 0);
    Assert.assertEquals("mov a, pr", InstructionUtil.compileLine(instruction, labels, (short) 0));

    instruction = InstructionUtil.parseLine("mov A, 36 ; test mov", labels, (short) 0);
    Assert.assertEquals("mov a, 36", InstructionUtil.compileLine(instruction, labels, (short) 0));

    instruction = InstructionUtil.parseLine("push a ; test single op", labels, (short) 0);
    Assert.assertEquals("push a", InstructionUtil.compileLine(instruction, labels, (short) 0));

    instruction = InstructionUtil.parseLine("push 89 ; test single op", null, (short) 0);
    Assert.assertEquals("push 89", InstructionUtil.compileLine(instruction, labels, (short) 0));

    instruction = InstructionUtil.parseLine("push 89", labels, (short) 0);
    Assert.assertEquals("test:\npush 89", InstructionUtil.compileLine(instruction, labels, (short) 56));

    instruction = InstructionUtil.parseLine("jmp test", labels, (short) 0);
    Assert.assertEquals("jmp test", InstructionUtil.compileLine(instruction, labels, (short) 0));

    instruction = InstructionUtil.parseLine("ret", labels, (short) 0);
    Assert.assertEquals("ret", InstructionUtil.compileLine(instruction, labels, (short) 0));
  }

  @Test
  public void testParseFile() throws ParseException {
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

    Assert.assertEquals(6, instructions.size());
    Assert.assertEquals(4, labels.size());
    Assert.assertEquals("test", labels.get(0).name);
    Assert.assertEquals((short) 2, labels.get(0).address);
    Assert.assertEquals("test1", labels.get(1).name);
    Assert.assertEquals((short) 3, labels.get(1).address);
    Assert.assertEquals("test2", labels.get(2).name);
    Assert.assertEquals((short) 4, labels.get(2).address);
    Assert.assertEquals(InstructionCode.CMP, InstructionCode.values()[instructions.get(0)[0]]);
    Assert.assertEquals(InstructionCode.LOOP, InstructionCode.values()[instructions.get(4)[0]]);

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

    Assert.assertEquals(expected, reCompiled);
  }
}