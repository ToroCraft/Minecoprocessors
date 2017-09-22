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

    Label label;

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
      case ROR:
      case ROL:
      case SAL:
      case SAR:
        line.append(" ");
        line.append(lower(Register.values()[instruction[1]]));
        line.append(", ");
        if (ByteUtil.getBit(instruction[3], 4)) {
          line.append(Integer.toString(instruction[2], 10));
        } else {
          line.append(lower(Register.values()[instruction[2]]));
        }
        break;
      case DJNZ:
        line.append(" ");
        line.append(lower(Register.values()[instruction[1]]));
        line.append(", ");
        label = labels.get(instruction[2]);
        if (label != null) {
          line.append(label.name.toLowerCase());
        }
        break;
      case JMP:
      case JNZ:
      case JZ:
      case JC:
      case JNC:
      case LOOP:
      case CALL:
        label = labels.get(instruction[1]);
        if (label != null) {
          line.append(" ");
          line.append(label.name.toLowerCase());
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
      case HLT:
      case CLZ:
      case CLC:
      case SEZ:
      case SEC:
        break;
      default:
        throw new RuntimeException("Command enum had unexpected value");
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
    if (!line.trim().isEmpty() && isLabelInstruction(line)) {
      parseLabelLine(line, labels, lineAddress);
    }
  }

  public static byte[] parseLine(String line, List<Label> labels, short lineAddress)
      throws ParseException {
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

    return parseCommandLine(line, labels);
  }

  static String removeComments(String line) {
    List<String> l = regex("^([^;]*);.*", line, Pattern.CASE_INSENSITIVE);
    if (l.size() == 1) {
      return l.get(0);
    }
    return line;
  }

  static String removeLabels(String line) {
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

  private static byte[] parseCommandLine(String line, List<Label> labels)
      throws ParseException {
    InstructionCode instructionCode = parseInstructionCode(line);

    byte[] instruction;

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
      case DJNZ:
      case ROR:
      case ROL:
      case SAL:
      case SAR:
        instruction =  parseDoubleOperands(line, labels);
        break;

      case JMP:
      case JNZ:
      case JZ:
      case JC:
      case JNC:
      case LOOP:
      case CALL:
        instruction = parseLabelOperand(line, labels);
        break;

      case MUL:
      case DIV:
      case NOT:
      case POP:
      case PUSH:
      case INT:
      case INC:
      case DEC:
        instruction = parseSingleOperand(line, labels);
        break;

      case RET:
      case NOP:
      case WFE:
      case HLT:
      case CLZ:
      case CLC:
      case SEZ:
      case SEC:
        instruction = new byte[1];
        break;

      default:
        throw new RuntimeException("instructionCode enum had unexpected value");
    }

    instruction[0] = (byte) instructionCode.ordinal();
    return instruction;
  }

  private static byte[] parseSingleOperand(String line, List<Label> labels) throws ParseException {
    byte[] instruction = new byte[4];
    List<String> l = regex("^\\s*[A-Z]+\\s+([A-Z0-9]+)\\s*$", line, Pattern.CASE_INSENSITIVE);
    if (l.size() != 1) {
      throw new ParseException(line, "incorrect operand format");
    }
    instruction = parseVariableOperand(line, instruction, l.get(0), 0, labels);
    return instruction;
  }

  static List<String> splitDoubleOperandString(String line) {
    return regex("^\\s*[A-Z]+\\s+([A-Z]+)\\s*,\\s*([A-Z0-9_-]+)\\s*$", line, Pattern.CASE_INSENSITIVE);
  }

  private static byte[] parseDoubleOperands(String line, List<Label> labels) throws ParseException {
    byte[] instruction = new byte[4];
    List<String> l = splitDoubleOperandString(line);
    if (l.size() != 2) {
      throw new ParseException(line, "incorrect operand format");
    }
    instruction = parseVariableOperand(line, instruction, l.get(0), 0, labels);
    instruction = parseVariableOperand(line, instruction, l.get(1), 1, labels);
    return instruction;
  }

  static byte[] parseVariableOperand(String line, byte[] instruction, String operand,
      int operandIndex, List<Label> labels) throws ParseException {

    if (isLiteral(operand)) {
      instruction[operandIndex + 1] = parseLiteral(line, operand);
      instruction[3] = ByteUtil.setBit(instruction[3], true, operandIndex * 4);

    } else if (isRegister(operand)) {
      instruction[operandIndex + 1] = (byte) parseRegister(line, operand).ordinal();

    } else {
      instruction[operandIndex + 1] = parseLabel(line, operand.toLowerCase(), labels);

    }
    return instruction;
  }

  private static byte[] parseLabelOperand(String line, List<Label> labels)
      throws ParseException {
    byte[] instruction = new byte[2];
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

  static boolean isRegister(String operand) {
    if (operand == null) {
      return false;
    }
    try {
      Register.valueOf(operand.toUpperCase());
      return true;
    } catch (IllegalArgumentException ignore){
      return false;
    }
  }

  static boolean isLiteral(String s) {
    if (s == null) {
      return false;
    }

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

    return s.matches("^[0-1]+b$");
  }

  static byte parseLiteral(String line, String s) throws ParseException {
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

}
