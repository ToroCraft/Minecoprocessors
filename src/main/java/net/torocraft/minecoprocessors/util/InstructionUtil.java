package net.torocraft.minecoprocessors.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.processor.InstructionCode;
import net.torocraft.minecoprocessors.processor.Register;

//TODO support .'s in labels
// which mean local? https://docs.oracle.com/cd/E19120-01/open.solaris/817-5477/esqaq/index.html


public class InstructionUtil {

  public static String compileFile(List<byte[]> instructions, List<Label> labels) {
    StringBuilder file = new StringBuilder();

    for (short address = 0; address < instructions.size(); address++) {
      file.append(compileLine(instructions.get(address), labels, address));
      file.append("\n");
    }

    return file.toString().trim();
  }

  public static String compileLine(byte[] instruction, List<Label> labels, short lineAddress) {

    if (instruction == null) {
      return "";
    }

    StringBuilder line = new StringBuilder();

    for (Label label : labels) {
      if (label.address == lineAddress) {
        line.append(label.name).append(":\n");
      }
    }

    InstructionCode command = InstructionCode.values()[instruction[0]];
    line.append(command.toString().toLowerCase());

    switch (command) {
      case MOV:
      case ADD:
      case AND:
      case OR:
      case XOR:
      case CMP:
      case SHL:
      case SHR:
      case SUB:
        line.append(" ");
        line.append(lower(Register.values()[instruction[1]]));
        line.append(", ");
        if (ByteUtil.getBit(instruction[3], 4)) {
          line.append(Integer.toString(instruction[2], 10));
        } else {
          line.append(lower(Register.values()[instruction[2]]));
        }
        break;
      case JMP:
      case JNZ:
      case JZ:
      case LOOP:
      case CALL:
        Label l = labels.get(instruction[1]);
        if (l != null) {
          line.append(" ");
          line.append(l.name.toLowerCase());
        }
        break;
      case MUL:
      case DIV:
      case NOT:
      case POP:
      case PUSH:
      case INT:
      case INC:
      case DEC:
        line.append(" ");
        if (ByteUtil.getBit(instruction[3], 0)) {
          line.append(Integer.toString(instruction[1], 10));
        } else {
          line.append(lower(Register.values()[instruction[1]]));
        }
        break;

      case RET:
      case NOP:
      case WFE:
        break;
      default:

    }
    return line.toString();
  }

  private static String lower(Enum<?> e) {
    return e.toString().toLowerCase();
  }

  public static List<byte[]> parseFile(String file, List<Label> labels) throws ParseException {
    List<byte[]> instructions = new ArrayList<>();

    String[] lines = file.split("\\n\\r?");

    for (String line : lines) {
      parseLineForLabels(line, labels, (short) instructions.size());
    }

    for (String line : lines) {
      byte[] instruction = parseLine(line, labels, (short) instructions.size());
      if (instruction != null) {
        instructions.add(instruction);
      }
    }

    return instructions;
  }

  public static void parseLineForLabels(String line, List<Label> labels, short lineAddress)
      throws ParseException {
    line = removeComments(line);
    if (line.trim().length() < 1) {
      return;
    }
    if (isLabelInstruction(line)) {
      parseLabelLine(line, labels, lineAddress);
      return;
    }
  }

  public static byte[] parseLine(String line, List<Label> labels, short lineAddress)
      throws ParseException {
    byte[] instruction = new byte[4];
    line = removeComments(line);
    if (line.trim().length() < 1) {
      return null;
    }
    if (isLabelInstruction(line)) {
      setLabelAddress(line, labels, lineAddress);
    }

    line = removeLabels(line);

    if (line.trim().length() < 1) {
      return null;
    }

    return parseCommandLine(line, labels, instruction);
  }

  private static String removeComments(String line) {
    List<String> l = regex("^([^;]*);.*", line, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return l.get(0);
    }
    return line;
  }

  private static String removeLabels(String line) {
    List<String> l = regex("^\\s*[A-Za-z0-9-_]+:\\s*(.*)$", line, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return l.get(0);
    }
    return line;
  }

  private static void setLabelAddress(String line, List<Label> labels, short lineAddress)
      throws ParseException {
    List<String> l = regex("^\\s*([A-Za-z0-9-_]+):.*$", line, Pattern.CASE_INSENSITIVE);
    if (l.size() != 1) {
      throw new ParseException(line, "incorrect label format");
    }
    String label = l.get(0).toLowerCase();
    setLabelAddress(line, labels, label, lineAddress);
  }

  private static void parseLabelLine(String line, List<Label> labels, short lineAddress)
      throws ParseException {
    List<String> l = regex("^\\s*([A-Za-z0-9-_]+):.*$", line, Pattern.CASE_INSENSITIVE);
    if (l.size() != 1) {
      throw new ParseException(line, "incorrect label format");
    }
    String label = l.get(0).toLowerCase();
    verifyLabelIsUnique(line, labels, label);
    labels.add(new Label(lineAddress, label));
  }

  private static void setLabelAddress(String line, List<Label> labels, String label, short address)
      throws ParseException {
    for (Label l : labels) {
      if (l.name.equalsIgnoreCase(label)) {
        l.address = address;
        return;
      }
    }
    throw new ParseException(line, "label not found");
  }

  private static void verifyLabelIsUnique(String line, List<Label> labels, String label)
      throws ParseException {
    for (Label l : labels) {
      if (l.name.equalsIgnoreCase(label)) {
        throw new ParseException(line, "label already defined");
      }
    }
  }

  private static boolean isLabelInstruction(String line) {
    return line.matches("^\\s*[A-Za-z0-9-_]+:.*$");
  }

  private static byte[] parseCommandLine(String line, List<Label> labels, byte[] instruction)
      throws ParseException {
    InstructionCode instructionCode = parseInstructionCode(line);
    instruction[0] = (byte) instructionCode.ordinal();

    switch (instructionCode) {
      case MOV:
      case ADD:
      case AND:
      case OR:
      case XOR:
      case CMP:
      case SHL:
      case SHR:
      case SUB:
        return parseDoubleOperands(line, instruction);

      case JMP:
      case JNZ:
      case JZ:
      case LOOP:
      case CALL:
        return parseLabelOperand(line, instruction, labels);

      case MUL:
      case DIV:
      case NOT:
      case POP:
      case PUSH:
      case INT:
      case INC:
      case DEC:
        return parseSingleOperand(line, instruction);

      case RET:
      case NOP:
      case WFE:
        return instruction;
      default:
        throw new ParseException(line, "invalid command [" + instructionCode + "]");
    }
  }

  private static byte[] parseSingleOperand(String line, byte[] instruction) throws ParseException {
    List<String> l = regex("^\\s*[A-Z]+\\s+([A-Z0-9]+)\\s*$", line, Pattern.CASE_INSENSITIVE);
    if (l.size() != 1) {
      throw new ParseException(line, "incorrect operand format");
    }
    instruction = parseVariableOperand(line, instruction, l.get(0), 0);
    return instruction;
  }

  private static byte[] parseDoubleOperands(String line, byte[] instruction) throws ParseException {
    List<String> l = regex("^\\s*[A-Z]+\\s+([A-Z]+)\\s*,\\s*([A-Z0-9]+)\\s*$", line,
        Pattern.CASE_INSENSITIVE);
    if (l.size() != 2) {
      throw new ParseException(line, "incorrect operand format");
    }
    instruction = parseVariableOperand(line, instruction, l.get(0), 0);
    instruction = parseVariableOperand(line, instruction, l.get(1), 1);
    return instruction;
  }

  private static byte[] parseVariableOperand(String line, byte[] instruction, String operand,
      int operandIndex) throws ParseException {
    if (isLiteral(operand)) {
      instruction[operandIndex + 1] = parseLiteral(line, operand);
      instruction[3] = ByteUtil.setBit(instruction[3], true, operandIndex * 4);
    } else {
      instruction[operandIndex + 1] = (byte) parseRegister(line, operand).ordinal();
    }
    return instruction;
  }

  private static byte[] parseLabelOperand(String line, byte[] instruction, List<Label> labels)
      throws ParseException {
    List<String> l = regex("^\\s*[A-Z]+\\s+([A-Z_-]+)\\s*$", line, Pattern.CASE_INSENSITIVE);
    if (l.size() != 1) {
      throw new ParseException(line, "incorrect label format");
    }
    instruction[1] = parseLabel(line, l.get(0).toLowerCase(), labels);
    return instruction;
  }

  private static byte parseLabel(String line, String label, List<Label> labels)
      throws ParseException {
    try {
      for (int i = 0; i < labels.size(); i++) {
        if (labels.get(i).name.equalsIgnoreCase(label)) {
          return (byte) i;
        }
      }
    } catch (Exception e) {
      Minecoprocessors.proxy.handleUnexpectedException(e);
      throw new ParseException(line, "[" + label + "] is not a valid label", e);
    }
    throw new ParseException(line, "[" + label + "] has not been defined");
  }

  private static Register parseRegister(String line, String s) throws ParseException {
    s = s.trim().toUpperCase();
    try {
      return Register.valueOf(s);
    } catch (IllegalArgumentException e) {
      throw new ParseException(line, "[" + s + "] is not a valid register", e);
    }
  }

  private static boolean isLiteral(String s) {
    s = s.trim();

    if (s.matches("^[0-9-]+$")) {
      return true;
    }

    if (s.matches("^0o[0-7]+$")) {
      return true;
    }

    if (s.matches("^0x[0-9A-Fa-f]+$")) {
      return true;
    }

    if (s.matches("^[0-9-]+d$")) {
      return true;
    }

    if (s.matches("^[0-1]+b$")) {
      return true;
    }
    return false;
  }

  private static void testIsLiteral() {
    assert !isLiteral("abc");
    assert isLiteral("15");
    assert isLiteral("-15");
    assert isLiteral("0x15");
    assert isLiteral("0o15");
    assert !isLiteral("0o18");
    assert isLiteral("0o10");
    assert isLiteral("0x00015");
    assert isLiteral("0x0aF");
    assert !isLiteral("-0x0aF");
    assert isLiteral("101b");
    assert !isLiteral("1012b");
    assert isLiteral("015d");
  }

  private static byte parseLiteral(String line, String s) throws ParseException {
    int i = parseLiteralToInt(line, s);

    if (i < 0) {
      i += 256;
    }

    if (i > 255 || i < 0) {
      throw new ParseException(line, "operand too large [" + s + "]");
    }

    return (byte) i;
  }

  private static int parseInt(String s, int radix, String line) throws ParseException {
    try {
      return Integer.parseInt(s, radix);
    } catch (NumberFormatException e) {
      throw new ParseException(line, "[" + s + "] is not a valid operand literal", e);
    }
  }

  private static int parseLiteralToInt(String line, String s) throws ParseException {
    s = s.trim();
    List<String> l;

    l = regex("^([0-9-]+)d?$", s, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return parseInt(l.get(0), 10, line);
    }

    l = regex("^0o([0-7]+)$", s, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return parseInt(l.get(0), 8, line);
    }

    l = regex("^0x([0-9A-Fa-f]+)$", s, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return parseInt(l.get(0), 16, line);
    }

    l = regex("^([0-1]+)b$", s, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return parseInt(l.get(0), 2, line);
    }

    throw new ParseException(line, "invalid operand literal type [" + s + "]");
  }

  private static void testParseLiteral() {
    try {
      assert parseLiteral(null, " -120 ") == (byte) -120;
      assert parseLiteral(null, " 120d") == (byte) 120;
      assert parseLiteral(null, " -15 ") == (byte) -15;
      assert parseLiteral(null, " 0o10 ") == (byte) 8;
      assert parseLiteral(null, "0x0ff") == (byte) 0x0ff;
      assert parseLiteral(null, "1111b") == (byte) 0x0f;
      assert parseLiteral(null, "10000000b") == (byte) 0b10000000;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testParseLiteralOversized() {
    ParseException e = null;
    try {
      parseLiteral(null, "100000000b");
    } catch (ParseException t) {
      e = t;
    }
    assert e != null;
  }

  public static List<String> regex(final String pattern, final String screen, int flags) {
    Pattern p = Pattern.compile(pattern, flags);
    Matcher m = p.matcher(screen);
    List<String> l = new ArrayList<>();
    if (m.find() && m.groupCount() > 0) {
      for (int i = 1; i <= m.groupCount(); i++) {
        String s = m.group(i);
        if (s != null) {
          l.add(s.replaceAll("\\s+", " ").trim());
        }
      }
      return l;
    }
    return l;
  }

  private static InstructionCode parseInstructionCode(String line) throws ParseException {
    line = line.toUpperCase().trim().split("\\s+")[0];
    try {
      return InstructionCode.valueOf(line);
    } catch (IllegalArgumentException e) {
      throw new ParseException(line, "invalid command", e);
    }
  }

  public static void test() {
    testIsLiteral();
    testParseLiteral();
    testParseLiteralOversized();
    testRemoveLabels();
    testRemoveComments();
    testStandardOperands();
    testLabelOperands();
    testSingleOperands();
    testNoOperands();
    testCompileLine();
    testParseFile();
  }

  private static void testRemoveLabels() {
    assert removeLabels("testfoo: mov a, 5 ; and comment; ignore me")
        .equals("mov a, 5 ; and comment; ignore me");
    assert removeLabels("testfoo:").equals("");
    assert removeLabels(" testfoo: ").equals("");
  }

  private static void testRemoveComments() {
    try {
      assert removeComments("test foo ; and comment; ignore me").equals("test foo");
      assert removeComments(" ; and comment; ignore me").equals("");
      assert parseLine("  ;loop \t test_label  \t ", null, (short) 0) == null;
      assert parseLine(";loop", null, (short) 0) == null;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testLabelOperands() {
    try {
      List<Label> labels = new ArrayList<>();
      labels.add(new Label((short) 13, "TEST_LABEL"));
      byte[] instruction = parseLine("loop \t test_label  \t ", labels, (short) 0);
      assert instruction[0] == (byte) InstructionCode.LOOP.ordinal();
      assert instruction[1] == (byte) 0;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testStandardOperands() {
    try {
      byte[] instruction = parseLine("mov A, B ; test mov", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.MOV.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[2] == (byte) Register.B.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = parseLine("moV \t D  \t , d", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.MOV.ordinal();
      assert instruction[1] == (byte) Register.D.ordinal();
      assert instruction[2] == (byte) Register.D.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = parseLine("add a,b", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.ADD.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[2] == (byte) Register.B.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = parseLine("Sub a,25", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.SUB.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[2] == (byte) 25;
      assert instruction[3] == (byte) 0b00010000;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testSingleOperands() {
    try {
      byte[] instruction = parseLine("push A", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.PUSH.ordinal();
      assert instruction[1] == (byte) Register.A.ordinal();
      assert instruction[3] == (byte) 0b00000000;
      instruction = parseLine("push 35", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.PUSH.ordinal();
      assert instruction[1] == (byte) 35;
      assert instruction[3] == (byte) 0b00000001;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testNoOperands() {
    try {
      byte[] instruction = parseLine("nop", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.NOP.ordinal();
      instruction = parseLine("ret  \t", null, (short) 0);
      assert instruction[0] == (byte) InstructionCode.RET.ordinal();
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testCompileLine() {
    try {
      List<Label> labels = new ArrayList<>();
      labels.add(new Label((short) 56, "test"));

      byte[] instruction = parseLine("mov A, pr ; test mov", labels, (short) 0);
      assert compileLine(instruction, labels, (short) 0).equals("mov a, pr");

      instruction = parseLine("mov A, 36 ; test mov", labels, (short) 0);
      assert compileLine(instruction, labels, (short) 0).equals("mov a, 36");

      instruction = parseLine("push a ; test single op", labels, (short) 0);
      assert compileLine(instruction, labels, (short) 0).equals("push a");

      instruction = parseLine("push 89 ; test single op", null, (short) 0);
      assert compileLine(instruction, labels, (short) 0).equals("push 89");

      instruction = parseLine("push 89", labels, (short) 0);
      assert compileLine(instruction, labels, (short) 56).equals("test:\npush 89");

      instruction = parseLine("jmp test", labels, (short) 0);
      assert compileLine(instruction, labels, (short) 0).equals("jmp test");

      instruction = parseLine("ret", labels, (short) 0);
      assert compileLine(instruction, labels, (short) 0).equals("ret");

    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private static void testParseFile() {
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

      List<byte[]> instructions = parseFile(file, labels);

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

      String reCompiled = compileFile(instructions, labels);

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
