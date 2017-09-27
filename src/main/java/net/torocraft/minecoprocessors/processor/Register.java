package net.torocraft.minecoprocessors.processor;

public enum Register {
  /**
   * general purpose register (accumulator)
   */
  A,
  /**
   * general purpose register
   */
  B,
  /**
   * general purpose register (counter)
   */
  C,
  /**
   * general purpose register
   */
  D,
  /**
   * front port register
   */
  PF,
  /**
   * back port register
   */
  PB,
  /**
   * left port register
   */
  PL,
  /**
   * right port register
   */
  PR,
  /**
   * port direction registers (high / low Z) <BR>
   *
   * <b>0</b>: output mode (low Z) <BR>
   *
   * <b>1</b>: input mode (high Z) <BR>
   *
   * <ul> <li><b>bit 0:</b> front port</li> <li><b>bit 1:</b> back port</li> <li><b>bit 2:</b> left port</li> <li><b>bit 3:</b> right port</li> </ul>
   */
  PORTS,
  /**
   * ADC control for ports <BR>
   *
   * <b>0</b>: digital <BR>
   *
   * <b>1</b>: ADC/DAC <BR>
   *
   * <ul> <li><b>bit 0:</b> front port</li> <li><b>bit 1:</b> back port</li> <li><b>bit 2:</b> left port</li> <li><b>bit 3:</b> right port</li> </ul>
   */
  ADC
}
