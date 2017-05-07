package net.torocraft.minecoprocessors;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;

public class CommonProxy {

	public void preInit(FMLPreInitializationEvent e) {
		BlockMinecoprocessor.init();
	}

	public void init(FMLInitializationEvent e) {
		TileEntityMinecoprocessor.init();
	}

	public void postInit(FMLPostInitializationEvent e) {

	}
}
