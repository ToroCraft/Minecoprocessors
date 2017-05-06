package net.torocraft.minecoprocessors.util;

import net.torocraft.minecoprocessors.processor.Processor;

public class TestRunner {

	public static void main(String[] args) {
		ByteUtil.test();
		InstructionUtil.test();
		Processor p = new Processor();
		p.test();
		System.out.println("pass!");
	}
}
