package net.torocraft.minecoprocessors.processor;

import java.util.ArrayList;
import mockit.Deencapsulation;
import net.minecraft.nbt.NBTTagCompound;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.Label;
import net.torocraft.minecoprocessors.util.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ProcessorTest {

  @Test
  public void testCopyArray() {
    byte[] a = {1, 2, 3, 4, 5, 6};
    byte[] b = new byte[a.length];
    Deencapsulation.invoke(Processor.class, "copy", b, a);
    Assert.assertEquals(1, b[0]);
    Assert.assertEquals(6, b[5]);
  }

  // TODO convert tests below to JUnit

//
//  private static void testNbt(Processor processor) {
//    processor.flush();
//    processor.labels.add(new Label((short) 189, "foobar"));
//    processor.program.add(new byte[] {0x00, 0x01, 0x02, 0x03});
//    processor.stack[0] = (byte) 0x99;
//    processor.registers[0] = (byte) 0xee;
//    processor.registers[4] = (byte) 0xcc;
//    processor.zero = true;
//
//    NBTTagCompound c = processor.writeToNBT();
//
//    processor.flush();
//    assert !processor.zero;
//    assert processor.labels.size() == 0;
//    assert processor.program.size() == 0;
//    Processor.reset(processor.registers);
//
//    processor.readFromNBT(c);
//
//    assert processor.zero;
//    assert processor.labels.size() == 1;
//    assert processor.labels.get(0).address == (short) 189;
//    assert processor.labels.get(0).name.equals("foobar");
//    assert processor.program.size() == 1;
//    byte[] testInstruction = processor.program.get(0);
//    assert testInstruction[0] == 0x00;
//    assert testInstruction[1] == 0x01;
//    assert testInstruction[2] == 0x02;
//    assert testInstruction[3] == 0x03;
//    assert processor.registers[0] == (byte) 0xee;
//    assert processor.registers[4] == (byte) 0xcc;
//
//  }
//
//  private static void testTestOverFlow(Processor processor) {
//    processor.testOverflow(1);
//    assert !processor.overflow;
//
//    processor.testOverflow(1000);
//    assert processor.overflow;
//
//    processor.testOverflow(128);
//    assert processor.overflow;
//
//    processor.testOverflow(127);
//    assert !processor.overflow;
//
//    processor.testOverflow(-128);
//    assert !processor.overflow;
//
//    processor.testOverflow(-129);
//    assert processor.overflow;
//  }
//
//  private static void testProcessMov(Processor processor) {
//    try {
//      setupTest(processor, 0, 30, 0, 0, "mov a, b");
//      processor.processMov();
//      processor.assertRegisters(30, 30, 0, 0);
//
//      setupTest(processor, 0, 30, 0, 0, "mov a, 51");
//      processor.processMov();
//      processor.assertRegisters(51, 30, 0, 0);
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessAdd(Processor processor) {
//    try {
//      setupTest(processor, 3, 30, 0, 0, "add a, b");
//      processor.processAdd();
//      processor.assertRegisters(33, 30, 0, 0);
//      assert !processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, 11, 0, 0, 0, "add a, 51");
//      processor.processAdd();
//      processor.assertRegisters(62, 0, 0, 0);
//      assert !processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, -51, 0, 0, 0, "add a, 51");
//      processor.processAdd();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert !processor.overflow;
//      assert processor.zero;
//
//      setupTest(processor, 130, 0, 130, 0, "add a, c");
//      processor.processAdd();
//      processor.assertRegisters(4, 0, 130, 0);
//      assert processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, 1, 0, 0, 0, "add a, 0xf0");
//      processor.processAdd();
//      processor.assertRegisters(241, 0, 0, 0);
//      assert !processor.overflow;
//      assert !processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessSub(Processor processor) {
//    try {
//      setupTest(processor, 50, 0, 0, 10, "sub a, d");
//      processor.processSub();
//      processor.assertRegisters(40, 0, 0, 10);
//      assert !processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, -130, 0, 0, 130, "sub a, d");
//      processor.processSub();
//      processor.assertRegisters(-4, 0, 0, 130);
//      assert processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, 130, 0, 0, 130, "sub a, d");
//      processor.processSub();
//      processor.assertRegisters(0, 0, 0, 130);
//      assert !processor.overflow;
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessCmp(Processor processor) {
//    try {
//      setupTest(processor, 50, 0, 0, 10, "cmp a, d");
//      processor.processCmp();
//      processor.assertRegisters(50, 0, 0, 10);
//      assert !processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, -130, 0, 0, 130, "cmp a, d");
//      processor.processCmp();
//      processor.assertRegisters(-130, 0, 0, 130);
//      assert processor.overflow;
//      assert !processor.zero;
//
//      setupTest(processor, 130, 0, 0, 130, "cmp a, d");
//      processor.processCmp();
//      processor.assertRegisters(130, 0, 0, 130);
//      assert !processor.overflow;
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessAnd(Processor processor) {
//    try {
//      setupTest(processor, 0b011111, 0b010, 0, 0, "and a, b");
//      processor.processAnd();
//      processor.assertRegisters(0b010, 0b010, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 0b011101, 0b010, 0, 0, "and a, b");
//      processor.processAnd();
//      processor.assertRegisters(0, 0b010, 0, 0);
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessXor(Processor processor) {
//    try {
//      setupTest(processor, 0b0101, 0b0110, 0, 0, "xor a, b");
//      processor.processXor();
//      processor.assertRegisters(0b011, 0b0110, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 0b0101, 0b0101, 0, 0, "xor a, b");
//      processor.processXor();
//      processor.assertRegisters(0, 0b0101, 0, 0);
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessOr(Processor processor) {
//    try {
//      setupTest(processor, 0b01000, 0, 0, 0b0111, "or d, a");
//      processor.processOr();
//      processor.assertRegisters(0b01000, 0, 0, 0b01111);
//      assert !processor.zero;
//
//      setupTest(processor, 0, 0, 0, 0, "or d, a");
//      processor.processOr();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessNot(Processor processor) {
//    try {
//      setupTest(processor, 0, 0, 0b1010, 0, "not c");
//      processor.processNot();
//      processor.assertRegisters(0, 0, 0b11110101, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 0, 0, 0b11111111, 0, "not c");
//      processor.processNot();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessJmp(Processor processor) {
//    try {
//      setupTest(processor, 0, 0, 0, 0, "jmp test_label");
//      processor.processJmp();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.ip == (short) 111;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessJz(Processor processor) {
//    try {
//
//      setupTest(processor, 0, 0, 0, 0, "jz test_label");
//      processor.zero = true;
//      processor.processJz();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.ip == (short) 111;
//
//      setupTest(processor, 0, 0, 0, 0, "jz test_label");
//      processor.zero = false;
//      processor.processJz();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.ip == 0;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessJnz(Processor processor) {
//    try {
//      setupTest(processor, 0, 0, 0, 0, "jnz test_label");
//      processor.zero = false;
//      processor.processJnz();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.ip == (short) 111;
//
//      setupTest(processor, 0, 0, 0, 0, "jnz test_label");
//      processor.zero = true;
//      processor.processJnz();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.ip == 0;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessShl(Processor processor) {
//    try {
//      setupTest(processor, 0b01, 4, 0, 0, "shl a, b");
//      processor.processShl();
//      processor.assertRegisters(0b010000, 4, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 0b01, 20, 0, 0, "shl a, b");
//      processor.processShl();
//      processor.assertRegisters(0, 20, 0, 0);
//      assert processor.zero;
//
//      setupTest(processor, 0, 2, 0, 0, "shl a, b");
//      processor.processShl();
//      processor.assertRegisters(0, 2, 0, 0);
//      assert processor.zero;
//
//      setupTest(processor, 0b01, 20, 0, 0, "shl a, 1");
//      processor.processShl();
//      processor.assertRegisters(0b010, 20, 0, 0);
//      assert !processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessShr(Processor processor) {
//
//    try {
//      setupTest(processor, 0b10000000, 1, 0, 0, "shr a, b");
//      processor.processShr();
//      processor.assertRegisters(0b01000000, 1, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 0b10000000, 100, 0, 0, "shr a, b");
//      processor.processShr();
//      processor.assertRegisters(0, 100, 0, 0);
//      assert processor.zero;
//
//      setupTest(processor, 0xff, 8, 0, 0, "shr a, b");
//      processor.processShr();
//      processor.assertRegisters(0, 8, 0, 0);
//      assert processor.zero;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessPushPop(Processor processor) {
//    try {
//      processor.reset();
//      processor.registers[Register.A.ordinal()] = 30;
//      processor.registers[Register.B.ordinal()] = 0;
//      processor.instruction = InstructionUtil.parseLine("push a", new ArrayList<Label>(), (short) 0);
//      processor.processPush();
//
//      assert processor.sp == 1;
//      assert processor.stack[0] == (byte) 30;
//
//      processor.processPush();
//
//      assert processor.sp == 2;
//      assert processor.stack[1] == (byte) 30;
//
//      processor.instruction = InstructionUtil.parseLine("pop b", new ArrayList<Label>(), (short) 0);
//      processor.processPop();
//      assert processor.sp == 1;
//      assert processor.registers[Register.B.ordinal()] == (byte) 30;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  static void setupTest(Processor processor, int ax, int bx, int cx, int dx, String line) throws ParseException {
//    processor.reset();
//    processor.labels = new ArrayList<>();
//    processor.labels.add(new Label((short) 111, "test_label"));
//    processor.registers[Register.A.ordinal()] = (byte) ax;
//    processor.registers[Register.B.ordinal()] = (byte) bx;
//    processor.registers[Register.C.ordinal()] = (byte) cx;
//    processor.registers[Register.D.ordinal()] = (byte) dx;
//    processor.instruction = InstructionUtil.parseLine(line, processor.labels, (short) 0);
//  }
//
//  private static void testPackFlags(Processor processor) {
//    processor.reset();
//    processor.temp = (byte) 0xff;
//    assert processor.packFlags() == Long.parseUnsignedLong("000000ff00000000", 16);
//    processor.fault = true;
//    processor.temp = 0;
//    assert processor.packFlags() == Long.parseUnsignedLong("0000000000000001", 16);
//    processor.zero = true;
//    assert processor.packFlags() == Long.parseUnsignedLong("0000000000000003", 16);
//    processor.overflow = true;
//    assert processor.packFlags() == Long.parseUnsignedLong("0000000000000007", 16);
//    processor.sp = (byte) 0xee;
//    processor.ip = (short) 0xabcd;
//    assert processor.packFlags() == Long.parseUnsignedLong("abcdee0000000007", 16);
//  }
//
//  private static void testUnpackFlags(Processor processor) {
//    processor.reset();
//    processor.unPackFlags(Long.parseUnsignedLong("abcdee0000000007", 16));
//    assert processor.sp == (byte) 0xee;
//    assert processor.ip == (short) 0xabcd;
//    assert processor.zero;
//    assert processor.overflow;
//    assert processor.fault;
//
//    processor.unPackFlags(Long.parseUnsignedLong("0000ff0000000000", 16));
//    assert processor.sp == -1;
//    assert processor.ip == 0;
//    assert !processor.zero;
//    assert !processor.overflow;
//    assert !processor.fault;
//  }
//
//  private static void testProcessCall(Processor processor) {
//    try {
//      setupTest(processor, 0, 0, 0, 0, "call test_label");
//      processor.ip = (short) 0xabcd;
//      processor.processCall();
//      processor.assertRegisters(0, 0, 0, 0);
//
//      assert processor.stack[0] == (byte) 0xcd;
//      assert processor.stack[1] == (byte) 0xab;
//
//      assert processor.ip == (short) 111;
//      assert processor.sp == 2;
//
//      processor.processRet();
//
//      assert processor.ip == (short) 0xabcd;
//      assert processor.sp == 0;
//
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessInc(Processor processor) {
//    try {
//      setupTest(processor, 10, 0, 0, 0, "inc a");
//      processor.processInc();
//      processor.assertRegisters(11, 0, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, -1, 0, 0, 0, "inc a");
//      processor.processInc();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.zero;
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessDec(Processor processor) {
//    try {
//      setupTest(processor, 10, 0, 0, 0, "dec a");
//      processor.processDec();
//      processor.assertRegisters(9, 0, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 1, 0, 0, 0, "dec a");
//      processor.processDec();
//      processor.assertRegisters(0, 0, 0, 0);
//      assert processor.zero;
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessMul(Processor processor) {
//    try {
//      setupTest(processor, 0xff, 2, 0, 0, "mul b");
//      processor.processMul();
//      processor.assertRegisters(0xfe, 2, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 5, 0, 0, 2, "mul d");
//      processor.processMul();
//      processor.assertRegisters(10, 0, 0, 2);
//      assert !processor.zero;
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }
//
//  private static void testProcessDiv(Processor processor) {
//    try {
//      setupTest(processor, 10, 2, 0, 0, "div b");
//      processor.processDiv();
//      processor.assertRegisters(5, 2, 0, 0);
//      assert !processor.zero;
//
//      setupTest(processor, 5, 0, 0, 2, "div d");
//      processor.processDiv();
//      processor.assertRegisters(2, 0, 0, 2);
//
//      setupTest(processor, 5, 0, 0, 0, "div d");
//      processor.processDiv();
//      processor.assertRegisters(5, 0, 0, 0);
//      assert !processor.zero;
//      assert processor.fault;
//    } catch (ParseException e) {
//      throw new AssertionError(e);
//    }
//  }

  @Ignore
  @Test
  public void runProcessor() {
    Processor p = new Processor();

    String program = "";
    program += "mov c, 10 \n";
    program += "start: \n";
    program += "sub c, 1 \n";
    program += "jnz start \n";
    program += "mov e, 100 \n";
    p.load(program);

    for (int i = 0; i < 100; i++) {

      p.tick();

      if (p.isFault()) {
        break;
      }
    }

    System.out.println(p.pinchDump());
  }

  // TODO update to JUnit asserts
  @Test
  public void runFaultRet() {
    Processor p = new Processor();

    String program = "";
    program += "mov c, 10 \n";
    program += "start: \n";
    program += "sub c, 1 \n";
    program += "jnz start \n";
    program += "ret \n";
    p.load(program);

    for (int i = 0; i < 100; i++) {
      p.tick();
      if (p.isFault()) {
        break;
      }
    }

    assert p.isFault();
    assert p.getError().equals("ret");
  }
}