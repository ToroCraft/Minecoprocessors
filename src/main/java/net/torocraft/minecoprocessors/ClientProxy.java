package net.torocraft.minecoprocessors;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;

public class ClientProxy extends CommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent e) {
    super.preInit(e);
  }

  @Override
  public void init(FMLInitializationEvent e) {
    super.init(e);
    BlockMinecoprocessor.registerRenders();
  }

  @Override
  public void postInit(FMLPostInitializationEvent e) {
    super.postInit(e);
  }

}