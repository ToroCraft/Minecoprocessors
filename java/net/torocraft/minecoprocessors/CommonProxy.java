package net.torocraft.minecoprocessors;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;
import net.torocraft.minecoprocessors.network.MessageProcessorRequest;
import net.torocraft.minecoprocessors.network.MessageProcessorUpdate;

public class CommonProxy {

  public void preInit(FMLPreInitializationEvent e) {
    BlockMinecoprocessor.init();
    int packetId = 0;
    MessageProcessorRequest.init(packetId++);
    MessageProcessorUpdate.init(packetId++);
  }

  public void init(FMLInitializationEvent e) {
    TileEntityMinecoprocessor.init();
    MinecoprocessorGuiHandler.init();
  }

  public void postInit(FMLPostInitializationEvent e) {

  }
}
