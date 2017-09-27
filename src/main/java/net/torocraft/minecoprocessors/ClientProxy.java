package net.torocraft.minecoprocessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;

public class ClientProxy extends CommonProxy {

  private boolean toldPlayerAboutException = false;

  @Override
  public String i18nFormat(String key, Object... parameters) {
    return I18n.format(key, parameters);
  }

  @Override
  public void handleUnexpectedException(Exception e) {
    super.handleUnexpectedException(e);
    if(!toldPlayerAboutException) {
      toldPlayerAboutException = true;
      Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation("minecoprocessors.error_chat"));
    }
  }

  @Override
  public void preInit(FMLPreInitializationEvent e) {
    super.preInit(e);
    BlockMinecoprocessor.preRegisterRenders();
  }

  @Override
  public void init(FMLInitializationEvent e) {
    super.init(e);
    BlockMinecoprocessor.registerRenders();
  }

}