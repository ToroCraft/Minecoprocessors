package net.torocraft.minecoprocessors.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.Query;

import net.torocraft.minecoprocessors.processor.InstructionCode;
import net.torocraft.minecoprocessors.processor.Register;

public class InstructionUtil {

	public static class Label {
		public final short address;
		public final String name;

		public Label(short address, String name) {
			this.address = address;
			this.name = name;
		}
	}

	public static String compileLine(byte[] instruction, List<Label> labels, short lineAddress) {
		StringBuilder line = new StringBuilder();

		for(Label label : labels){
			if(label.address == lineAddress){
				line.append(label.name).append(":\n");
			}
		}

		InstructionCode command = InstructionCode.values()[instruction[0]];
		line.append(command);

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
			line.append(Register.values()[instruction[1]]);
			line.append(", ");
			if (ByteUtil.getBit(instruction[3], 4)) {
				line.append(Integer.toString(instruction[2], 10));
			}else{
				line.append(Register.values()[instruction[2]]);
			}
			break;
		case JMP:
		case JNZ:
		case JZ:
		case LOOP:
		case CALL:
			line.append(" ");
			line.append(labels.get(instruction[1]));
			//return parseLabelOperand(line, instruction, labels);
			break;

		case MUL:
		case DIV:
		case NOT:
		case POP:
		case PUSH:
			line.append(" ");
			if (ByteUtil.getBit(instruction[3], 0)) {
				line.append(Integer.toString(instruction[1], 10));
			}else{
				line.append(Register.values()[instruction[1]]);
			}
			break;

		case RET:
			break;
		default:
			//TODO fail somehow?
			//throw new ParseException(line, "invalid command [" + command + "]");
		}


		return line.toString();
	}

	public static byte[] parseLine(String line, List<Label> labels, short lineAddress) throws ParseException {
		byte[] instruction = new byte[4];

		line = removeComments(line);

		if (line.trim().length() < 1) {
			return null;
		}

		if (isLabelInstruction(line)) {
			parseLabelLine(line, labels, lineAddress);
			return null;
		}

		return parseCommandLine(line, labels, instruction);
	}

	private static String removeComments(String line) {
		List<String> l = regex("^([^;]+);.*", line, Pattern.CASE_INSENSITIVE);
		if (l.size() == 1) {
			return l.get(0);
		}
		return line;
	}

	private static void parseLabelLine(String line, List<Label> labels, short lineAddress) throws ParseException {
		List<String> l = regex("^\\s*([A-Za-z-_]+):\\s*$", line, Pattern.CASE_INSENSITIVE);
		if (l.size() != 1) {
			throw new ParseException(line, "incorrect label format");
		}
		String label = l.get(0).toLowerCase();
		verifyLabelIsUnique(line, labels, label);
		labels.add(new Label(lineAddress, label));
	}

	private static void verifyLabelIsUnique(String line, List<Label> labels, String label) throws ParseException {
		for (Label l : labels) {
			if (l.name.equalsIgnoreCase(label)) {
				throw new ParseException(line, "label already defined");
			}
		}
	}

	private static boolean isLabelInstruction(String line) {
		return line.matches("^\\s*[A-Za-z-_]+:\\s*$");
	}

	private static byte[] parseCommandLine(String line, List<Label> labels, byte[] instruction) throws ParseException {
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
			return parseSingleOperand(line, instruction);

		case RET:
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
		List<String> l = regex("^\\s*[A-Z]+\\s+([A-Z]+)\\s*,\\s*([A-Z0-9]+)\\s*$", line, Pattern.CASE_INSENSITIVE);
		if (l.size() != 2) {
			throw new ParseException(line, "incorrect operand format");
		}
		instruction = parseVariableOperand(line, instruction, l.get(0), 0);
		instruction = parseVariableOperand(line, instruction, l.get(1), 1);
		return instruction;
	}

	private static byte[] parseVariableOperand(String line, byte[] instruction, String operand, int operandIndex) throws ParseException {
		if (isNumeric(operand)) {
			instruction[operandIndex + 1] = parseLiteral(line, operand);
			instruction[3] = ByteUtil.setBit(instruction[3], true, operandIndex * 4);
		} else {
			instruction[operandIndex + 1] = (byte) parseRegister(line, operand).ordinal();
		}
		return instruction;
	}

	private static boolean isNumeric(String s) {
		return s.trim().matches("[0-9]+");
	}

	private static byte[] parseLabelOperand(String line, byte[] instruction, List<Label> labels) throws ParseException {
		List<String> l = regex("^\\s*[A-Z]+\\s+([A-Z_-]+)\\s*$", line, Pattern.CASE_INSENSITIVE);
		if (l.size() != 1) {
			throw new ParseException(line, "incorrect label format");
		}
		instruction[1] = parseLabel(line, l.get(0), labels);
		return instruction;
	}

	private static byte parseLabel(String line, String label, List<Label> labels) throws ParseException {
		try {
			for (int i = 0; i < labels.size(); i++) {
				if (labels.get(i).name.equalsIgnoreCase(label)) {
					return (byte) i;
				}
			}
		} catch (Exception e) {
			throw new ParseException(line, "[" + label + "] is not a valid label", e);
		}
		throw new ParseException(line, "[" + label + "] has not been defined");
	}

	private static Register parseRegister(String line, String s) throws ParseException {
		try {
			return Register.valueOf(s.trim().toUpperCase());
		} catch (Exception e) {
			throw new ParseException(line, "[" + s + "] is not a valid register", e);
		}
	}

	private static byte parseLiteral(String line, String s) throws ParseException {
		try {
			return Byte.valueOf(s.trim(), 10);
		} catch (Exception e) {
			throw new ParseException(line, "[" + s + "] is not a valid constant", e);
		}
	}

	private static List<String> regex(final String pattern, final String screen, int flags) {
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
		try {
			return InstructionCode.valueOf(line.toUpperCase().trim().split("\\s+")[0]);
		} catch (Exception e) {
			throw new ParseException(line, "invalid command", e);
		}
	}

	public static void test() {
		testRemoveComments();
		testStandardOperands();
		testLabelOperands();
		testSingleOperands();
		testCompileLine();
	}

	private static void testRemoveComments() {
		try {
			assert removeComments("test foo ; and comment; ignore me").equals("test foo");
			assert removeComments(" ; and comment; ignore me").equals("");
			assert parseLine("  ;loop \t test_label  \t ", null, (short) 0) == null;
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
			byte[] instruction = parseLine("mov AX, oa ; test mov", null, (short) 0);
			assert instruction[0] == (byte) InstructionCode.MOV.ordinal();
			assert instruction[1] == (byte) Register.AX.ordinal();
			assert instruction[2] == (byte) Register.OA.ordinal();
			assert instruction[3] == (byte) 0b00000000;
			instruction = parseLine("moV \t ib  \t , oa", null, (short) 0);
			assert instruction[0] == (byte) InstructionCode.MOV.ordinal();
			assert instruction[1] == (byte) Register.IB.ordinal();
			assert instruction[2] == (byte) Register.OA.ordinal();
			assert instruction[3] == (byte) 0b00000000;
			instruction = parseLine("add ax,bx", null, (short) 0);
			assert instruction[0] == (byte) InstructionCode.ADD.ordinal();
			assert instruction[1] == (byte) Register.AX.ordinal();
			assert instruction[2] == (byte) Register.BX.ordinal();
			assert instruction[3] == (byte) 0b00000000;
			instruction = parseLine("Sub ax,25", null, (short) 0);
			assert instruction[0] == (byte) InstructionCode.SUB.ordinal();
			assert instruction[1] == (byte) Register.AX.ordinal();
			assert instruction[2] == (byte) 25;
			assert instruction[3] == (byte) 0b00010000;
		} catch (ParseException e) {
			throw new AssertionError(e);
		}
	}

	private static void testSingleOperands() {
		try {
			byte[] instruction = parseLine("push AX", null, (short) 0);
			assert instruction[0] == (byte) InstructionCode.PUSH.ordinal();
			assert instruction[1] == (byte) Register.AX.ordinal();
			assert instruction[3] == (byte) 0b00000000;
			instruction = parseLine("push 35", null, (short) 0);
			assert instruction[0] == (byte) InstructionCode.PUSH.ordinal();
			assert instruction[1] == (byte) 35;
			assert instruction[3] == (byte) 0b00000001;
		} catch (ParseException e) {
			throw new AssertionError(e);
		}
	}

	private static void testCompileLine() {
		try {
			List<Label> labels = new ArrayList<>();
			labels.add(new Label((short)56, "TEST"));

			byte[] instruction = parseLine("mov AX, oa ; test mov", labels, (short) 0);
			assert compileLine(instruction, labels, (short) 0).equals("MOV AX, OA");

			instruction = parseLine("mov AX, 36 ; test mov", labels, (short) 0);
			assert compileLine(instruction, labels, (short) 0).equals("MOV AX, 36");

			instruction = parseLine("push AX ; test single op", labels, (short) 0);
			assert compileLine(instruction, labels, (short) 0).equals("PUSH AX");

			instruction = parseLine("push 89 ; test single op", null, (short) 0);
			assert compileLine(instruction, labels, (short) 0).equals("PUSH 89");

			instruction = parseLine("push 89", labels, (short) 0);
			assert compileLine(instruction, labels, (short) 56).equals("TEST:\nPUSH 89");

			instruction = parseLine("jmp test", labels, (short) 0);
			System.out.println(compileLine(instruction, labels, (short) 0));
			assert compileLine(instruction, labels, (short) 0).equals("JMP TEST");



		} catch (ParseException e) {
			throw new AssertionError(e);
		}
	}

}
