/*
 * @file ModMinecoprocessors.java
 * @license GPL
 *
 * Main mod class.
 */
package net.torocraft.minecoprocessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

    //// @todo remove ------------------------------------------------------------------------------------------------------
    //@Mod(modid = Minecoprocessors.MODID, version = Minecoprocessors.VERSION, name = Minecoprocessors.MODNAME)
    //public class Minecoprocessors {
    //  @Mod.Instance(MODID)
    //  public static Minecoprocessors INSTANCE;
    //  public static SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Minecoprocessors.MODID);
    //  @SidedProxy(clientSide = "net.torocraft.minecoprocessors.ClientProxy", serverSide = "net.torocraft.minecoprocessors.CommonProxy")
    //  public static CommonProxy proxy;
    //  @EventHandler public void preInit(FMLPreInitializationEvent e) { proxy.preInit(e); }
    //  @EventHandler public void init(FMLInitializationEvent e) { proxy.init(e); }
    //}
    //// @todo /remove -----------------------------------------------------------------------------------------------------

@Mod("minecoprocessors")
public class ModMinecoprocessors
{
  public static final String MODID = "minecoprocessors";
  public static final String MODNAME = "Minecoprocessors";
  public static final String MODVERSION = "@MOD_VERSION@";
  public static final String MODMCVERSION = "@MOD_MCVERSION@";
  public static final String MODFINGERPRINT = "@MOD_SIGNSHA1@";
  public static final String MODBUILDID = "@MOD_BUILDID@";
  public static final int VERSION_DATAFIXER = 0;
  private static final Logger LOGGER = LogManager.getLogger();

  // -------------------------------------------------------------------------------------------------------------------

  public ModMinecoprocessors()
  {
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
    {
      ModContent.registerBlockItems(event);
      ModContent.registerItems(event);
    }

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
      // Networking.init();

      /// @todo THIS WAS CommonProxy.preInit() -----------
      // public void preInit(FMLPreInitializationEvent e) {
      //    logger = e.getModLog();
      //    int packetId = 0;
      //    MessageEnableGuiUpdates.init(packetId++);
      //    MessageProcessorUpdate.init(packetId++);
      //    MessageProcessorAction.init(packetId++);
      //    MessageBookCodeData.init(packetId++);
      //    TileEntityMinecoprocessor.init();
      //    MinecoprocessorGuiHandler.init();
      //  }
      /// @todo ------------------------------------------
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event)
    {}

    @SubscribeEvent
    public static void onConfigLoad(net.minecraftforge.fml.config.ModConfig.Loading event)
    { ModConfig.onLoad(event.getConfig()); }

    @SubscribeEvent
    public static void onConfigChanged(net.minecraftforge.fml.config.ModConfig.ConfigReloading event)
    { ModConfig.onFileChange(event.getConfig()); }

  }

  // -------------------------------------------------------------------------------------------------------------------
  // Sided proxy functionality
  // -------------------------------------------------------------------------------------------------------------------

  public static final ISidedProxy proxy = DistExecutor.runForDist(()->ClientProxy::new, ()->ServerProxy::new);

  public interface ISidedProxy
  {
    default @Nullable PlayerEntity getPlayerClientSide() { return null; }
    default @Nullable World getWorldClientSide() { return null; }
    default @Nullable Minecraft mc() { return null; }
    default String i18nFormat(String key, Object... parameters) { return key; }
    default void handleUnexpectedException(Exception e) { e.printStackTrace(); }
  }

  public static final class ClientProxy implements ISidedProxy
  {
    public @Nullable PlayerEntity getPlayerClientSide() { return Minecraft.getInstance().player; }
    public @Nullable World getWorldClientSide() { return Minecraft.getInstance().world; }
    public @Nullable Minecraft mc() { return Minecraft.getInstance(); }
    public String i18nFormat(String key, Object... parameters) { return I18n.format(key, parameters); }

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

}
