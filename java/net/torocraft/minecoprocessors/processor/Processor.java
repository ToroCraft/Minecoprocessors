package net.torocraft.minecoprocessors.processor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.torocraft.minecoprocessors.util.ByteUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.Label;
import net.torocraft.minecoprocessors.util.ParseException;

//TODO change block state to show if the proc is running or halted


@SuppressWarnings({"rawtypes", "unchecked"})
public class Processor implements IProcessor {

  private static final String NBT_STACK = "stack";
  private static final String NBT_REGISTERS = "registers";
  private static final String NBT_PROGRAM = "program";
  private static final String NBT_LABELS = "labels";
  private static final String NBT_FLAGS = "flags";

  /*
   * program
   */
  private List<Label> labels = new ArrayList<>();
  private List program = new ArrayList();

  /*
   * state
   */
  private byte[] instruction;
  private final byte[] stack = new byte[64];
  private final byte[] registers = new byte[Register.values().length];
  private byte temp;

  /*
   * pointers
   */
  private short ip;
  private byte sp;

  /*
   * flags
   */
  private boolean fault;
  private boolean zero;
  private boolean overflow;
  private boolean carry;
  private boolean wait;

  private void flush() {
    reset();
    reset(stack);

    labels.clear();
    program.clear();
  }

  // TODO move to util class
  public static void reset(byte[] a) {
    for (int i = 0; i < a.length; i++) {
      a[i] = 0;
    }
  }

  // TODO move to util class
  public static void reset(boolean[] a) {
    for (int i = 0; i < a.length; i++) {
      a[i] = false;
    }
  }

  @Override
  public void reset() {
    fault = false;
    zero = false;
    overflow = false;
    carry = false;
    wait = false;
    ip = 0;
    sp = 0;
    reset(registers);
    registers[Register.PORTS.ordinal()] = (byte) 0xb1110;
  }

  @Override
  public void wake() {
    wait = false;
  }

  @Override
  public void load(String file) {
    try {
      flush();
      program = InstructionUtil.parseFile(file, labels);
    } catch (ParseException e) {
      e.printStackTrace();
      fault = true;
    }
  }

  private long packFlags() {
    long flags = 0;
    flags = ByteUtil.setShort(flags, ip, 3);
    flags = ByteUtil.setByteInLong(flags, sp, 5);
    flags = ByteUtil.setByteInLong(flags, temp, 4);
    flags = ByteUtil.setBitInLong(flags, fault, 0);
    flags = ByteUtil.setBitInLong(flags, zero, 1);
    flags = ByteUtil.setBitInLong(flags, overflow, 2);
    flags = ByteUtil.setBitInLong(flags, carry, 3);
    flags = ByteUtil.setBitInLong(flags, wait, 4);
    return flags;
  }

  private void testPackFlags() {
    reset();
    temp = (byte) 0xff;
    assert packFlags() == Long.parseUnsignedLong("000000ff00000000", 16);
    fault = true;
    temp = 0;
    assert packFlags() == Long.parseUnsignedLong("0000000000000001", 16);
    zero = true;
    assert packFlags() == Long.parseUnsignedLong("0000000000000003", 16);
    overflow = true;
    assert packFlags() == Long.parseUnsignedLong("0000000000000007", 16);
    sp = (byte) 0xee;
    ip = (short) 0xabcd;
    assert packFlags() == Long.parseUnsignedLong("abcdee0000000007", 16);
  }

  private void unPackFlags(long flags) {
    ip = ByteUtil.getShort(flags, 3);
    sp = ByteUtil.getByteInLong(flags, 5);
    temp = ByteUtil.getByteInLong(flags, 4);
    fault = ByteUtil.getBitInLong(flags, 0);
    zero = ByteUtil.getBitInLong(flags, 1);
    overflow = ByteUtil.getBitInLong(flags, 2);
    carry = ByteUtil.getBitInLong(flags, 3);
    wait = ByteUtil.getBitInLong(flags, 4);
  }

  private void testUnpackFlags() {
    reset();
    unPackFlags(Long.parseUnsignedLong("abcdee0000000007", 16));
    assert sp == (byte) 0xee;
    assert ip == (short) 0xabcd;
    assert zero;
    assert overflow;
    assert fault;

    unPackFlags(Long.parseUnsignedLong("0000ff0000000000", 16));
    assert sp == -1;
    assert ip == 0;
    assert !zero;
    assert !overflow;
    assert !fault;
  }

  private static void copy(byte[] a, byte[] b) {
    if (a.length != b.length) {
      new RuntimeException(
          "WARNING: copying different sized a[" + a.length + "] b[" + b.length + "]")
          .printStackTrace();
    }
    for (int i = 0; i < Math.min(a.length, b.length); i++) {
      a[i] = b[i];
    }
  }

  @Override
  public void readFromNBT(NBTTagCompound c) {
    copy(stack, c.getByteArray(NBT_STACK));
    copy(registers, c.getByteArray(NBT_REGISTERS));
    unPackFlags(c.getLong(NBT_FLAGS));

    program = new ArrayList();
    NBTTagList programTag = (NBTTagList) c.getTag(NBT_PROGRAM);
    if (programTag != null) {
      for (int i = 0; i < programTag.tagCount(); i++) {
        program.add(((NBTTagByteArray) programTag.get(i)).getByteArray());
      }
    }

    labels = new ArrayList<>();
    NBTTagList labelTag = (NBTTagList) c.getTag(NBT_LABELS);
    if (labelTag != null) {
      for (int i = 0; i < labelTag.tagCount(); i++) {
        labels.add(Label.fromNbt((NBTTagCompound) labelTag.get(i)));
      }
    }
  }

  @Override
  public NBTTagCompound writeToNBT() {
    NBTTagCompound c = new NBTTagCompound();
    c.setByteArray(NBT_STACK, stack);
    c.setByteArray(NBT_REGISTERS, registers);
    c.setLong(NBT_FLAGS, packFlags());

    NBTTagList programTag = new NBTTagList();
    for (Object b : program) {
      programTag.appendTag(new NBTTagByteArray((byte[]) b));
    }
    c.setTag(NBT_PROGRAM, programTag);

    NBTTagList labelTag = new NBTTagList();
    for (Label label : labels) {
      labelTag.appendTag(label.toNbt());
    }
    c.setTag(NBT_LABELS, labelTag);

    return c;
  }

  @Override
  public void tick() {
    if (temp > 1) {
      temp--;
    }
    if (fault || wait) {
      return;
    }
    process();
  }

  private void process() {

    if (ip >= program.size()) {
      fault = true;
      return;
    }

    if (ip < 0) {
      ip = 0;
    }

    instruction = (byte[]) program.get(ip);

    System.out.println(pinchDump());

    if (temp < 110) {
      temp += 2;
    }

    ip++;

    switch (InstructionCode.values()[instruction[0]]) {
      case ADD:
        processAdd();
        return;
      case AND:
        processAnd();
        return;
      case CALL:
        processCall();
        return;
      case CMP:
        processCmp();
        return;
      case DIV:
        processDiv();
        return;
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
        processMul();
        return;
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
        processRet();
        return;
      case NOP:
        return;
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
      case WFE:
        processWfe();
        return;
      case INT:
        return;
      case INC:
        processInc();
        return;
      case DEC:
        processDec();
        return;
      default:
        break;
    }

    fault = true;
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

  private void processWfe() {
    wait = true;
  }

  private void processJmp() {
    ip = labels.get(instruction[1]).address;
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
    if (sp >= stack.length) {
      fault = true;
      return;
    }
    byte a = getVariableOperand(0);
    stack[sp++] = a;
  }

  private void processPop() {
    if (sp <= 0) {
      fault = true;
      return;
    }
    registers[instruction[1]] = stack[--sp];
  }

  private void processCall() {
    stack[sp++] = ByteUtil.getByteInShort(ip, 0);
    stack[sp++] = ByteUtil.getByteInShort(ip, 1);
    ip = labels.get(instruction[1]).address;
  }

  private void processRet() {
    ip = ByteUtil.setByteInShort(ip, stack[--sp], 1);
    ip = ByteUtil.setByteInShort(ip, stack[--sp], 0);
  }

  private void testProcessCall() {
    try {
      setupTest(0, 0, 0, 0, "call test_label");
      ip = (short) 0xabcd;
      processCall();
      assertRegisters(0, 0, 0, 0);

      assert stack[0] == (byte) 0xcd;
      assert stack[1] == (byte) 0xab;

      assert ip == (short) 111;
      assert sp == 2;

      processRet();

      assert ip == (short) 0xabcd;
      assert sp == 0;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void processInc() {
    int a = getVariableOperand(0);
    int z = a + 1;
    zero = z == 0;
    registers[instruction[1]] = (byte) z;
  }

  private void testProcessInc() {
    try {
      setupTest(10, 0, 0, 0, "inc a");
      processInc();
      assertRegisters(11, 0, 0, 0);
      assert !zero;

      setupTest(-1, 0, 0, 0, "inc a");
      processInc();
      assertRegisters(0, 0, 0, 0);
      assert zero;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void processDec() {
    int a = getVariableOperand(0);
    int z = a - 1;
    zero = z == 0;
    registers[instruction[1]] = (byte) z;
  }

  private void testProcessDec() {
    try {
      setupTest(10, 0, 0, 0, "dec a");
      processDec();
      assertRegisters(9, 0, 0, 0);
      assert !zero;

      setupTest(1, 0, 0, 0, "dec a");
      processDec();
      assertRegisters(0, 0, 0, 0);
      assert zero;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void processMul() {
    int a = registers[Register.A.ordinal()];
    int b = getVariableOperand(0);
    long z = a * b;
    zero = z == 0;
    testOverflow(z);
    registers[Register.A.ordinal()] = (byte) z;
  }

  private void testProcessMul() {
    try {
      setupTest(0xff, 2, 0, 0, "mul b");
      processMul();
      assertRegisters(0xfe, 2, 0, 0);
      assert !zero;

      setupTest(5, 0, 0, 2, "mul d");
      processMul();
      assertRegisters(10, 0, 0, 2);
      assert !zero;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void processDiv() {
    int a = registers[Register.A.ordinal()];
    int b = getVariableOperand(0);
    if (b == 0) {
      fault = true;
      return;
    }
    long z = a / b;
    zero = z == 0;
    testOverflow(z);
    registers[Register.A.ordinal()] = (byte) z;
  }

  private void testProcessDiv() {
    try {
      setupTest(10, 2, 0, 0, "div b");
      processDiv();
      assertRegisters(5, 2, 0, 0);
      assert !zero;

      setupTest(5, 0, 0, 2, "div d");
      processDiv();
      assertRegisters(2, 0, 0, 2);

      setupTest(5, 0, 0, 0, "div d");
      processDiv();
      assertRegisters(5, 0, 0, 0);
      assert !zero;
      assert fault;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testOverflow(int z) {
    overflow = z != (int) (byte) z;
  }

  private void testOverflow(long z) {
    overflow = z != (long) (byte) z;
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
    testCopyArray();
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
    testPackFlags();
    testUnpackFlags();
    testProcessCall();
    testProcessInc();
    testProcessDec();
    testProcessMul();
    testProcessDiv();
    // testNbt();
  }

  private void testCopyArray() {
    byte[] a = {1, 2, 3, 4, 5, 6};
    byte[] b = new byte[a.length];
    copy(b, a);
    assert (b[0] == 1);
    assert (b[5] == 6);
  }

  @SuppressWarnings("unused")
  private void testNbt() {
    // TODO find a better way to test NBT, it doesn't seem to be working
    // well in this test case
    flush();
    labels.add(new Label((short) 189, "foobar"));
    program.add(new byte[]{0x00, 0x01, 0x02, 0x03});
    stack[0] = (byte) 0x99;
    registers[0] = (byte) 0xee;
    registers[4] = (byte) 0xcc;
    zero = true;

    for (byte b : registers) {
      System.out.println("B:" + b);
    }

    NBTTagCompound c = writeToNBT();

    flush();
    assert !zero;
    assert labels.size() == 0;
    assert program.size() == 0;
    reset(registers);

    readFromNBT(c);

    assert zero;
    assert labels.size() == 1;
    assert labels.get(0).address == (short) 189;
    assert labels.get(0).name.equals("foobar");
    assert program.size() == 1;
    byte[] instruction = (byte[]) program.get(0);
    assert instruction[0] == 0x00;
    assert instruction[1] == 0x01;
    assert instruction[2] == 0x02;
    assert instruction[3] == 0x03;

    for (byte b : registers) {
      System.out.println("A:" + b);
    }

    assert registers[0] == (byte) 0xee;
    assert registers[4] == (byte) 0xcc;

  }

  // TODO test copy and reset array methods

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
      setupTest(0, 30, 0, 0, "mov a, b");
      processMov();
      assertRegisters(30, 30, 0, 0);

      setupTest(0, 30, 0, 0, "mov a, 51");
      processMov();
      assertRegisters(51, 30, 0, 0);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessAdd() {
    try {
      setupTest(3, 30, 0, 0, "add a, b");
      processAdd();
      assertRegisters(33, 30, 0, 0);
      assert !overflow;
      assert !zero;

      setupTest(11, 0, 0, 0, "add a, 51");
      processAdd();
      assertRegisters(62, 0, 0, 0);
      assert !overflow;
      assert !zero;

      setupTest(-51, 0, 0, 0, "add a, 51");
      processAdd();
      assertRegisters(0, 0, 0, 0);
      assert !overflow;
      assert zero;

      setupTest(130, 0, 130, 0, "add a, c");
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
      setupTest(50, 0, 0, 10, "sub a, d");
      processSub();
      assertRegisters(40, 0, 0, 10);
      assert !overflow;
      assert !zero;

      setupTest(-130, 0, 0, 130, "sub a, d");
      processSub();
      assertRegisters(-4, 0, 0, 130);
      assert overflow;
      assert !zero;

      setupTest(130, 0, 0, 130, "sub a, d");
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
      setupTest(50, 0, 0, 10, "cmp a, d");
      processCmp();
      assertRegisters(50, 0, 0, 10);
      assert !overflow;
      assert !zero;

      setupTest(-130, 0, 0, 130, "cmp a, d");
      processCmp();
      assertRegisters(-130, 0, 0, 130);
      assert overflow;
      assert !zero;

      setupTest(130, 0, 0, 130, "cmp a, d");
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
      setupTest(0b011111, 0b010, 0, 0, "and a, b");
      processAnd();
      assertRegisters(0b010, 0b010, 0, 0);
      assert !zero;

      setupTest(0b011101, 0b010, 0, 0, "and a, b");
      processAnd();
      assertRegisters(0, 0b010, 0, 0);
      assert zero;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessXor() {
    try {
      setupTest(0b0101, 0b0110, 0, 0, "xor a, b");
      processXor();
      assertRegisters(0b011, 0b0110, 0, 0);
      assert !zero;

      setupTest(0b0101, 0b0101, 0, 0, "xor a, b");
      processXor();
      assertRegisters(0, 0b0101, 0, 0);
      assert zero;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessOr() {
    try {
      setupTest(0b01000, 0, 0, 0b0111, "or d, a");
      processOr();
      assertRegisters(0b01000, 0, 0, 0b01111);
      assert !zero;

      setupTest(0, 0, 0, 0, "or d, a");
      processOr();
      assertRegisters(0, 0, 0, 0);
      assert zero;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessNot() {
    try {
      setupTest(0, 0, 0b1010, 0, "not c");
      processNot();
      assertRegisters(0, 0, 0b11110101, 0);
      assert !zero;

      setupTest(0, 0, 0b11111111, 0, "not c");
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
      assert ip == (short) 111;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessJz() {
    try {

      setupTest(0, 0, 0, 0, "jz test_label");
      zero = true;
      processJz();
      assertRegisters(0, 0, 0, 0);
      assert ip == (short) 111;

      setupTest(0, 0, 0, 0, "jz test_label");
      zero = false;
      processJz();
      assertRegisters(0, 0, 0, 0);
      assert ip == 0;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessJnz() {
    try {
      setupTest(0, 0, 0, 0, "jnz test_label");
      zero = false;
      processJnz();
      assertRegisters(0, 0, 0, 0);
      assert ip == (short) 111;

      setupTest(0, 0, 0, 0, "jnz test_label");
      zero = true;
      processJnz();
      assertRegisters(0, 0, 0, 0);
      assert ip == 0;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessShl() {
    try {
      setupTest(0b01, 4, 0, 0, "shl a, b");
      processShl();
      assertRegisters(0b010000, 4, 0, 0);
      assert !zero;

      setupTest(0b01, 20, 0, 0, "shl a, b");
      processShl();
      assertRegisters(0, 20, 0, 0);
      assert zero;

      setupTest(0, 2, 0, 0, "shl a, b");
      processShl();
      assertRegisters(0, 2, 0, 0);
      assert zero;

      setupTest(0b01, 20, 0, 0, "shl a, 1");
      processShl();
      assertRegisters(0b010, 20, 0, 0);
      assert !zero;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void testProcessShr() {

    try {
      setupTest(0b10000000, 1, 0, 0, "shr a, b");
      processShr();
      assertRegisters(0b01000000, 1, 0, 0);
      assert !zero;

      setupTest(0b10000000, 100, 0, 0, "shr a, b");
      processShr();
      assertRegisters(0, 100, 0, 0);
      assert zero;

      setupTest(0xff, 8, 0, 0, "shr a, b");
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
      registers[Register.A.ordinal()] = 30;
      registers[Register.B.ordinal()] = 0;
      instruction = InstructionUtil.parseLine("push a", new ArrayList<Label>(), (short) 0);
      processPush();

      assert sp == 1;
      assert stack[0] == (byte) 30;

      processPush();

      assert sp == 2;
      assert stack[1] == (byte) 30;

      instruction = InstructionUtil.parseLine("pop b", new ArrayList<Label>(), (short) 0);
      processPop();
      assert sp == 1;
      assert registers[Register.B.ordinal()] == (byte) 30;

    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void assertRegisters(int ax, int bx, int cx, int dx) {
    assert registers[Register.A.ordinal()] == (byte) ax;
    assert registers[Register.B.ordinal()] == (byte) bx;
    assert registers[Register.C.ordinal()] == (byte) cx;
    assert registers[Register.D.ordinal()] == (byte) dx;
  }

  private void setupTest(int ax, int bx, int cx, int dx, String line) throws ParseException {
    reset();
    labels = new ArrayList<>();
    labels.add(new Label((short) 111, "test_label"));
    registers[Register.A.ordinal()] = (byte) ax;
    registers[Register.B.ordinal()] = (byte) bx;
    registers[Register.C.ordinal()] = (byte) cx;
    registers[Register.D.ordinal()] = (byte) dx;
    instruction = InstructionUtil.parseLine(line, labels, (short) 0);
  }

  public boolean isFault() {
    return fault;
  }

  private String pad(String s) {
    if (s.length() == 1) {
      return "0" + s;
    }
    return s;
  }

  private void dumpRegister(StringBuilder s, Register reg) {

    // s.append(reg.toString().toLowerCase());
    // s.append("[");
    s.append(pad(Integer.toUnsignedString(registers[reg.ordinal()], 16)));
    // s.append("] ");
  }

  public String pinchDump() {
    StringBuilder s = new StringBuilder();

    s.append("a|b|c|d[");
    dumpRegister(s, Register.A);
    s.append(" ");
    dumpRegister(s, Register.B);
    s.append(" ");
    dumpRegister(s, Register.C);
    s.append(" ");
    dumpRegister(s, Register.D);
    s.append("]  ");

    s.append("f|b|l|r[");
    dumpRegister(s, Register.PF);
    s.append(" ");
    dumpRegister(s, Register.PB);
    s.append(" ");
    dumpRegister(s, Register.PL);
    s.append(" ");
    dumpRegister(s, Register.PR);
    s.append("]   ");

    dumpRegister(s, Register.PORTS);

    s.append(" ").append(temp).append("°F ");

    s.append("  (").append(InstructionUtil.compileLine(instruction, labels, (short) -1))
        .append(") ");

    if (fault) {
      s.append("FAULT ");
    }
    if (zero) {
      s.append("ZF ");
    }
    if (overflow) {
      s.append("OF ");
    }

    return s.toString();
  }

  @Override
  public byte[] getRegisters() {
    return registers;
  }

}
