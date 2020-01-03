package net.torocraft.minecoprocessors.processor;

import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.torocraft.minecoprocessors.util.ByteUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.Label;
import net.torocraft.minecoprocessors.util.ParseException;

import java.util.ArrayList;
import java.util.List;


public class Processor
{
  private static final int MEMORY_SIZE = 64;

  /*
   * program
   */
  List<Label> labels = new ArrayList<>();
  List<byte[]> program = new ArrayList<>();

  /*
   * state
   */
  byte[] instruction;
  protected byte[] stack = new byte[MEMORY_SIZE];
  byte[] registers = new byte[Register.values().length];

  /**
    * Fault Code status
    * Contains the fault code for various processor fault states like stack underflow, div by zero, and so on.
    *
    * Inaccessible by user code.
    *
    * <ul><li>
    *     <b>0x00:</b> Division by zero.
    *     <b>0x01:</b> Stack underflow.
    *     <b>0x02:</b> Stack overflow.
    *     <b>0x03:</b> Undefined IP.
    *     <b>0x04:</b> Code error.
    *     <b>0x05:</b> Out of bounds error.
    *     <b>0xFE:</b> Stopped by HLT instruction.
    *     <b>0xFF:</b> Normal operation.
    * </li></ul>
    */
  byte faultCode = FaultCode.FAULT_STATE_NOMINAL; // initial state

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
  boolean carry;
  boolean wait;

  /*
   * tmp
   */
  private boolean step;
  private String error;

  void flush() {
    reset();
    stack = new byte[MEMORY_SIZE];

    labels.clear();
    program.clear();
  }

  public void reset() {
    fault = false;
    zero = false;
    overflow = false;
    carry = false;
    wait = false;
    step = false;
    error = "";
    ip = 0;
    sp = 0;
    registers = new byte[Register.values().length];
    registers[Register.PORTS.ordinal()] = (byte) 0xb1110;
    faultCode = FaultCode.FAULT_STATE_NOMINAL;
  }

  public void wake() {
    wait = false;
  }

  public boolean load(List<String> file) {
    try {
      flush();
      if (file != null) {
        program = InstructionUtil.parseFile(file, labels);
        return (!program.isEmpty());
      } else {
        program = new ArrayList<>();
        labels = new ArrayList<>();
      }
    } catch (ParseException e) {
      error = e.getMessage();
      faultCode = FaultCode.FAULT_UNKNOWN_OPCODE;
      fault = true;
    }
    return false;
  }

  long packFlags() {
    long flags = 0;
    flags = ByteUtil.setShort(flags, ip, 3);
    flags = ByteUtil.setByte(flags, sp, 5);
    // byte 4  not currently used
    flags = ByteUtil.setBit(flags, fault, 0);
    flags = ByteUtil.setBit(flags, zero, 1);
    flags = ByteUtil.setBit(flags, overflow, 2);
    flags = ByteUtil.setBit(flags, carry, 3);
    flags = ByteUtil.setBit(flags, wait, 4);
    return flags;
  }

  void unPackFlags(long flags) {
    ip = ByteUtil.getShort(flags, 3);
    sp = ByteUtil.getByte(flags, 5);
    // byte 4 not currently used
    fault = ByteUtil.getBit(flags, 0);
    zero = ByteUtil.getBit(flags, 1);
    overflow = ByteUtil.getBit(flags, 2);
    carry = ByteUtil.getBit(flags, 3);
    wait = ByteUtil.getBit(flags, 4);
  }

  private static byte[] addRegistersIfMissing(byte[] registersIn) {
    if (registersIn.length >= Register.values().length) {
      return registersIn;
    }

    byte[] registersNew = new byte[Register.values().length];
    System.arraycopy(registersIn, 0, registersNew, 0, registersIn.length);
    return registersNew;
  }


  public void setNBT(CompoundNBT nbt)
  {
    stack = nbt.getByteArray("stack");
    registers = addRegistersIfMissing(nbt.getByteArray("registers"));
    faultCode = nbt.getByte("faultCode");
    unPackFlags(nbt.getLong("flags"));
    error = nbt.getString("error");
    program = new ArrayList<>();
    ListNBT programTag = (ListNBT) nbt.get("program");
    if(programTag != null) {
      for(INBT tag: programTag) {
        program.add(((ByteArrayNBT)tag).getByteArray());
      }
    }
    labels = new ArrayList<>();
    ListNBT labelTag = (ListNBT) nbt.get("labels");
    if(labelTag != null) {
      for(INBT tag : labelTag) {
        labels.add(Label.fromNbt((CompoundNBT) tag));
      }
    }
  }

  public CompoundNBT getNBT()
  {
    CompoundNBT nbt = new CompoundNBT();
    nbt.putByteArray("stack", stack);
    nbt.putByteArray("registers", registers);
    nbt.putByte("faultCode", faultCode);
    nbt.putLong("flags", packFlags());
    if((error!=null) && (!error.isEmpty())) nbt.putString("error", error);
    ListNBT programTag = new ListNBT();
    for(byte[] b: program) programTag.add(new ByteArrayNBT(b));
    nbt.put("program", programTag);
    ListNBT labelTag = new ListNBT();
    for (Label label : labels) labelTag.add(label.toNbt());
    nbt.put("labels", labelTag);
    return nbt;
  }

  /**
   * returns true if GUI should be updated after this tick
   */
  public boolean tick() {
    if (fault || (wait && !step)) {
      return false;
    }
    step = false;

    try {
      process();
      // TODO handle parse exception (actually make a new exception type to use in a running processor)
    } catch (Exception e) {
      ModMinecoprocessors.proxy.handleUnexpectedException(e);
      error = getInstructionString();
      fault = true;
    }
    return true;
  }

  private String getInstructionString() {
    try {
      return InstructionUtil.compileLine(instruction, labels, ip);
    } catch (Exception e) {
      ModMinecoprocessors.proxy.handleUnexpectedException(e);
      return "??";
    }
  }

  private void process() throws ParseException {

    if (ip >= program.size()) {
      faultCode = FaultCode.FAULT_UNDEFINED_IP;
      fault = true;
      return;
    }

    if (ip < 0) {
      ip = 0;
    }

    instruction = program.get(ip);

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
      case LOOP:
        processJmp();
        return;
      case JNZ:
      case JNE:
        processJnz();
        return;
      case JZ:
      case JE:
        processJz();
        return;
      case JG:
        processJg();
        return;
      case JGE:
        processJge();
        return;
      case JL:
        processJl();
        return;
      case JLE:
        processJle();
        return;
      case MOV:
        processMov();
        return;
      case MUL:
        processMul();
        return;
      case NOP:
      case INT:
        return;
      case NOT:
        processNot();
        return;
      case OR:
        processOr();
        return;
      case POPA:
        processPopAll();
        return;
      case POP:
        processPop();
        return;
      case PUSH:
        processPush();
        return;
      case PUSHA:
        processPushAll();
        return;
      case RET:
        processRet();
        return;
      case SAL:
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
      case INC:
        processInc();
        return;
      case DEC:
        processDec();
        return;
      case DJNZ:
        processDjnz();
        break;
      case JC:
        processJc();
        break;
      case JNC:
        processJnc();
        break;
      case ROR:
        processRor();
        break;
      case ROL:
        processRol();
        break;
      case SAR:
        processSar();
        break;
      case HLT:
        processHlt();
        break;
      case CLZ:
        processClz();
        break;
      case CLC:
        processClc();
        break;
      case SEZ:
        processSez();
        break;
      case SEC:
        processSec();
        break;
      case DUMP:
        processDump();
        break;
      default:
        throw new RuntimeException("InstructionCode enum had unexpected value");
    }
  }

  void processMov() throws ParseException {
    byte source = getVariableOperand(1);
    if (isLabelOperand(instruction, 0)) {
      throw new ParseException(InstructionUtil.compileLine(instruction, labels, (short) 0), InstructionUtil.ERROR_LABEL_IN_FIRST_OPERAND);
    } else if (isMemoryReferenceOperand(instruction, 0)) {
      writeToMemory(source);
    } else {
      registers[instruction[1]] = source;
    }
  }

  private void writeToMemory(byte source) {
    try {
      stack[getVariableOperandNoReference(0) + getMemoryOffset(0)] = source;
    } catch (ArrayIndexOutOfBoundsException e) {
      faultCode = FaultCode.FAULT_OUT_OF_BOUNDS;
      fault = true;
    }
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

    if (a > b) {
      zero = false;
      carry = false;
    } else if (a < b) {
      zero = false;
      carry = true;
    } else if (a == b) {
      zero = true;
      carry = false;
    }
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

  void processSar() {
    int a = getVariableOperand(0);
    byte n = (byte) Math.min(getVariableOperand(1), 8);
    byte z = (byte) (a >> n);
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processRor() {
    int a = getVariableOperand(0) & 0x0ff;
    byte n = (byte) Math.min(getVariableOperand(1), 8);
    byte z = (byte) ((a >>> n) | (a << 8 - n));
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processRol() {
    int a = getVariableOperand(0) & 0x0ff;
    byte n = (byte) Math.min(getVariableOperand(1), 8);
    byte z = (byte) ((a << n) | (a >>> 8 - n));
    zero = z == 0;
    registers[instruction[1]] = z;
  }

  void processWfe() {
    wait = true;
  }

  void processHlt() {
    faultCode = FaultCode.FAULT_HLT_INSTRUCTION;
    fault = true;
  }

  void processClz() {
    zero = false;
  }

  void processClc() {
    carry = false;
  }

  void processSez() {
    zero = true;
  }

  void processSec() {
    carry = true;
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

  void processJc() {
    if (carry) {
      processJmp();
    }
  }

  void processJnc() {
    if (!carry) {
      processJmp();
    }
  }

  void processJg() {
    if (!carry && !zero) {
      processJmp();
    }
  }

  void processJge() {
    if ((!carry && !zero) || (!carry && zero)) {
      processJmp();
    }
  }

  void processJl() {
    if (carry && !zero) {
      processJmp();
    }
  }

  void processJle() {
    if ((carry && zero) || (!carry && zero)) {
      processJmp();
    }
  }

  void processDjnz() {
    processDec();
    byte tmp = instruction[1];
    instruction[1] = instruction[2];
    processJnz();
    instruction[1] = tmp;
  }

  void processPushAll() {
    for (int i = 0; i < 4; i++) {
      if (sp >= stack.length) {
        faultCode = FaultCode.FAULT_STACK_OVERFLOW;
        fault = true;
        return;
      }

      stack[sp++] = registers[i];
    }
  }

  void processPush() {
    if (sp >= stack.length) {
      faultCode = FaultCode.FAULT_STACK_OVERFLOW;
      fault = true;
      return;
    }
    byte a = getVariableOperand(0);
    stack[sp++] = a;
  }

  void processPop() {
    if (sp <= 0) {
      faultCode = FaultCode.FAULT_STACK_UNDERFLOW;
      fault = true;
      return;
    }
    registers[instruction[1]] = stack[--sp];
  }

  void processPopAll() {
    for (int i = 3; i >= 0; i--) {
      if (sp <= 0) {
        faultCode = FaultCode.FAULT_STACK_UNDERFLOW;
        fault = true;
        return;
      }

      registers[i] = stack[--sp];
    }
  }

  void processCall() {
    if (sp >= stack.length - 1) {
      faultCode = FaultCode.FAULT_STACK_OVERFLOW;
      fault = true;
      return;
    }
    stack[sp++] = ByteUtil.getByte(ip, 0);
    stack[sp++] = ByteUtil.getByte(ip, 1);
    ip = labels.get(instruction[1]).address;
  }

  void processRet() {
    if (sp <= 1) {
      faultCode = FaultCode.FAULT_STACK_UNDERFLOW;
      fault = true;
      error = "ret";
      return;
    }
    ip = ByteUtil.setByte(ip, stack[--sp], 1);
    ip = ByteUtil.setByte(ip, stack[--sp], 0);
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
      faultCode = FaultCode.FAULT_DIVISION_BY_ZERO;
      fault = true;
      return;
    }
    long z = a / b;
    zero = z == 0;
    testOverflow(z);
    registers[Register.A.ordinal()] = (byte) z;
  }

  void processDump() {
    System.out.println(coreDump());
  }

  public String coreDump() {
    StringBuilder s = new StringBuilder();

    s.append("Redstone Processor:\n\n");

    s.append(" a  b  c  d    pf pb pl pr\n");
    s.append(" ");
    dumpRegister(s, Register.A);
    s.append(" ");
    dumpRegister(s, Register.B);
    s.append(" ");
    dumpRegister(s, Register.C);
    s.append(" ");
    dumpRegister(s, Register.D);
    s.append("   ");
    dumpRegister(s, Register.PF);
    s.append(" ");
    dumpRegister(s, Register.PB);
    s.append(" ");
    dumpRegister(s, Register.PL);
    s.append(" ");
    dumpRegister(s, Register.PR);
    s.append("\n\n");

    s.append(" ports  adc    ZF CF F  S\n");
    s.append(" ");
    dumpRegister(s, Register.PORTS);
    s.append("     ");
    dumpRegister(s, Register.ADC);

    s.append("     ");
    dumpFlag(s, zero);
    s.append(" ");
    dumpFlag(s, carry);
    s.append(" ");
    dumpFlag(s, fault);
    s.append(" ");
    dumpFlag(s, wait);
    s.append("\n\n");

    s.append(" fault code: ");
    s.append(" ");
    s.append(faultCode);
    s.append("\n\n");

    s.append(" memory\n");
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 8; j++) {
        s.append(" ");


        // @todo: util.toHex, as GuiMinecoprocessor client only Dist
        //  s.append(GuiMinecoprocessor.toHex(stack[(i * 8) + j]));


      }
      s.append("\n");
    }

    return s.toString();
  }

  private void dumpRegister(StringBuilder s, Register reg) {
    s.append(fix(pad(Integer.toUnsignedString(registers[reg.ordinal()], 16))));
  }

  private static void dumpFlag(StringBuilder s, boolean flag) {
    s.append(flag ? "1 " : "0 ");
  }

  public String stateLineDump() {
    return String.format(
      "{ ip:%04x sp:%02x f:%s regs:%02x%02x%02x%02x io:%02x%02x%02x%02x }",
      ip, sp,
      (fault ? "F" : "-") + (zero ? "Z" : "-") + (overflow ? "O" : "-") + (carry ? "C" : "-") + (wait ? "W" : "-"),
      registers[Register.A.ordinal()], registers[Register.B.ordinal()], registers[Register.C.ordinal()], registers[Register.D.ordinal()],
      registers[Register.PF.ordinal()], registers[Register.PB.ordinal()], registers[Register.PL.ordinal()], registers[Register.PR.ordinal()]
    );
  }

  byte getVariableOperand(int operandIndex) {
    if (isLabelOperand(instruction, operandIndex)) {
      return getProgramValueFromLabelOperand(operandIndex);
    }

    byte value = getVariableOperandNoReference(operandIndex);
    if (isMemoryReferenceOperand(instruction, operandIndex)) {
      value = stack[value];
    }
    return value;
  }

  private byte getProgramValueFromLabelOperand(int operandIndex) {
    byte value = instruction[operandIndex + 1];
    short address = labels.get(value).address;
    if (isOffsetOperand(instruction, operandIndex)) {
      address += instruction[4];
    }
    return program.get(address)[1];
  }

  int getMemoryOffset(int operandIndex) {
    if (isOffsetOperand(instruction, operandIndex)) {
      return instruction[4];
    }
    return 0;
  }

  byte getVariableOperandNoReference(int operandIndex) {
    byte value = instruction[operandIndex + 1];
    if (isRegisterOperand(instruction, operandIndex)) {
      value = registers[value];
    }
    return value;
  }

  public static boolean isMemoryReferenceOperand(byte[] instruction, int operandIndex) {
    return ByteUtil.getBit(instruction[3], (operandIndex * 4) + 3);
  }

  public static boolean isOffsetOperand(byte[] instruction, int operandIndex) {
    return ByteUtil.getBit(instruction[3], (operandIndex * 4) + 2);
  }

  public static boolean isLiteralOperand(byte[] instruction, int operandIndex) {
    int offset = operandIndex * 4;
    return ByteUtil.getBit(instruction[3], offset) && !ByteUtil.getBit(instruction[3], offset + 1);
  }

  public static boolean isRegisterOperand(byte[] instruction, int operandIndex) {
    int offset = operandIndex * 4;
    return !ByteUtil.getBit(instruction[3], offset) && !ByteUtil.getBit(instruction[3], offset + 1);
  }

  public static boolean isLabelOperand(byte[] instruction, int operandIndex) {
    int offset = operandIndex * 4;
    return !ByteUtil.getBit(instruction[3], offset) && ByteUtil.getBit(instruction[3], offset + 1);
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


  public byte[] getRegisters() {
    return registers;
  }

  public byte getRegister(Register reg) {
    return registers[reg.ordinal()];
  }

  public List<byte[]> getProgram() {
    return program;
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

  public boolean isStep() {
    return step;
  }

  public String getError() {
    return error;
  }

  public byte getFaultCode() {
    return faultCode;
  }
}
