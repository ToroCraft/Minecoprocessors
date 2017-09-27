package net.torocraft.minecoprocessors;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;
import net.torocraft.minecoprocessors.network.MessageBookCodeData;
import net.torocraft.minecoprocessors.network.MessageEnableGuiUpdates;
import net.torocraft.minecoprocessors.network.MessageProcessorAction;
import net.torocraft.minecoprocessors.network.MessageProcessorUpdate;
import org.apache.logging.log4j.Logger;

public class CommonProxy {

  public Logger logger;

  public String i18nFormat(String key, Object... parameters) {
    return key;
  }

  public void handleUnexpectedException(Exception e) {
    e.printStackTrace();
  }

  public void preInit(FMLPreInitializationEvent e) {
    logger = e.getModLog();
    int packetId = 0;
    MessageEnableGuiUpdates.init(packetId++);
    MessageProcessorUpdate.init(packetId++);
    MessageProcessorAction.init(packetId++);
    MessageBookCodeData.init(packetId++);
    TileEntityMinecoprocessor.init();
    MinecoprocessorGuiHandler.init();
  }

  public void init(@SuppressWarnings("unused") FMLInitializationEvent e) {

  }
}
