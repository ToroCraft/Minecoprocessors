package net.torocraft.minecoprocessors.util;

import net.torocraft.minecoprocessors.processor.Processor;

public class TestRunner {

	public static void main(String[] args) {
		ByteUtil.test();
		InstructionUtil.test();
		testProcessor();
		runProcessor();
		System.out.println("pass!");
	}
	
	
	private static void testProcessor() {
		new Processor().test();
	}
	
	private static void runProcessor() {
		Processor p = new Processor();
		
		String program = "";
		program += "mov cx, 10 \n";
		program += "start: \n";
		program += "sub cx, 1 \n";
		program += "jnz start \n";
		program += "mov oa, 100 \n";
		p.load(program);
		
		
		for (int i = 0; i < 100; i++) {
			
			p.tick();
			
			if(p.isFault()){
				break;
			}
		}
		
		System.out.println(p.pinchDump());
		
	}
}
