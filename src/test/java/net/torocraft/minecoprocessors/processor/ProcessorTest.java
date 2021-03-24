package net.torocraft.minecoprocessors.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundNBT;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.Label;
import net.torocraft.minecoprocessors.util.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ProcessorTest {

  private static final short TEST_LABEL_ADDRESS = 111;
  private static final short TEST_LABEL_ADDRESS_2 = 53;

  /**
   * Operand Types (4th Instruction Byte):
   *
   * <ul> <li><b>0:</b> Register ID (ordinal value of the Register enum)</li> <li><b>1:</b> Literal Value</li> <li><b>2:</b> Label</li> <li><b>bit 2</b> Has Offset</li> <li><b>bit 3:</b> Is Memory
   * Reference</li> </ul>
   */

  @Test
  public void isLiteralOperand() {
    Processor processor = new Processor();
    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0b00000000};
    Assert.assertFalse(Processor.isLiteralOperand(processor.instruction, 0));
    Assert.assertFalse(Processor.isLiteralOperand(processor.instruction, 1));

    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0b00010000};
    Assert.assertFalse(Processor.isLiteralOperand(processor.instruction, 0));
    Assert.assertTrue(Processor.isLiteralOperand(processor.instruction, 1));

    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0b00110100};
    Assert.assertFalse(Processor.isLiteralOperand(processor.instruction, 0));
    Assert.assertFalse(Processor.isLiteralOperand(processor.instruction, 1));
  }

  @Test
  public void isRegisterOperand() {
    Processor processor = new Processor();
    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0b00000000};
    Assert.assertTrue(Processor.isRegisterOperand(processor.instruction, 0));
    Assert.assertTrue(Processor.isRegisterOperand(processor.instruction, 1));

    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0b00010000};
    Assert.assertTrue(Processor.isRegisterOperand(processor.instruction, 0));
    Assert.assertFalse(Processor.isRegisterOperand(processor.instruction, 1));

    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0b00110100};
    Assert.assertTrue(Processor.isRegisterOperand(processor.instruction, 0));
    Assert.assertFalse(Processor.isRegisterOperand(processor.instruction, 1));
  }

  @Test
  public void isMemoryReferenceOperand() {
    Processor processor = new Processor();
    processor.instruction = inst(0, 0, 0, 0b10000000);
    Assert.assertFalse(Processor.isMemoryReferenceOperand(processor.instruction, 0));
    Assert.assertTrue(Processor.isMemoryReferenceOperand(processor.instruction, 1));

    processor.instruction = inst(0, 0, 0, 0b10010000);
    Assert.assertFalse(Processor.isMemoryReferenceOperand(processor.instruction, 0));
    Assert.assertTrue(Processor.isMemoryReferenceOperand(processor.instruction, 1));

    processor.instruction = inst(0, 0, 0, 0b00111100);
    Assert.assertTrue(Processor.isMemoryReferenceOperand(processor.instruction, 0));
    Assert.assertFalse(Processor.isMemoryReferenceOperand(processor.instruction, 1));
  }

  @Test
  public void getMemoryOffset() {
    Processor processor = new Processor();
    processor.instruction = inst(0, 0, 0, 0b10000000);
    Assert.assertEquals(0, processor.getMemoryOffset(0));

    processor.instruction = inst(0, 0, 0, 0b10000000, 23);
    Assert.assertEquals(0, processor.getMemoryOffset(0));

    processor.instruction = inst(0, 0, 0, 0b11000000, 15);
    Assert.assertEquals(15, processor.getMemoryOffset(1));

    processor.instruction = inst(0, 0, 0, 0b10000100, 32);
    Assert.assertEquals(32, processor.getMemoryOffset(0));
  }

  @Test
  public void isMemoryOffsetOperand() {
    Processor processor = new Processor();
    processor.instruction = inst(0, 0, 0, 0b01000000);
    Assert.assertFalse(Processor.isOffsetOperand(processor.instruction, 0));
    Assert.assertTrue(Processor.isOffsetOperand(processor.instruction, 1));

    processor.instruction = inst(0, 0, 0, 0b00000100);
    Assert.assertFalse(Processor.isOffsetOperand(processor.instruction, 1));
    Assert.assertTrue(Processor.isOffsetOperand(processor.instruction, 0));
  }

  @Test
  public void getVariableOperand() {
    Processor processor = new Processor();
    processor.registers[0] = 10;
    processor.registers[1] = 11;
    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 1, (byte) 0b00000000};
    Assert.assertEquals((byte) 10, processor.getVariableOperand(0));
    Assert.assertEquals((byte) 11, processor.getVariableOperand(1));

    processor.stack[10] = 100;
    processor.stack[11] = 101;
    processor.instruction = new byte[]{(byte) 0, (byte) 0, (byte) 1, (byte) 0b10001000};
    Assert.assertEquals((byte) 100, processor.getVariableOperand(0));
    Assert.assertEquals((byte) 101, processor.getVariableOperand(1));

    processor.stack[20] = 100;
    processor.stack[21] = 101;
    processor.instruction = new byte[]{(byte) 0, (byte) 20, (byte) 21, (byte) 0b10011001};
    Assert.assertEquals((byte) 100, processor.getVariableOperand(0));
    Assert.assertEquals((byte) 101, processor.getVariableOperand(1));
  }

  @Test
  public void testNbt() {
    Processor processor = new Processor();
    processor.flush();
    processor.labels.add(new Label((short) 189, "foobar"));
    processor.program.add(new byte[]{0x00, 0x01, 0x02, 0x03});
    processor.stack[0] = (byte) 0x99;
    processor.stack[63] = (byte) 0x12;
    processor.registers[0] = (byte) 0xee;
    processor.registers[4] = (byte) 0xcc;
    processor.zero = true;

    CompoundNBT c = processor.getNBT();

    processor.flush();
    Assert.assertFalse(processor.zero);
    Assert.assertEquals(0, processor.labels.size());
    Assert.assertEquals(0, processor.program.size());
    for(int i=0; i<processor.registers.length; ++i) processor.registers[i] = 0;
    processor.setNBT(c);
    Assert.assertTrue(processor.zero);
    Assert.assertEquals(64, processor.stack.length);
    Assert.assertEquals((byte) 0x99, processor.stack[0]);
    Assert.assertEquals((byte) 0x12, processor.stack[63]);
    Assert.assertEquals(1, processor.labels.size());
    Assert.assertEquals((short) 189, processor.labels.get(0).address);
    Assert.assertEquals("foobar", processor.labels.get(0).name);
    Assert.assertEquals(1, processor.program.size());
    byte[] testInstruction = processor.program.get(0);
    Assert.assertEquals(0x00, testInstruction[0]);
    Assert.assertEquals(0x01, testInstruction[1]);
    Assert.assertEquals(0x02, testInstruction[2]);
    Assert.assertEquals(0x03, testInstruction[3]);
    Assert.assertEquals((byte) 0xee, processor.registers[0]);
    Assert.assertEquals((byte) 0xcc, processor.registers[4]);
  }

  @Test
  public void testTestOverFlow() {
    Processor processor = new Processor();

    processor.testOverflow(1);
    Assert.assertFalse(processor.overflow);

    processor.testOverflow(1000);
    Assert.assertTrue(processor.overflow);

    processor.testOverflow(128);
    Assert.assertTrue(processor.overflow);

    processor.testOverflow(127);
    Assert.assertFalse(processor.overflow);

    processor.testOverflow(-128);
    Assert.assertFalse(processor.overflow);

    processor.testOverflow(-129);
    Assert.assertTrue(processor.overflow);

    processor.testOverflow(129L);
    Assert.assertTrue(processor.overflow);
  }

  @Test
  public void testProcessWfe() {
    Processor processor = new Processor();
    Assert.assertFalse(processor.isWait());
    processor.processWfe();
    Assert.assertTrue(processor.isWait());
  }

  @Test
  public void testProcessHlt() {
    Processor processor = new Processor();
    Assert.assertFalse(processor.isFault());
    processor.processHlt();
    Assert.assertTrue(processor.isFault());
  }

  @Test
  public void testProcessClz() {
    Processor processor = new Processor();
    processor.zero = true;
    processor.processClz();
    Assert.assertFalse(processor.isZero());
  }

  @Test
  public void testProcessClc() {
    Processor processor = new Processor();
    processor.carry = true;
    processor.processClc();
    Assert.assertFalse(processor.isCarry());
  }

  @Test
  public void testProcessSez() {
    Processor processor = new Processor();
    processor.zero = false;
    processor.processSez();
    Assert.assertTrue(processor.isZero());
  }

  @Test
  public void testProcessSec() {
    Processor processor = new Processor();
    processor.carry = false;
    processor.processSec();
    Assert.assertTrue(processor.isCarry());
  }

  @Test
  public void testProcessMov() throws ParseException {
    Processor processor = setupTest(0, 30, 0, 0, "mov a, b");
    processor.processMov();
    assertRegisters(processor, 30, 30, 0, 0);

    processor = setupTest(0, 30, 0, 0, "mov a, 51");
    processor.processMov();
    assertRegisters(processor, 51, 30, 0, 0);
  }

  @Test
  public void testProcessMov_fromProgram() throws ParseException {
    Processor processor;
    processor = setupTest(0, 30, 0, 0, "mov d, test_label_2");
    processor.program = new ArrayList<>(Collections.nCopies(TEST_LABEL_ADDRESS_2 + 1, inst()));
    processor.program.set(TEST_LABEL_ADDRESS_2, inst(0, 43));
    processor.processMov();
    assertRegisters(processor, 0, 30, 0, 43);

    processor = setupTest(0, 30, 0, 0, "mov d, test_label - 2");
    processor.program = new ArrayList<>(Collections.nCopies(TEST_LABEL_ADDRESS + 1, inst()));
    processor.program.set(TEST_LABEL_ADDRESS - 2, inst(0, 44));
    processor.processMov();
    assertRegisters(processor, 0, 30, 0, 44);
  }

  @Test
  public void testProcessMov_toProgram() throws ParseException {
    Processor processor;
    ParseException e = null;
    try {
      processor = setupTest(0, 30, 0, 0, "mov test_label, d");
      processor.processMov();
    } catch (ParseException ex) {
      e = ex;
    }
    Assert.assertNotNull(e);
    Assert.assertEquals(InstructionUtil.ERROR_LABEL_IN_FIRST_OPERAND, e.message);
  }

  @Test
  public void testProcessMov_fromMemory() throws ParseException {
    Processor processor = setupTest(0, 30, 0, 0, "mov a, [2]");
    processor.stack[2] = (byte) 12;
    processor.processMov();
    assertRegisters(processor, 12, 30, 0, 0);

    processor = setupTest(10, 30, 0, 0, "mov [a], 0x52");
    processor.processMov();
    assertRegisters(processor, 10, 30, 0, 0);
    Assert.assertEquals((byte) 0x52, processor.stack[10]);

    processor = setupTest(10, 30, 0, 0, "mov [a+10], 0x52");
    processor.processMov();
    assertRegisters(processor, 10, 30, 0, 0);
    Assert.assertEquals((byte) 0x52, processor.stack[20]);
  }

  @Test
  public void testProcessAdd() throws ParseException {
    Processor processor = setupTest(3, 30, 0, 0, "add a, b");
    processor.processAdd();
    assertRegisters(processor, 33, 30, 0, 0);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(11, 0, 0, 0, "add a, 51");
    processor.processAdd();
    assertRegisters(processor, 62, 0, 0, 0);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(-51, 0, 0, 0, "add a, 51");
    processor.processAdd();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertFalse(processor.overflow);
    Assert.assertTrue(processor.zero);

    processor = setupTest(130, 0, 130, 0, "add a, c");
    processor.processAdd();
    assertRegisters(processor, 4, 0, 130, 0);
    Assert.assertTrue(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(1, 0, 0, 0, "add a, 0xf0");
    processor.processAdd();
    assertRegisters(processor, 241, 0, 0, 0);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);
  }

  @Test
  public void testProcessAdd_fromMemory() throws ParseException {
    Processor processor = setupTest(60, 0, 0, 0, "add a, [25]");
    processor.stack[25] = 9;
    processor.processAdd();
    assertRegisters(processor, 69, 0, 0, 0);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(60, 0, 0, 0, "add a, [25]");
    processor.stack[25] = 9;
    processor.processAdd();
    assertRegisters(processor, 69, 0, 0, 0);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);
  }

  @Test
  public void testProcessSub() throws ParseException {
    Processor processor = setupTest(50, 0, 0, 10, "sub a, d");
    processor.processSub();
    assertRegisters(processor, 40, 0, 0, 10);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(-130, 0, 0, 130, "sub a, d");
    processor.processSub();
    assertRegisters(processor, -4, 0, 0, 130);
    Assert.assertTrue(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(130, 0, 0, 130, "sub a, d");
    processor.processSub();
    assertRegisters(processor, 0, 0, 0, 130);
    Assert.assertFalse(processor.overflow);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessCmp() throws ParseException {
    Processor processor = setupTest(50, 0, 0, 10, "cmp a, d");
    processor.processCmp();
    assertRegisters(processor, 50, 0, 0, 10);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(-130, 0, 0, 130, "cmp a, d");
    processor.processCmp();
    assertRegisters(processor, -130, 0, 0, 130);
    Assert.assertTrue(processor.overflow);
    Assert.assertFalse(processor.zero);

    processor = setupTest(130, 0, 0, 130, "cmp a, d");
    processor.processCmp();
    assertRegisters(processor, 130, 0, 0, 130);
    Assert.assertFalse(processor.overflow);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessAnd() throws ParseException {
    Processor processor = setupTest(0b011111, 0b010, 0, 0, "and a, b");
    processor.processAnd();
    assertRegisters(processor, 0b010, 0b010, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b011101, 0b010, 0, 0, "and a, b");
    processor.processAnd();
    assertRegisters(processor, 0, 0b010, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessXor() throws ParseException {
    Processor processor = setupTest(0b0101, 0b0110, 0, 0, "xor a, b");
    processor.processXor();
    assertRegisters(processor, 0b011, 0b0110, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b0101, 0b0101, 0, 0, "xor a, b");
    processor.processXor();
    assertRegisters(processor, 0, 0b0101, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessOr() throws ParseException {
    Processor processor = setupTest(0b01000, 0, 0, 0b0111, "or d, a");
    processor.processOr();
    assertRegisters(processor, 0b01000, 0, 0, 0b01111);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0, 0, 0, 0, "or d, a");
    processor.processOr();
    assertRegisters(processor, 0, 0, 0, 0);
    assert processor.zero;
  }

  @Test
  public void testProcessNot() throws ParseException {
    Processor processor = setupTest(0, 0, 0b1010, 0, "not c");
    processor.processNot();
    assertRegisters(processor, 0, 0, 0b11110101, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0, 0, 0b11111111, 0, "not c");
    processor.processNot();
    assertRegisters(processor, 0, 0, 0, 0);
    assert processor.zero;
  }

  @Test
  public void testProcessJmp() throws ParseException {
    Processor processor = setupTest(0, 0, 0, 0, "jmp test_label");
    processor.processJmp();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertEquals(TEST_LABEL_ADDRESS, processor.ip);
  }

  @Test
  public void testProcessJz() throws ParseException {
    Processor processor = setupTest(0, 0, 0, 0, "jz test_label");
    processor.zero = true;
    processor.processJz();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertEquals(TEST_LABEL_ADDRESS, processor.ip);

    processor = setupTest(0, 0, 0, 0, "jz test_label");
    processor.zero = false;
    processor.processJz();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertEquals((short) 0, processor.ip);

  }

  @Test
  public void testProcessJnz() throws ParseException {
    Processor processor = setupTest(0, 0, 0, 0, "jnz test_label");
    processor.zero = false;
    processor.processJnz();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertEquals(TEST_LABEL_ADDRESS, processor.ip);

    processor = setupTest(0, 0, 0, 0, "jnz test_label");
    processor.zero = true;
    processor.processJnz();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertEquals(0, processor.ip);
  }

  @Test
  public void testProcessJc() throws ParseException {
    Assert.assertFalse(willJump(Processor::processJc, false, true));
    Assert.assertFalse(willJump(Processor::processJc, false, false));
    Assert.assertTrue(willJump(Processor::processJc, true, false));
    Assert.assertTrue(willJump(Processor::processJc, true, true));
  }

  @Test
  public void testProcessJnc() throws ParseException {
    Assert.assertTrue(willJump(Processor::processJnc, false, false));
    Assert.assertTrue(willJump(Processor::processJnc, false, true));
    Assert.assertFalse(willJump(Processor::processJnc, true, false));
    Assert.assertFalse(willJump(Processor::processJnc, true, true));
  }

  @Test
  public void testProcessJg() throws ParseException {
    Assert.assertTrue(willJump(Processor::processJg, false, false));
    Assert.assertFalse(willJump(Processor::processJg, true, true));
    Assert.assertFalse(willJump(Processor::processJg, true, true));
    Assert.assertFalse(willJump(Processor::processJg, false, true));
  }

  @Test
  public void testProcessJl() throws ParseException {
    Assert.assertTrue(willJump(Processor::processJl, true, false));
    Assert.assertFalse(willJump(Processor::processJl, true, true));
    Assert.assertFalse(willJump(Processor::processJl, false, false));
    Assert.assertFalse(willJump(Processor::processJl, false, true));
  }

  @Test
  public void testProcessJle() throws ParseException {
    Assert.assertTrue(willJump(Processor::processJle, true, true));
    Assert.assertFalse(willJump(Processor::processJle, true, false));
    Assert.assertTrue(willJump(Processor::processJle, false, true));
    Assert.assertFalse(willJump(Processor::processJle, false, false));
  }

  @Test
  public void testProcessDjnz() throws ParseException {
    //TODO there is nothing that enforces the second argument to be label

    Processor processor = setupTest(0, 3, 0, 0, "djnz b, test_label");
    processor.processDjnz();
    assertRegisters(processor, 0, 2, 0, 0);
    Assert.assertFalse(processor.zero);
    Assert.assertEquals(TEST_LABEL_ADDRESS, processor.ip);

    processor = setupTest(0, 2, 0, 0, "djnz b, test_label");
    processor.processDjnz();
    assertRegisters(processor, 0, 1, 0, 0);
    Assert.assertFalse(processor.zero);
    Assert.assertEquals(TEST_LABEL_ADDRESS, processor.ip);

    processor = setupTest(0, 1, 0, 0, "djnz b, test_label");
    processor.processDjnz();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessShl() throws ParseException {
    Processor processor = setupTest(0b01, 4, 0, 0, "shl a, b");
    processor.processShl();
    assertRegisters(processor, 0b010000, 4, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b01, 20, 0, 0, "shl a, b");
    processor.processShl();
    assertRegisters(processor, 0, 20, 0, 0);
    Assert.assertTrue(processor.zero);

    processor = setupTest(0, 2, 0, 0, "shl a, b");
    processor.processShl();
    assertRegisters(processor, 0, 2, 0, 0);
    Assert.assertTrue(processor.zero);

    processor = setupTest(0b01, 20, 0, 0, "shl a, 1");
    processor.processShl();
    assertRegisters(processor, 0b010, 20, 0, 0);
    Assert.assertFalse(processor.zero);
  }

  @Test
  public void testProcessShr() throws ParseException {
    Processor processor = setupTest(0b10000000, 1, 0, 0, "shr a, b");
    processor.processShr();
    assertRegisters(processor, 0b01000000, 1, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b10000000, 100, 0, 0, "shr a, b");
    processor.processShr();
    assertRegisters(processor, 0, 100, 0, 0);
    Assert.assertTrue(processor.zero);

    processor = setupTest(0xff, 8, 0, 0, "shr a, b");
    processor.processShr();
    assertRegisters(processor, 0, 8, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessSar() throws ParseException {
    Processor processor = setupTest(0b10000000, 1, 0, 0, "sar a, b");
    processor.processSar();
    assertRegisters(processor, 0b11000000, 1, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b10000000, 100, 0, 0, "sar a, b");
    processor.processSar();
    assertRegisters(processor, 0b11111111, 100, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0x0, 5, 0, 0, "sar a, b");
    processor.processSar();
    assertRegisters(processor, 0, 5, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessRol() throws ParseException {
    Processor processor = setupTest(0b01, 4, 0, 0, "rol a, b");
    processor.processRol();
    assertRegisters(processor, 0b010000, 4, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b11110000, 2, 0, 0, "rol a, b");
    processor.processRol();
    assertRegisters(processor, 0b11000011, 2, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b11110000, 30, 0, 0, "rol a, b");
    processor.processRol();
    assertRegisters(processor, 0b11110000, 30, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0, 30, 0, 0, "rol a, b");
    processor.processRol();
    assertRegisters(processor, 0, 30, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessRor() throws ParseException {
    Processor processor = setupTest(0b01, 4, 0, 0, "ror a, b");
    processor.processRor();
    assertRegisters(processor, 0b00010000, 4, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b11110000, 2, 0, 0, "ror a, b");
    processor.processRor();
    assertRegisters(processor, 0b00111100, 2, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0b11110000, 30, 0, 0, "ror a, b");
    processor.processRor();
    assertRegisters(processor, 0b11110000, 30, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(0, 30, 0, 0, "ror a, b");
    processor.processRor();
    assertRegisters(processor, 0, 30, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessPushPop() throws ParseException {
    Processor processor = new Processor();
    processor.reset();
    processor.registers[Register.A.ordinal()] = 30;
    processor.registers[Register.B.ordinal()] = 0;
    processor.instruction = InstructionUtil.parseLine("push a", new ArrayList<>(), (short) 0);
    processor.processPush();

    Assert.assertEquals(1, processor.sp);
    Assert.assertEquals((byte) 30, processor.stack[0]);

    processor.processPush();

    Assert.assertEquals(2, processor.sp);
    Assert.assertEquals((byte) 30, processor.stack[1]);

    processor.instruction = InstructionUtil.parseLine("pop b", new ArrayList<>(), (short) 0);
    processor.processPop();
    Assert.assertEquals(1, processor.sp);
    Assert.assertEquals((byte) 30, processor.registers[Register.B.ordinal()]);
  }

  @Test
  public void testPackFlags() {
    Processor processor = new Processor();
    processor.reset();

    processor.fault = true;
    Assert.assertEquals(Long.parseUnsignedLong("0000000000000001", 16), processor.packFlags());

    processor.zero = true;
    Assert.assertEquals(Long.parseUnsignedLong("0000000000000003", 16), processor.packFlags());

    processor.overflow = true;
    Assert.assertEquals(Long.parseUnsignedLong("0000000000000007", 16), processor.packFlags());

    processor.sp = (byte) 0xee;
    processor.ip = (short) 0xabcd;
    Assert.assertEquals(Long.parseUnsignedLong("abcdee0000000007", 16), processor.packFlags());
  }

  @Test
  public void testUnpackFlags() {
    Processor processor = new Processor();
    processor.reset();
    processor.unPackFlags(Long.parseUnsignedLong("abcdee0000000007", 16));
    Assert.assertEquals((byte) 0xee, processor.sp);
    Assert.assertEquals((short) 0xabcd, processor.ip);
    Assert.assertTrue(processor.zero);
    Assert.assertTrue(processor.overflow);
    Assert.assertTrue(processor.fault);

    processor.unPackFlags(Long.parseUnsignedLong("0000ff0000000000", 16));
    Assert.assertEquals(-1, processor.sp);
    Assert.assertEquals(0, processor.ip);
    Assert.assertFalse(processor.zero);
    Assert.assertFalse(processor.overflow);
    Assert.assertFalse(processor.fault);
  }

  @Test
  public void testProcessCall() throws ParseException {
    Processor processor = setupTest(0, 0, 0, 0, "call test_label");
    processor.ip = (short) 0xabcd;
    processor.processCall();
    assertRegisters(processor, 0, 0, 0, 0);

    Assert.assertEquals((byte) 0xcd, processor.stack[0]);
    Assert.assertEquals((byte) 0xab, processor.stack[1]);

    Assert.assertEquals(TEST_LABEL_ADDRESS, processor.ip);
    Assert.assertEquals((short) 2, processor.sp);

    processor.processRet();

    Assert.assertEquals((short) 0xabcd, processor.ip);
    Assert.assertEquals(0, processor.sp);
  }

  @Test
  public void testProcessInc() throws ParseException {
    Processor processor = setupTest(10, 0, 0, 0, "inc a");
    processor.processInc();
    assertRegisters(processor, 11, 0, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(-1, 0, 0, 0, "inc a");
    processor.processInc();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessDec() throws ParseException {
    Processor processor = setupTest(10, 0, 0, 0, "dec a");
    processor.processDec();
    assertRegisters(processor, 9, 0, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(1, 0, 0, 0, "dec a");
    processor.processDec();
    assertRegisters(processor, 0, 0, 0, 0);
    Assert.assertTrue(processor.zero);
  }

  @Test
  public void testProcessMul() throws ParseException {
    Processor processor = setupTest(0xff, 2, 0, 0, "mul b");
    processor.processMul();
    assertRegisters(processor, 0xfe, 2, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(5, 0, 0, 2, "mul d");
    processor.processMul();
    assertRegisters(processor, 10, 0, 0, 2);
    Assert.assertFalse(processor.zero);
  }

  @Test
  public void testProcessDiv() throws ParseException {
    Processor processor = setupTest(10, 2, 0, 0, "div b");
    processor.processDiv();
    assertRegisters(processor, 5, 2, 0, 0);
    Assert.assertFalse(processor.zero);

    processor = setupTest(5, 0, 0, 2, "div d");
    processor.processDiv();
    assertRegisters(processor, 2, 0, 0, 2);

    processor = setupTest(5, 0, 0, 0, "div d");
    processor.processDiv();
    assertRegisters(processor, 5, 0, 0, 0);
    Assert.assertFalse(processor.zero);
    Assert.assertTrue(processor.fault);
  }

  @Ignore
  @Test
  public void runProcessor() {
    Processor p = new Processor();

    List<String> program = Arrays.asList(
        "mov c, 10 ",
        "start: ",
        "sub c, 1 ",
        "jnz start ",
        "mov e, 100 "
    );
    p.load(program);

    for (int i = 0; i < 100; i++) {

      p.tick();

      if (p.isFault()) {
        break;
      }
    }

    p.processDump();
  }

  @Ignore
  @Test
  public void processDump() {
    Processor processor = new Processor();
    processor.stack[2] = (byte) 0xee;
    processor.processDump();
  }

  @Test
  public void runFaultRet() {
    Processor p = new Processor();

    List<String> program = Arrays.asList(
        "mov c, 10 ",
        "start: ",
        "sub c, 1 ",
        "jnz start ",
        "ret "
    );
    p.load(program);

    for (int i = 0; i < 100; i++) {
      p.tick();
      if (p.isFault()) {
        break;
      }
    }

    Assert.assertTrue(p.isFault());
    Assert.assertEquals("ret", p.getError());
  }

  private static Processor setupTest(int ax, int bx, int cx, int dx, String line) throws ParseException {
    Processor processor = new Processor();
    processor.reset();
    processor.labels = new ArrayList<>();
    processor.labels.add(new Label(TEST_LABEL_ADDRESS, "test_label"));
    processor.labels.add(new Label(TEST_LABEL_ADDRESS_2, "test_label_2"));
    processor.registers[Register.A.ordinal()] = (byte) ax;
    processor.registers[Register.B.ordinal()] = (byte) bx;
    processor.registers[Register.C.ordinal()] = (byte) cx;
    processor.registers[Register.D.ordinal()] = (byte) dx;
    processor.instruction = InstructionUtil.parseLine(line, processor.labels, (short) 0);
    return processor;
  }

  private static void assertRegisters(Processor processor, int a, int b, int c, int d) {
    Assert.assertEquals((byte) a, processor.registers[Register.A.ordinal()]);
    Assert.assertEquals((byte) b, processor.registers[Register.B.ordinal()]);
    Assert.assertEquals((byte) c, processor.registers[Register.C.ordinal()]);
    Assert.assertEquals((byte) d, processor.registers[Register.D.ordinal()]);
  }

  @FunctionalInterface
  private interface ProcessorFunction {
    void run(Processor processor);
  }

  private boolean willJump(ProcessorFunction func, boolean carry, boolean zero) throws ParseException {
    Processor processor = setupTest(0, 0, 0, 0, "jmp test_label");
    processor.carry = carry;
    processor.zero = zero;
    func.run(processor);
    assertRegisters(processor, 0, 0, 0, 0);
    return TEST_LABEL_ADDRESS == processor.ip;
  }

  private static byte[] inst(int... a) {
    byte[] inst = new byte[a.length];
    for (int i = 0; i < a.length; i++) {
      inst[i] = (byte) a[i];
    }
    return inst;
  }
}
