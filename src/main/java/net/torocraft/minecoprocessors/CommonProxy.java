package net.torocraft.minecoprocessors;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;
import net.torocraft.minecoprocessors.network.MessageProcessorAction;
import net.torocraft.minecoprocessors.network.MessageEnableGuiUpdates;
import net.torocraft.minecoprocessors.network.MessageProcessorUpdate;

public class CommonProxy {

  public void preInit(FMLPreInitializationEvent e) {
    int packetId = 0;
    MessageEnableGuiUpdates.init(packetId++);
    MessageProcessorUpdate.init(packetId++);
    MessageProcessorAction.init(packetId++);
    TileEntityMinecoprocessor.init();
    MinecoprocessorGuiHandler.init();
  }

  public void init(FMLInitializationEvent e) {

  }

  public void postInit(FMLPostInitializationEvent e) {

  }
}
