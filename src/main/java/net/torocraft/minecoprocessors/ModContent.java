/*
 * @file ModContent.java
 * @license GPL
 *
 * Definition and initialisation of blocks of this
 * module, along with their tile entities if applicable.
 */
package net.torocraft.minecoprocessors;

import net.torocraft.minecoprocessors.*;
import net.torocraft.minecoprocessors.blocks.*;
import net.torocraft.minecoprocessors.gui.*;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.RegistryEvent;
import net.torocraft.minecoprocessors.items.ItemBookCode;

@SuppressWarnings("unused")
public class ModContent
{
  //--------------------------------------------------------------------------------------------------------------------
  // Blocks
  //--------------------------------------------------------------------------------------------------------------------

  public static final BlockMinecoprocessor MINECOPROCESSOR = (BlockMinecoprocessor)(new BlockMinecoprocessor(
    BlockMinecoprocessor.CONFIG_DEFAULT,
    Block.Properties.create(Material.MISCELLANEOUS, MaterialColor.STONE).hardnessAndResistance(0f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModMinecoprocessors.MODID, "processor"));

  public static final BlockMinecoprocessor MINECOPROCESSOR_OVERCLOCKED = (BlockMinecoprocessor)(new BlockMinecoprocessor(
    BlockMinecoprocessor.CONFIG_OVERCLOCKED,
    Block.Properties.create(Material.MISCELLANEOUS, MaterialColor.STONE).hardnessAndResistance(0f, 10f).sound(SoundType.STONE)
  )).setRegistryName(new ResourceLocation(ModMinecoprocessors.MODID, "overclocked_processor"));

  private static final Block MOD_BLOCKS[] = {
    MINECOPROCESSOR,
    MINECOPROCESSOR_OVERCLOCKED
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Items
  //--------------------------------------------------------------------------------------------------------------------

  public static final ItemBookCode CODE_BOOK = (ItemBookCode)(new ItemBookCode(
    (new Item.Properties()).group(ModMinecoprocessors.ITEMGROUP)
  ).setRegistryName(ModMinecoprocessors.MODID, "code_book"));

  private static final Item MOD_ITEMS[] = {
    CODE_BOOK,
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entities types
  //--------------------------------------------------------------------------------------------------------------------

  public static final TileEntityType<?> TET_MINECOPROCESSOR = TileEntityType.Builder
    .create(TileEntityMinecoprocessor::new, MOD_BLOCKS)
    .build(null)
    .setRegistryName(ModMinecoprocessors.MODID, "te_processor");

  private static final TileEntityType<?> TILE_ENTITY_TYPES[] = {
    TET_MINECOPROCESSOR
  };

  //--------------------------------------------------------------------------------------------------------------------
  // Container registration
  //--------------------------------------------------------------------------------------------------------------------

  //@todo implement
  //  public static final ContainerType<ContainerMinecoprocessor> CT_MINECOPROCESSOR = new ContainerType<ContainerMinecoprocessor>(ContainerMinecoprocessor::new);
  //
  //  // DON'T FORGET TO REGISTER THE GUI in registerContainerGuis(), no list/map format found yet for that.
  //  private static final ContainerType<?> CONTAINER_TYPES[] = {
  //    CT_MINECOPROCESSOR,
  //  };

  //--------------------------------------------------------------------------------------------------------------------
  // Initialisation events
  //--------------------------------------------------------------------------------------------------------------------

  public static void registerBlocks(final RegistryEvent.Register<Block> event)
  {
    for(Block e:MOD_BLOCKS) event.getRegistry().register(e);
  }

  public static void registerBlockItems(final RegistryEvent.Register<Item> event)
  {
    for(Block e:MOD_BLOCKS) {
      ResourceLocation rl = e.getRegistryName();
      if(rl == null) continue;
      event.getRegistry().register(new BlockItem(e, (new BlockItem.Properties().group(ModMinecoprocessors.ITEMGROUP))).setRegistryName(rl));
    }
  }

  public static void registerItems(final RegistryEvent.Register<Item> event)
  {
    for(Item e:MOD_ITEMS) event.getRegistry().register(e);
  }

  public static void registerTileEntities(final RegistryEvent.Register<TileEntityType<?>> event)
  {
    for(final TileEntityType<?> e:TILE_ENTITY_TYPES) event.getRegistry().register(e);
  }

  public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event)
  {
    //@todo implement
    // for(final ContainerType<?> e:CONTAINER_TYPES) event.getRegistry().register(e);
  }

  @OnlyIn(Dist.CLIENT)
  public static void registerContainerGuis(final FMLClientSetupEvent event)
  {
    //@todo implement
    //ScreenManager.registerFactory(CT_MINECOPROCESSOR, GuiMinecoprocessor::new);
  }

}
