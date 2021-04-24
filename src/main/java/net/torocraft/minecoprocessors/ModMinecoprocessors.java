/*
 * @file ModMinecoprocessors.java
 * @license GPL
 *
 * Main mod class.
 */
package net.torocraft.minecoprocessors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.network.Networking;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


@Mod("minecoprocessors")
public class ModMinecoprocessors
{
  public static final String MODID = "minecoprocessors";
  public static final String MODNAME = "Minecoprocessors";
  public static final int VERSION_DATAFIXER = 0;
  private static final Logger LOGGER = LogManager.getLogger();

  // -------------------------------------------------------------------------------------------------------------------

  public ModMinecoprocessors()
  {
    logGitVersion();
    MinecraftForge.EVENT_BUS.register(this);
    // ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG_SPEC); not needed yet
    ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_CONFIG_SPEC);
  }

  // -------------------------------------------------------------------------------------------------------------------

  public static final Logger logger() { return LOGGER; }

  // -------------------------------------------------------------------------------------------------------------------
  // Events
  // -------------------------------------------------------------------------------------------------------------------

  @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
  public static final class ForgeEvents
  {
    @SubscribeEvent
    public static final void onBlocksRegistry(final RegistryEvent.Register<Block> event)
    { ModContent.registerBlocks(event); }

    @SubscribeEvent
    public static final void onItemRegistry(final RegistryEvent.Register<Item> event)
    { ModContent.registerBlockItems(event); ModContent.registerItems(event); }

    @SubscribeEvent
    public static final void onTileEntityRegistry(final RegistryEvent.Register<TileEntityType<?>> event)
    { ModContent.registerTileEntities(event); }

    @SubscribeEvent
    public static final void onRegisterContainerTypes(final RegistryEvent.Register<ContainerType<?>> event)
    { ModContent.registerContainers(event); }

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event)
    {
      ModConfig.apply();
      Networking.init();
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {
      ModContent.registerGuis(event);
      ModContent.processContentClientSide(event);
    }

    @SubscribeEvent
    public static void onConfigLoad(net.minecraftforge.fml.config.ModConfig.Loading event)
    { ModConfig.onLoad(event.getConfig()); }

    @SubscribeEvent
    public static void onConfigChanged(net.minecraftforge.fml.config.ModConfig.Reloading event)
    { ModConfig.onFileChange(event.getConfig()); }

  }

  // -------------------------------------------------------------------------------------------------------------------
  // Sided proxy functionality
  // -------------------------------------------------------------------------------------------------------------------

  public static final ISidedProxy proxy = DistExecutor.unsafeRunForDist(()->ClientProxy::new, ()->ServerProxy::new);

  public interface ISidedProxy
  {
    default @Nullable PlayerEntity getPlayerClientSide() { return null; }
    default @Nullable World getWorldClientSide() { return null; }
    default @Nullable Minecraft mc() { return null; }
    default void handleUnexpectedException(Exception e) { e.printStackTrace(); }
  }

  public static final class ClientProxy implements ISidedProxy
  {
    public @Nullable PlayerEntity getPlayerClientSide() { return Minecraft.getInstance().player; }
    public @Nullable World getWorldClientSide() { return Minecraft.getInstance().world; }
    public @Nullable Minecraft mc() { return Minecraft.getInstance(); }

    private static boolean toldPlayerAboutException = false;
    public void handleUnexpectedException(Exception e) {
      if(!toldPlayerAboutException) {
        toldPlayerAboutException = true;
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("minecoprocessors.error_chat"));
      }
    }
  }

  public static final class ServerProxy implements ISidedProxy
  {
    public @Nullable PlayerEntity getPlayerClientSide() { return null; }
    public @Nullable World getWorldClientSide() { return null; }
    public @Nullable Minecraft mc() { return null; }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Item group / creative tab
  // -------------------------------------------------------------------------------------------------------------------

  public static final ItemGroup ITEMGROUP = (new ItemGroup("tab" + MODID) {
    @OnlyIn(Dist.CLIENT)
    public ItemStack createIcon()
    { return new ItemStack(ModContent.MINECOPROCESSOR); }
  });

  // -------------------------------------------------------------------------------------------------------------------
  // Version info
  // -------------------------------------------------------------------------------------------------------------------

  public static void logGitVersion()
  {
    try {
      InputStream is = ModMinecoprocessors.class.getResourceAsStream("/.gitversion-" + MODID);
      if(is==null) return;
      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String version = br.lines().collect(Collectors.joining("\n"));
      LOGGER.info(MODNAME + ((version.isEmpty())?(" (dev build)"):(" GIT id #"+version)) + ".");
    } catch(Throwable e) {
      return; // don't log resource not there.
    }
  }
}
