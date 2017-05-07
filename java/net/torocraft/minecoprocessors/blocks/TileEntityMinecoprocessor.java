package net.torocraft.minecoprocessors.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.processor.IProcessor;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;

public class TileEntityMinecoprocessor extends TileEntity implements ITickable {

	private static final String NAME = "minecoprocessor_tile_entity";

	private static final String NBT_PROCESSOR = "processor";

	private final IProcessor processor = new Processor();

	public static void init() {
		GameRegistry.registerTileEntity(TileEntityMinecoprocessor.class, NAME);
	}

	public TileEntityMinecoprocessor() {
		loadSampleProgram();
	}

	private void loadSampleProgram() {
		String program = "";
		program += "start: \n";
		program += "mov c, 10 \n";
		program += "label: \n";
		program += "sub c, 1 \n";
		program += "jnz label \n";
		program += "mov n, 1 \n";
		program += "jmp start \n";
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
		if(world.isRemote || world.getTotalWorldTime() % 20 != 0){
			return;
		}
		
		System.out.println("tick");
		processor.tick();
	}

	public void updatePorts(boolean e, boolean w, boolean n, boolean s) {

		byte[] registers = processor.getRegisters();

		registers[Register.E.ordinal()] = e ? (byte) 1 : 0;
		registers[Register.W.ordinal()] = w ? (byte) 1 : 0;
		registers[Register.N.ordinal()] = n ? (byte) 1 : 0;
		registers[Register.S.ordinal()] = s ? (byte) 1 : 0;

	}

	public void reset() {
		System.out.println("reset");
		processor.reset();
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
