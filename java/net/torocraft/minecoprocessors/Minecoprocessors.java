package net.torocraft.minecoprocessors;

import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Minecoprocessors.MODID, version = Minecoprocessors.VERSION)
public class Minecoprocessors {
	public static final String MODID = "minecoprocessors";
	public static final String VERSION = "1.0";

	@EventHandler
	public void init(FMLInitializationEvent event) {
		// some example code
		System.out.println("DIRT BLOCK >> " + Blocks.DIRT.getUnlocalizedName());
	}
}
