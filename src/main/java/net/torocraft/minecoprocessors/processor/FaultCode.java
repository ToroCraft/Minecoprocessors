package net.torocraft.minecoprocessors.processor;

public class FaultCode
{
  public static final byte FAULT_DIVISION_BY_ZERO = 0x00;
  public static final byte FAULT_STACK_UNDERFLOW = 0x01;
  public static final byte FAULT_STACK_OVERFLOW = 0x02;
  public static final byte FAULT_UNDEFINED_IP = 0x03;
  public static final byte FAULT_UNKNOWN_OPCODE = 0x04;
  public static final byte FAULT_OUT_OF_BOUNDS = 0x05;
  public static final byte FAULT_HLT_INSTRUCTION = (byte) 0xFE;
  public static final byte FAULT_STATE_NOMINAL = (byte) 0xFF;
}
