package net.torocraft.minecoprocessors.processor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.util.ByteUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.Label;
import net.torocraft.minecoprocessors.util.ParseException;

//latch pins

public class Processor implements IProcessor {

  private static final String NBT_STACK = "stack";
  private static final String NBT_REGISTERS = "registers";
  private static final String NBT_PROGRAM = "program";
  private static final String NBT_LABELS = "labels";
  private static final String NBT_FLAGS = "flags";
  private static final String NBT_ERROR = "error";

  /*
   * program
   */
  List<Label> labels = new ArrayList<>();
  List<byte[]> program = new ArrayList<>();

  /*
   * state
   */
  byte[] instruction;
  protected byte[] stack = new byte[64];
  byte[] registers = new byte[Register.values().length];
  float temp;

  /*
   * pointers
   */
  short ip;
  byte sp;

  /*
   * flags
   */
  boolean fault;
  boolean zero;
  boolean overflow;
  private boolean carry;
  private boolean wait;

  /*
   * tmp
   */
  private boolean step;
  private String error;
  
  @SuppressWarnings("unused")
  private byte prevTemp;
  private float tempVelocity;
  private float tempAcc;

  void flush() {
    reset();
    stack = new byte[Register.values().length];

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

  public static void reset(int[] a) {
    for (int i = 0; i < a.length; i++) {
      a[i] = 0;
    }
  }

  @Override
  public void reset() {
    fault = false;
    zero = false;
    overflow = false;
    carry = false;
    wait = false;
    step = false;
    error = null;
    ip = 0;
    sp = 0;
    registers = new byte[Register.values().length];
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
      if (file != null) {
        program = InstructionUtil.parseFile(file, labels);
      } else {
        program = new ArrayList<>();
        labels = new ArrayList<>();
      }
    } catch (ParseException e) {
      error = e.getMessage();
      fault = true;
    }
  }

  long packFlags() {
    long flags = 0;
    flags = ByteUtil.setShort(flags, ip, 3);
    flags = ByteUtil.setByteInLong(flags, sp, 5);
    flags = ByteUtil.setByteInLong(flags, getTemp(), 4);
    flags = ByteUtil.setBitInLong(flags, fault, 0);
    flags = ByteUtil.setBitInLong(flags, zero, 1);
    flags = ByteUtil.setBitInLong(flags, overflow, 2);
    flags = ByteUtil.setBitInLong(flags, carry, 3);
    flags = ByteUtil.setBitInLong(flags, wait, 4);
    return flags;
  }

  void unPackFlags(long flags) {
    ip = ByteUtil.getShort(flags, 3);
    sp = ByteUtil.getByteInLong(flags, 5);
    temp = ByteUtil.getByteInLong(flags, 4);
    fault = ByteUtil.getBitInLong(flags, 0);
    zero = ByteUtil.getBitInLong(flags, 1);
    overflow = ByteUtil.getBitInLong(flags, 2);
    carry = ByteUtil.getBitInLong(flags, 3);
    wait = ByteUtil.getBitInLong(flags, 4);
  }

  private static void copy(byte[] a, byte[] b) {
    if (a.length != b.length) {
      new RuntimeException("WARNING: copying different sized a[" + a.length + "] b[" + b.length + "]").printStackTrace();
    }
    // FIXME replace with System.arraycopy() ?
    for (int i = 0; i < Math.min(a.length, b.length); i++) {
      a[i] = b[i];
    }
  }

  private static byte[] addRegistersIfMissing(byte[] registersIn) {
    if (registersIn.length >= Register.values().length) {
     return registersIn;
    }

    byte[] registersNew = new byte[Register.values().length];
    // FIXME replace with System.arraycopy() ?
    for (int i = 0; i < registersNew.length && i < registersIn.length; i++) {
      registersNew[i] = registersIn[i];
    }
    return registersNew;
  }

  @Override
  public void readFromNBT(NBTTagCompound c) {
    stack = c.getByteArray(NBT_STACK);
    registers = addRegistersIfMissing(c.getByteArray(NBT_REGISTERS));

    unPackFlags(c.getLong(NBT_FLAGS));

    error = c.getString(NBT_ERROR);
    if (error != null && error.isEmpty()) {
      error = null;
    }

    program = new ArrayList<>();
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
    if (error != null) {
      c.setString(NBT_ERROR, error);
    }
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

  @SuppressWarnings("unused")
  private void tempUpdate() {
    tempVelocity += tempAcc;

    if (tempVelocity > 0.2f) {
      tempVelocity = 0.2f;
    } else if (tempVelocity < -0.1f) {
      tempVelocity = -0.1f;
    }

    if (tempVelocity != 0) {
      temp = temp + tempVelocity;
    }

    if (temp > 125f) {
      temp = 125f;
    } else if (temp < 0) {
      temp = 0f;
    }

  }

  @SuppressWarnings("unused")
  private void coolCycle() {
    if (temp == 0) {
      tempAcc = 0;
      return;
    }
    if (temp > 0) {
      tempAcc = -0.003f;
    } else if (temp < 0) {
      tempAcc = 0;
      temp = 0;
      tempVelocity = 0;
    }
  }


  @SuppressWarnings("unused")
  private void heatCycle() {
    tempAcc = 0.002f;
  }


  /**
   * returns true if GUI should be updated after this tick
   */
  @Override
  public boolean tick() {
    //tempUpdate();
    //coolCycle();

    if (fault || (wait && !step)) {
     // boolean cooled = prevTemp != getTemp();
      //prevTemp = getTemp();
      ///return cooled;
      return false;
    }
    step = false;

    try {
      process();
    } catch (Exception e) {
      Minecoprocessors.proxy.handleUnexpectedException(e);
      error = getInstructionString();
      fault = true;
    }
    //prevTemp = getTemp();
    return true;
  }

  private String getInstructionString() {
    try{
      return InstructionUtil.compileLine(instruction, labels, ip);
    } catch(Exception e) {
      Minecoprocessors.proxy.handleUnexpectedException(e);
      return "??";
    }
  }

  private void process() {

    if (ip >= program.size()) {
      fault = true;
      return;
    }

    if (ip < 0) {
      ip = 0;
    }

    instruction = program.get(ip);

    // System.out.println(pinchDump());

    //heatCycle();

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

  void processMov() {
    registers[instruction[1]] = getVariableOperand(1);
  }

  void processAdd() {
    int a = getVariableOperand(0);
    int b = getVariableOperand(1);
    int z = a + b;
    testOverflow(z);
    zero = z == 0;
    registers[instruction[1]] = (byte) z;
  }

  void processAnd() {
    byte a = getVariableOperand(0);
    byte b = getVariableOperand(1);
    byte z = (byte) (a & b);
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processXor() {
    byte a = getVariableOperand(0);
    byte b = getVariableOperand(1);
    byte z = (byte) (a ^ b);
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processOr() {
    byte a = getVariableOperand(0);
    byte b = getVariableOperand(1);
    byte z = (byte) (a | b);
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processNot() {
    byte a = getVariableOperand(0);
    byte z = (byte) ~a;
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processSub() {
    int a = getVariableOperand(0);
    int b = getVariableOperand(1);
    int z = a - b;
    testOverflow(z);
    zero = z == 0;
    registers[instruction[1]] = (byte) z;
  }

  void processCmp() {
    int a = getVariableOperand(0);
    int b = getVariableOperand(1);
    int z = a - b;
    testOverflow(z);
    zero = z == 0;
  }

  void processShl() {
    byte a = getVariableOperand(0);
    byte b = getVariableOperand(1);
    if (b > 8) {
      b = 8;
    }
    byte z = (byte) (a << b);
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processShr() {
    int a = getVariableOperand(0) & 0x00ff;
    int b = getVariableOperand(1) & 0x00ff;
    if (b > 8) {
      b = 8;
    }
    byte z = (byte) (a >>> b);
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processWfe() {
    wait = true;
  }

  void processJmp() {
    ip = labels.get(instruction[1]).address;
  }

  void processJz() {
    if (zero) {
      processJmp();
    }
  }

  void processJnz() {
    if (!zero) {
      processJmp();
    }
  }

  void processPush() {
    if (sp >= stack.length) {
      fault = true;
      return;
    }
    byte a = getVariableOperand(0);
    stack[sp++] = a;
  }

  void processPop() {
    if (sp <= 0) {
      fault = true;
      return;
    }
    registers[instruction[1]] = stack[--sp];
  }

  void processCall() {
    stack[sp++] = ByteUtil.getByteInShort(ip, 0);
    stack[sp++] = ByteUtil.getByteInShort(ip, 1);
    ip = labels.get(instruction[1]).address;
  }

  void processRet() {
    if (sp <= 1) {
      fault = true;
      error = "ret";
      return;
    }
    ip = ByteUtil.setByteInShort(ip, stack[--sp], 1);
    ip = ByteUtil.setByteInShort(ip, stack[--sp], 0);
  }

  void processInc() {
    int a = getVariableOperand(0);
    int z = a + 1;
    zero = z == 0;
    registers[instruction[1]] = (byte) z;
  }

  void processDec() {
    int a = getVariableOperand(0);
    int z = a - 1;
    zero = z == 0;
    registers[instruction[1]] = (byte) z;
  }

  void testOverflow(int z) {
    overflow = z != (byte) z;
  }

  void testOverflow(long z) {
    overflow = z != (byte) z;
  }

  void processMul() {
    int a = registers[Register.A.ordinal()];
    int b = getVariableOperand(0);
    long z = a * b;
    zero = z == 0;
    testOverflow(z);
    registers[Register.A.ordinal()] = (byte) z;
  }

  void processDiv() {
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

  public boolean isFault() {
    return fault;
  }

  private static String pad(String s) {
    if (s.length() == 1) {
      return "0" + s;
    }
    return s;
  }

  private static String fix(String s) {
    if (s.length() > 2) {
      return s.substring(s.length() - 2, s.length());
    }
    return s;
  }

  private void dumpRegister(StringBuilder s, Register reg) {

    // s.append(reg.toString().toLowerCase());
    // s.append("[");
    s.append(fix(pad(Integer.toUnsignedString(registers[reg.ordinal()], 16))));
    // s.append("] ");
  }

  String pinchDump() {
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

    s.append("  (").append(InstructionUtil.compileLine(instruction, labels, (short) -1)).append(") ");

    if (fault) {
      s.append("FAULT ");
    }
    if (zero) {
      s.append("ZF ");
    }
    if (wait) {
      s.append("WAIT ");
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

  public List<byte[]> getProgram() {
    return program;
  }

  public byte getTemp() {
    return (byte) Math.round(temp);
  }

  public short getIp() {
    return ip;
  }

  public byte getSp() {
    return sp;
  }

  public boolean isZero() {
    return zero;
  }

  public boolean isOverflow() {
    return overflow;
  }

  public boolean isCarry() {
    return carry;
  }

  public boolean isWait() {
    return wait;
  }

  public void setWait(boolean wait) {
    this.wait = wait;
  }

  public List<Label> getLabels() {
    return labels;
  }

  public void setStep(boolean step) {
    this.step = step;
  }

  public String getError() {
    return error;
  }

  public boolean isHot() {
    return temp > 120;
  }

}
