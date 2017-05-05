package net.torocraft.minecoprocessors.util;

import net.torocraft.minecoprocessors.processor.InstructionCode;

public class InstructionUtil {

	public static byte[] parseLine(String line) {
		byte[] instruction = new byte[4];
		
		InstructionCode instructionCode = parseInstructionCode(line);
		instruction[0] = (byte) instructionCode.ordinal();
		
		switch (instructionCode) {
		case MOV:
			return parseMov(line, instruction);
		case ADD:
			break;
		case AND:
			break;
		case CMP:
			break;
		case DIV:
			break;
		case JMP:
			break;
		case JNZ:
			break;
		case JZ:
			break;
		case LOOP:
			break;
		case MUL:
			break;
		case NOT:
			break;
		case OR:
			break;
		case POP:
			break;
		case PUSH:
			break;
		case RET:
			break;
		case SHL:
			break;
		case SHR:
			break;
		case SUB:
			break;
		case XOR:
			break;
		default:
			break;
		}
		
		return instruction;
	}
	
	private static byte[] parseMov(String line, byte[] instruction) {
		instruction[1] = 1;
		instruction[2] = 2;
		return instruction;
	}

	private static InstructionCode parseInstructionCode(String line) {
		try {
			return InstructionCode.valueOf(line.trim().split("\\s+")[0]);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void test() {

	}

}
