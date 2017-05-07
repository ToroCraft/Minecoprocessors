package net.torocraft.minecoprocessors.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.processor.IProcessor;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.ByteUtil;

@SuppressWarnings("unused")
public class TileEntityMinecoprocessor extends TileEntity implements ITickable {

	private static final String NAME = "minecoprocessor_tile_entity";
	private static final String NBT_PROCESSOR = "processor";

	private final IProcessor processor = new Processor();
	private final boolean[] prevPortValues = new boolean[4];

	public static void init() {
		GameRegistry.registerTileEntity(TileEntityMinecoprocessor.class, NAME);
	}

	public TileEntityMinecoprocessor() {
		loadSampleProgramOutput();
	}

	private void loadSampleProgramInput() {
		String program = "";
		program += "; ouput: none      \n";
		program += "	mov ports, 0   \n";
		program += "	 			   \n";
		program += "start: 			   \n";
		program += "	jmp start 	   \n";
		program += "	 			   \n";
		processor.load(program);
	}

	private void loadSampleProgramOutput() {
		String program = "";
		program += "; ouput: all        \n";
		program += "	mov ports, 15   \n";
		program += "	 			   \n";
		program += "start: 			   \n";
		program += "	mov pf, 1	   \n";
		program += "	mov pr, 1	   \n";
		program += "	mov pb, 1	   \n";
		program += "	mov pl, 1	   \n";
		program += "	mov pf, 0	   \n";
		program += "	mov pr, 0	   \n";
		program += "	mov pb, 0	   \n";
		program += "	mov pl, 0	   \n";
		program += "	jmp start 	   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		processor.load(program);
	}

	private void loadSampleProgram1() {
		String program = "";
		program += "; ouput: pf pr     \n";
		program += "	mov ports, 9   \n";
		program += "	 			   \n";
		program += "start: 			   \n";
		program += "	mov c, 10 	   \n";
		program += "	 			   \n";
		program += "label: 			   \n";
		program += "	mov pf, 0	   \n";
		program += "	mov pr, 1	   \n";
		program += "	sub c, 1 	   \n";
		program += "	mov pf, 1	   \n";
		program += "	mov pr, 0	   \n";
		program += "	jnz label 	   \n";
		program += "			 	   \n";
		program += "	mov n, 1 	   \n";
		program += "	jmp start 	   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		processor.load(program);
	}

	@Override
	public void readFromNBT(NBTTagCompound c) {
		super.readFromNBT(c);
		processor.readFromNBT(c.getCompoundTag(NBT_PROCESSOR));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound cIn) {
		NBTTagCompound c = super.writeToNBT(cIn);
		c.setTag(NBT_PROCESSOR, processor.writeToNBT());
		return c;
	}

	@Override
	public void update() {
		if (world.isRemote || world.getTotalWorldTime() % 10 != 0) {
			return;
		}
		processor.tick();
		detectOutputChanges();
	}

	private boolean detectOutputChanges() {
		boolean updated = false;
		updated = updated || detectOutputChange(0);
		updated = updated || detectOutputChange(1);
		updated = updated || detectOutputChange(2);
		updated = updated || detectOutputChange(3);

		return updated;
	}

	private boolean detectOutputChange(int portIndex) {
		byte[] registers = processor.getRegisters();
		byte ports = registers[Register.PORTS.ordinal()];

		boolean curVal = ByteUtil.getBit(registers[Register.PF.ordinal() + portIndex], 0);

		if (ByteUtil.getBit(ports, portIndex) && prevPortValues[portIndex] != curVal) {
			prevPortValues[portIndex] = curVal;
			BlockMinecoprocessor.INSTANCE.onPortChange(world, pos, world.getBlockState(pos), portIndex);
			return true;
		}
		return false;
	}

	public boolean updateInputPorts(boolean[] values) {
		boolean updated = false;
		for (int i = 0; i < 4; i++) {
			updated = updated || updateInputPort(i, values[i]);
		}
		if (updated) {
			// TODO edge trigger support
			// TODO interrupt processor
		}
		return updated;
	}

	private boolean updateInputPort(int portIndex, boolean value) {
		byte[] registers = processor.getRegisters();
		byte ports = registers[Register.PORTS.ordinal()];
		if (!ByteUtil.getBit(ports, portIndex) && prevPortValues[portIndex] != value) {
			prevPortValues[portIndex] = value;
			registers[Register.PF.ordinal() + portIndex] = value ? (byte) 1 : 0;
			return true;
		}
		return false;
	}

	private boolean getPortSignal(int portIndex) {
		boolean outputMode = ByteUtil.getBit(processor.getRegisters()[Register.PORTS.ordinal()], portIndex);
		if (!outputMode) {
			return false;
		}
		return ByteUtil.getBit(processor.getRegisters()[Register.PF.ordinal() + portIndex], 0);
	}

	public boolean getFrontPortSignal() {
		return getPortSignal(0);
	}

	public boolean getBackPortSignal() {
		return getPortSignal(1);
	}

	public boolean getLeftPortSignal() {
		return getPortSignal(2);
	}

	public boolean getRightPortSignal() {
		return getPortSignal(3);
	}

	public void reset() {
		if(world.isRemote){
			return;
		}
		System.out.println("reset");
		processor.reset();
		for(int portIndex = 0; portIndex < 4; portIndex++){
			 detectOutputChange(portIndex);
		}
	}

}
