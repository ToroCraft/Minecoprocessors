package net.torocraft.minecoprocessors.processor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.torocraft.minecoprocessors.util.ByteUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil.Label;
import net.torocraft.minecoprocessors.util.ParseException;

@SuppressWarnings({ "rawtypes" })
public class Processor implements IProcessor {

	private static final String NBT_PROGRAM = "program";

	private List<Label> labels = new ArrayList<>();
	private byte[] instruction;
	private List program = new ArrayList();
	private byte[] stack = new byte[64];
	private byte[] registers = new byte[Register.values().length];
	private boolean overflow;
	private boolean zero;
	private boolean falt;
	private short pc;
	private byte sp;

	@Override
	public void reset() {
		falt = false;
		zero = false;
		overflow = false;
		pc = 0;
		sp = -1;
	}

	@Override
	public void setInput(byte b) {

	}

	@Override
	public byte getOutput() {
		return 0;
	}

	@Override
	public void load(String program) {
		labels = new ArrayList<>();
		try {
			InstructionUtil.parseFile(program, labels);
		} catch (ParseException e) {
			e.printStackTrace();
			falt = true;
		}
		reset();
	}

	@Override
	public void readFromNBT(NBTTagCompound c) {
		// c.setb
		// program = c.getString(NBT_PROGRAM);
	}

	@Override
	public NBTTagCompound writeToNBT() {
		NBTTagCompound c = new NBTTagCompound();
		// c.setString(NBT_PROGRAM, program);
		return c;
	}

	@Override
	public void tick() {
		if (falt) {
			return;
		}
		process();
	}

	private void process() {

		if (pc >= program.size()) {
			falt = true;
			return;
		}

		if (pc < 0) {
			pc = 0;
		}

		instruction = (byte[]) program.get(pc);
		pc++;

		switch (InstructionCode.values()[instruction[0]]) {
		case ADD:
			processAdd();
			return;
		case AND:
			processAnd();
			return;
		case CALL:
			break;
		case CMP:
			processCmp();
			return;
		case DIV:
			break;
		case JMP:
			processJmp();
			return;
		case JNZ:
			processJnz();
			return;
		case JZ:
			processJz();
			return;
		case LOOP:
			processJmp();
			return;
		case MOV:
			processMov();
			return;
		case MUL:
			break;
		case NOT:
			processNot();
			return;
		case OR:
			processOr();
			return;
		case POP:
			processPop();
			return;
		case PUSH:
			processPush();
			return;
		case RET:
			break;
		case SHL:
			processShl();
			return;
		case SHR:
			processShr();
			return;
		case SUB:
			processSub();
			return;
		case XOR:
			processXor();
			return;
		}

		falt = true;
	}

	private void processMov() {
		registers[instruction[1]] = getVariableOperand(1);
	}

	private void processAdd() {
		int a = getVariableOperand(0);
		int b = getVariableOperand(1);
		int z = a + b;
		testOverflow(z);
		zero = z == 0;
		registers[instruction[1]] = (byte) z;
	}

	private void processAnd() {
		byte a = getVariableOperand(0);
		byte b = getVariableOperand(1);
		byte z = (byte) (a & b);
		zero = z == 0;
		registers[instruction[1]] = z;
	}

	private void processXor() {
		byte a = getVariableOperand(0);
		byte b = getVariableOperand(1);
		byte z = (byte) (a ^ b);
		zero = z == 0;
		registers[instruction[1]] = z;
	}

	private void processOr() {
		byte a = getVariableOperand(0);
		byte b = getVariableOperand(1);
		byte z = (byte) (a | b);
		zero = z == 0;
		registers[instruction[1]] = z;
	}

	private void processNot() {
		byte a = getVariableOperand(0);
		byte z = (byte) ~a;
		zero = z == 0;
		registers[instruction[1]] = z;
	}

	private void processSub() {
		int a = getVariableOperand(0);
		int b = getVariableOperand(1);
		int z = a - b;
		testOverflow(z);
		zero = z == 0;
		registers[instruction[1]] = (byte) z;
	}

	private void processCmp() {
		int a = getVariableOperand(0);
		int b = getVariableOperand(1);
		int z = a - b;
		testOverflow(z);
		zero = z == 0;
	}

	private void processShl() {
		byte a = getVariableOperand(0);
		byte b = getVariableOperand(1);
		if (b > 8) {
			b = 8;
		}
		byte z = (byte) (a << b);
		zero = z == 0;
		registers[instruction[1]] = z;
	}

	private void processShr() {
		int a = getVariableOperand(0) & 0x00ff;
		int b = getVariableOperand(1) & 0x00ff;
		if (b > 8) {
			b = 8;
		}
		byte z = (byte) (a >>> b);
		zero = z == 0;
		registers[instruction[1]] = z;
	}

	private void processJmp() {
		pc = labels.get(instruction[1]).address;
	}

	private void processJz() {
		if (zero) {
			processJmp();
		}
	}

	private void processJnz() {
		if (!zero) {
			processJmp();
		}
	}

	private void processPush() {
		byte a = getVariableOperand(0);
		sp++;
		if (sp >= stack.length) {
			falt = true;
			return;
		}
		stack[sp] = a;
	}

	private void processPop() {
		if (sp < 0) {
			falt = true;
			return;
		}
		registers[instruction[1]] = stack[sp];
		sp--;
	}

	private void testOverflow(int z) {
		overflow = z != (int) (byte) z;
	}

	private byte getVariableOperand(int operandIndex) {
		byte operand = instruction[operandIndex + 1];
		if (isLiteral(operandIndex)) {
			return operand;
		} else {
			return registers[operand];
		}
	}

	private boolean isLiteral(int operandIndex) {
		return ByteUtil.getBit(instruction[3], operandIndex * 4);
	}

	public void test() {
		testTestOverFlow();
		testProcessMov();
		testProcessAdd();
		testProcessAnd();
		testProcessSub();
		testProcessCmp();
		testProcessJmp();
		testProcessJz();
		testProcessJnz();
		testProcessNot();
		testProcessOr();
		testProcessShl();
		testProcessShr();
		testProcessXor();
		testProcessPushPop();
	}

	private void testTestOverFlow() {
		testOverflow(1);
		assert !overflow;

		testOverflow(1000);
		assert overflow;

		testOverflow(128);
		assert overflow;

		testOverflow(127);
		assert !overflow;

		testOverflow(-128);
		assert !overflow;

		testOverflow(-129);
		assert overflow;
	}

	private void testProcessMov() {
		try {
			setupTest(0, 30, 0, 0, "mov ax, bx");
			processMov();
			assertRegisters(30, 30, 0, 0);

			setupTest(0, 30, 0, 0, "mov ax, 51");
			processMov();
			assertRegisters(51, 30, 0, 0);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessAdd() {
		try {
			setupTest(3, 30, 0, 0, "add ax, bx");
			processAdd();
			assertRegisters(33, 30, 0, 0);
			assert !overflow;
			assert !zero;

			setupTest(11, 0, 0, 0, "add ax, 51");
			processAdd();
			assertRegisters(62, 0, 0, 0);
			assert !overflow;
			assert !zero;

			setupTest(-51, 0, 0, 0, "add ax, 51");
			processAdd();
			assertRegisters(0, 0, 0, 0);
			assert !overflow;
			assert zero;

			setupTest(130, 0, 130, 0, "add ax, cx");
			processAdd();
			assertRegisters(4, 0, 130, 0);
			assert overflow;
			assert !zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessSub() {
		try {
			setupTest(50, 0, 0, 10, "sub ax, dx");
			processSub();
			assertRegisters(40, 0, 0, 10);
			assert !overflow;
			assert !zero;

			setupTest(-130, 0, 0, 130, "sub ax, dx");
			processSub();
			assertRegisters(-4, 0, 0, 130);
			assert overflow;
			assert !zero;

			setupTest(130, 0, 0, 130, "sub ax, dx");
			processSub();
			assertRegisters(0, 0, 0, 130);
			assert !overflow;
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessCmp() {
		try {
			setupTest(50, 0, 0, 10, "cmp ax, dx");
			processCmp();
			assertRegisters(50, 0, 0, 10);
			assert !overflow;
			assert !zero;

			setupTest(-130, 0, 0, 130, "cmp ax, dx");
			processCmp();
			assertRegisters(-130, 0, 0, 130);
			assert overflow;
			assert !zero;

			setupTest(130, 0, 0, 130, "cmp ax, dx");
			processCmp();
			assertRegisters(130, 0, 0, 130);
			assert !overflow;
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessAnd() {
		try {
			setupTest(0b011111, 0b010, 0, 0, "and ax, bx");
			processAnd();
			assertRegisters(0b010, 0b010, 0, 0);
			assert !zero;

			setupTest(0b011101, 0b010, 0, 0, "and ax, bx");
			processAnd();
			assertRegisters(0, 0b010, 0, 0);
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessXor() {
		try {
			setupTest(0b0101, 0b0110, 0, 0, "xor ax, bx");
			processXor();
			assertRegisters(0b011, 0b0110, 0, 0);
			assert !zero;

			setupTest(0b0101, 0b0101, 0, 0, "xor ax, bx");
			processXor();
			assertRegisters(0, 0b0101, 0, 0);
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessOr() {
		try {
			setupTest(0b01000, 0, 0, 0b0111, "or dx, ax");
			processOr();
			assertRegisters(0b01000, 0, 0, 0b01111);
			assert !zero;

			setupTest(0, 0, 0, 0, "or dx, ax");
			processOr();
			assertRegisters(0, 0, 0, 0);
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessNot() {
		try {
			setupTest(0, 0, 0b1010, 0, "not cx");
			processNot();
			assertRegisters(0, 0, 0b11110101, 0);
			assert !zero;

			setupTest(0, 0, 0b11111111, 0, "not cx");
			processNot();
			assertRegisters(0, 0, 0, 0);
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessJmp() {
		try {
			setupTest(0, 0, 0, 0, "jmp test_label");
			processJmp();
			assertRegisters(0, 0, 0, 0);
			assert pc == (short) 111;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessJz() {
		try {
			zero = true;
			setupTest(0, 0, 0, 0, "jz test_label");
			processJz();
			assertRegisters(0, 0, 0, 0);
			assert pc == (short) 111;

			zero = false;
			setupTest(0, 0, 0, 0, "jz test_label");
			processJz();
			assertRegisters(0, 0, 0, 0);
			assert pc == 0;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessJnz() {
		try {
			zero = false;
			setupTest(0, 0, 0, 0, "jnz test_label");
			processJnz();
			assertRegisters(0, 0, 0, 0);
			assert pc == (short) 111;

			zero = true;
			setupTest(0, 0, 0, 0, "jnz test_label");
			processJnz();
			assertRegisters(0, 0, 0, 0);
			assert pc == 0;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessShl() {
		try {
			setupTest(0b01, 4, 0, 0, "shl ax, bx");
			processShl();
			assertRegisters(0b010000, 4, 0, 0);
			assert !zero;

			setupTest(0b01, 20, 0, 0, "shl ax, bx");
			processShl();
			assertRegisters(0, 20, 0, 0);
			assert zero;

			setupTest(0, 2, 0, 0, "shl ax, bx");
			processShl();
			assertRegisters(0, 2, 0, 0);
			assert zero;

			setupTest(0b01, 20, 0, 0, "shl ax, 1");
			processShl();
			assertRegisters(0b010, 20, 0, 0);
			assert !zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessShr() {

		try {
			setupTest(0b10000000, 1, 0, 0, "shr ax, bx");
			processShr();
			assertRegisters(0b01000000, 1, 0, 0);
			assert !zero;

			setupTest(0b10000000, 100, 0, 0, "shr ax, bx");
			processShr();
			assertRegisters(0, 100, 0, 0);
			assert zero;

			setupTest(0xff, 8, 0, 0, "shr ax, bx");
			processShr();
			assertRegisters(0, 8, 0, 0);
			assert zero;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void testProcessPushPop() {
		try {
			reset();
			registers[Register.AX.ordinal()] = 30;
			registers[Register.BX.ordinal()] = 0;
			instruction = InstructionUtil.parseLine("push ax", new ArrayList<Label>(), (short) 0);
			processPush();

			assert sp == 0;
			assert stack[0] == (byte) 30;

			processPush();

			assert sp == 1;
			assert stack[1] == (byte) 30;

			instruction = InstructionUtil.parseLine("pop bx", new ArrayList<Label>(), (short) 0);
			processPop();
			assert sp == 0;
			assert registers[Register.BX.ordinal()] == (byte) 30;

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private void assertRegisters(int ax, int bx, int cx, int dx) {
		assert registers[Register.AX.ordinal()] == (byte) ax;
		assert registers[Register.BX.ordinal()] == (byte) bx;
		assert registers[Register.CX.ordinal()] == (byte) cx;
		assert registers[Register.DX.ordinal()] == (byte) dx;
	}

	private void setupTest(int ax, int bx, int cx, int dx, String line) throws ParseException {
		reset();
		labels = new ArrayList<>();
		labels.add(new Label((short) 111, "test_label"));
		registers[Register.AX.ordinal()] = (byte) ax;
		registers[Register.BX.ordinal()] = (byte) bx;
		registers[Register.CX.ordinal()] = (byte) cx;
		registers[Register.DX.ordinal()] = (byte) dx;
		instruction = InstructionUtil.parseLine(line, labels, (short) 0);
	}
}
