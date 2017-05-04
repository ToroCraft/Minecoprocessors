package net.torocraft.minecoprocessors.processor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

public class Processor implements IProcessor {

	private static final String NBT_PROGRAM = "program";

	private String program;
	private byte input;
	private byte output;

	private List<Byte> stack = new ArrayList<Byte>();
	private List<String> instructions = new ArrayList<String>();

	private byte pc;
	private byte sp;

	private byte ax;
	private byte bx;
	private byte cx;
	private byte dx;

	private boolean falt;

	@Override
	public void reset() {
		falt = false;
	}

	@Override
	public void tick() {
		if(falt){
			return;
		}

	}

	private void nextInstruction() {
		pc++;
		if(pc >= instructions.size()){
			falt = true;
			return;
		}
		String line = instructions.get(pc);

		switch (parseInstructionCode(line)) {
		case MOV:
			processMov();
			return;
		}

		System.out.println("command not found");
		falt = true;
	}

	private void processMov() {

	}

	private InstructionCode parseInstructionCode(String line) {
		try {
			return InstructionCode.valueOf(line.trim().split("\\s+")[0]);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void setInput(byte b) {

	}

	@Override
	public byte getOutput() {
		return 0;
	}

	@Override
	public void setProgram(String program) {
		this.program = program;
	}

	@Override
	public void readFromNBT(NBTTagCompound c) {
		program = c.getString(NBT_PROGRAM);
	}

	@Override
	public NBTTagCompound writeToNBT() {
		NBTTagCompound c = new NBTTagCompound();
		c.setString(NBT_PROGRAM, program);
		return c;
	}
}
