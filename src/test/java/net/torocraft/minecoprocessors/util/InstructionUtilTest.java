package net.torocraft.minecoprocessors.util;

import java.util.ArrayList;
import java.util.List;
import mockit.Deencapsulation;
import net.torocraft.minecoprocessors.gui.GuiMinecoprocessor;
import net.torocraft.minecoprocessors.processor.InstructionCode;
import net.torocraft.minecoprocessors.processor.Register;
import org.junit.Assert;
import org.junit.Test;

public class InstructionUtilTest {

  @Test
  public void isRegister() {
    Assert.assertTrue(InstructionUtil.isRegister("a"));
    Assert.assertTrue(InstructionUtil.isRegister("B"));
    Assert.assertTrue(InstructionUtil.isRegister("PORTs"));
    Assert.assertTrue(InstructionUtil.isRegister("pf"));
    Assert.assertFalse(InstructionUtil.isRegister("15"));
    Assert.assertFalse(InstructionUtil.isRegister("-15"));
    Assert.assertFalse(InstructionUtil.isRegister("0xf0"));
    Assert.assertFalse(InstructionUtil.isRegister("010b"));
    Assert.assertFalse(InstructionUtil.isRegister("0o010"));
    Assert.assertFalse(InstructionUtil.isRegister(null));
  }

  @Test
  public void isLiteral() {
    Assert.assertTrue(InstructionUtil.isLiteral("15"));
    Assert.assertTrue(InstructionUtil.isLiteral("-15"));
    Assert.assertTrue(InstructionUtil.isLiteral("0xf0"));
    Assert.assertTrue(InstructionUtil.isLiteral("010b"));
    Assert.assertTrue(InstructionUtil.isLiteral("0o010"));
    // TODO detect over sized operands
    // Assert.assertFalse(InstructionUtil.isLiteral("0xf00"));
    Assert.assertFalse(InstructionUtil.isLiteral("a"));
    Assert.assertFalse(InstructionUtil.isLiteral("015b"));
    Assert.assertFalse(InstructionUtil.isLiteral("0xg3"));
    Assert.assertFalse(InstructionUtil.isLiteral("label_name"));
    Assert.assertFalse(InstructionUtil.isLiteral("a"));
  }

  @Test
  public void splitDoubleOperandString() {
    List<String> l = InstructionUtil.splitDoubleOperandString("test a, b");
    Assert.assertEquals(2, l.size());
    Assert.assertEquals("a", l.get(0));
    Assert.assertEquals("b", l.get(1));
  }

  @Test
  public void splitDoubleOperandString_literals() {
    List<String> l = InstructionUtil.splitDoubleOperandString("test a, 0xff");
    Assert.assertEquals(2, l.size());
    Assert.assertEquals("a", l.get(0));
    Assert.assertEquals("0xff", l.get(1));
  }

  @Test
  public void splitDoubleOperandString_pointer() {
    List<String> l = InstructionUtil.splitDoubleOperandString("test [a], [0xff]");
    Assert.assertEquals(2, l.size());
    Assert.assertEquals("[a]", l.get(0));
    Assert.assertEquals("[0xff]", l.get(1));
  }

  @Test
  public void splitDoubleOperandString_label() {
    List<String> l = InstructionUtil.splitDoubleOperandString("test a, test_label-op");
    Assert.assertEquals(2, l.size());
    Assert.assertEquals("a", l.get(0));
    Assert.assertEquals("test_label-op", l.get(1));
  }

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
    byte[] instruction;
    List<Label> labels = new ArrayList<>();
    labels.add(new Label((short) 13, "TEST_LABEL1"));
    labels.add(new Label((short) 14, "TEST_LABEL2"));
    labels.add(new Label((short) 15, "TEST_LABEL3"));

    instruction = InstructionUtil.parseLine("mov A, B ; test mov", labels, (short) 0);
    Assert.assertEquals((byte) InstructionCode.MOV.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) Register.B.ordinal(), instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("moV \t D  \t , d", labels, (short) 0);
    Assert.assertEquals((byte) InstructionCode.MOV.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.D.ordinal(), instruction[1]);
    Assert.assertEquals((byte) Register.D.ordinal(), instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("add a,b", labels, (short) 0);
    Assert.assertEquals((byte) InstructionCode.ADD.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) Register.B.ordinal(), instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);

    instruction = InstructionUtil.parseLine("Sub a,25", labels, (short) 0);
    Assert.assertEquals((byte) InstructionCode.SUB.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.A.ordinal(), instruction[1]);
    Assert.assertEquals((byte) 25, instruction[2]);
    Assert.assertEquals((byte) 0b00010000, instruction[3]);

    instruction = InstructionUtil.parseLine("DJNZ d, TEST_LABEL2", labels, (short) 0);
    Assert.assertEquals((byte) InstructionCode.DJNZ.ordinal(), instruction[0]);
    Assert.assertEquals((byte) Register.D.ordinal(), instruction[1]);
    Assert.assertEquals((byte) (byte) 1, instruction[2]);
    Assert.assertEquals((byte) 0b00000000, instruction[3]);
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
    testParseCompile("mov A, pr ; test mov", "mov a, pr");
    testParseCompile("mov A, 36 ; test mov", "mov a, 36");
    testParseCompile("push a ; test single op", "push a");
    testParseCompile("push 89 ; test single op", "push 89");
    testParseCompile("push 89", "test:\npush 89", (short) 56);
    testParseCompile("jmp test", "jmp test");
    testParseCompile("ret", "ret");
    testParseCompile("Jc   test", "jc test");
    testParseCompile("djnz A,test", "djnz a, test");
    testParseCompile("jnc test ", "jnc test");
    testParseCompile("ROR a, 5", "ror a, 5");
    testParseCompile("ROL pf, 1 ", "rol pf, 1");
    testParseCompile("SAL a, 5", "sal a, 5");
    testParseCompile("SAR pf, 1 ", "sar pf, 1");
    testParseCompile("HLT ", "hlt");
    testParseCompile("CLZ ", "clz");
    testParseCompile("CLC ", "clc");
    testParseCompile("SEZ ", "sez");
    testParseCompile("SEC ", "sec");
  }

  private void testParseCompile(String in, String out) throws ParseException {
    testParseCompile(in, out, (short) 0);
  }

  private void testParseCompile(String in, String out, short address) throws ParseException {
    List<Label> labels = new ArrayList<>();
    labels.add(new Label((short) 56, "test"));
    byte[] instruction = InstructionUtil.parseLine(in, labels, (short) 0);
    Assert.assertEquals(out, InstructionUtil.compileLine(instruction, labels, address));
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

  @Test
  public void isMemoryPointer() {
    Assert.assertFalse(InstructionUtil.isMemoryPointer("0xff"));
    Assert.assertFalse(InstructionUtil.isMemoryPointer("0x[f]f"));
    Assert.assertTrue(InstructionUtil.isMemoryPointer("[0xff]"));
    Assert.assertTrue(InstructionUtil.isMemoryPointer("[a]"));
    Assert.assertTrue(InstructionUtil.isMemoryPointer("[label_name]"));
    Assert.assertTrue(InstructionUtil.isMemoryPointer("[-234]"));
  }

  @Test
  public void stripMemoryPointerBrackets() {
    Assert.assertEquals("foo", InstructionUtil.stripMemoryPointerBrackets("foo"));
    Assert.assertEquals("foo", InstructionUtil.stripMemoryPointerBrackets("[foo]"));
    Assert.assertEquals("f[o]o", InstructionUtil.stripMemoryPointerBrackets("[f[o]o]"));
  }

  @Test
  public void parseVariableOperand() throws ParseException {
    List<Label> labels = new ArrayList<>();
    byte[] instruction;

    instruction = InstructionUtil.parseVariableOperand("", new byte[4], "0xfe", 0, labels);
    Assert.assertEquals((byte) 0, instruction[0]);
    Assert.assertEquals((byte) 0xfe, instruction[1]);
    Assert.assertEquals((byte) 0, instruction[2]);
    Assert.assertEquals((byte) 1, instruction[3]);

    instruction = InstructionUtil.parseVariableOperand("", new byte[4], "b", 1, labels);
    Assert.assertEquals((byte) 0, instruction[0]);
    Assert.assertEquals((byte) 0, instruction[1]);
    Assert.assertEquals((byte) 1, instruction[2]);
    Assert.assertEquals((byte) 0, instruction[3]);

    instruction = InstructionUtil.parseVariableOperand("", new byte[4], "011b", 1, labels);
    Assert.assertEquals((byte) 0, instruction[0]);
    Assert.assertEquals((byte) 0, instruction[1]);
    Assert.assertEquals((byte) 3, instruction[2]);
    Assert.assertEquals((byte) 0b00010000, instruction[3]);

    instruction = InstructionUtil.parseVariableOperand("", new byte[4], "[0xff]", 1, labels);
    Assert.assertEquals(4, instruction.length);
    Assert.assertEquals((byte) 0, instruction[0]);
    Assert.assertEquals((byte) 0, instruction[1]);
    Assert.assertEquals((byte) 0xff, instruction[2]);
    Assert.assertEquals((byte) 0b10010000, instruction[3]);
  }

  private static void printInstruction(byte[] instruction) {
    System.out.print("Instruction: ");
    for (byte b : instruction) {
      System.out.print(GuiMinecoprocessor.toBinary(b));
      System.out.print("  ");
    }
    System.out.println();
  }

  /**
   * Instruction Format:
   *
   * INST | OP1 | OP2 | OP_TYPES | OFFSET
   *
   * <ul>
   * <li><b>byte0:</b> instruction ID (ordinal of InstructionCode enum)</li>
   * <li><b>byte1:</b> first operand value</li>
   * <li><b>byte2:</b> second operand value</li>
   * <li><b>byte3_nibble0:</b> operand type</li>
   * <li><b>byte3_nibble1:</b> operand type</li>
   * <li><b>byte4:</b> offset value</li>
   * </ul>
   *
   * Operand Types:
   *
   * <ul>
   * <li><b>0:</b> Register ID (ordinal value of the Register enum)</li>
   * <li><b>1:</b> Literal Value</li>
   * <li><b>2:</b> Label</li>
   * <li><b>bit 2</b> Has Offset</li>
   * <li><b>bit 3:</b> Is Memory Pointer</li>
   * </ul>
   */
  @Test
  public void parseDoubleOperands() throws ParseException {
    List<Label> labels = new ArrayList<>();
    byte[] instruction;
    instruction = InstructionUtil.parseDoubleOperands("mov a, b", labels);
    Assert.assertEquals(4, instruction.length);
    Assert.assertEquals((byte) InstructionCode.MOV.ordinal(), instruction[0]);
    Assert.assertEquals((byte) 0, instruction[1]);
    Assert.assertEquals((byte) 1, instruction[2]);
    Assert.assertEquals((byte) 0, instruction[3]);
  }

  @Test
  public void parseDoubleOperands_doublePointer() throws ParseException {
    List<Label> labels = new ArrayList<>();
    ParseException e = null;
    try {
      InstructionUtil.parseDoubleOperands("mov [a], [b]", labels);
    } catch (ParseException ex) {
      e = ex;
    }
    Assert.assertNotNull(e);
    Assert.assertEquals(InstructionUtil.ERROR_DOUBLE_POINTER, e.message);
  }
}