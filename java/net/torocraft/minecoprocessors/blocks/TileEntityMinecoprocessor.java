package net.torocraft.minecoprocessors.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class TileEntityMinecoprocessor extends TileEntity implements ITickable {

	private static final String NAME = "minecoprocessor_tile_entity";

	private static final String NBT_PROGRAM = "program";

	private String program;

	public static void init() {
		GameRegistry.registerTileEntity(TileEntityMinecoprocessor.class, NAME);
	}

	public TileEntityMinecoprocessor() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void readFromNBT(NBTTagCompound c) {
		super.readFromNBT(c);
		program = c.getString(NBT_PROGRAM);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound cIn) {
		NBTTagCompound c = super.writeToNBT(cIn);
		c.setString(NBT_PROGRAM, program);
		return c;
	}

	@Override
	public void update() {

	}

}
