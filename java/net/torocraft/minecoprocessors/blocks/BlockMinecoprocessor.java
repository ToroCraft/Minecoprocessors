package net.torocraft.minecoprocessors.blocks;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.Minecoprocessors;

public class BlockMinecoprocessor extends BlockContainer {

	public static final String NAME = "minecoprocessor";

	public static BlockMinecoprocessor INSTANCE;
	public static Item ITEM_INSTANCE;

	public static void init() {
		INSTANCE = new BlockMinecoprocessor();
		ResourceLocation resourceName = new ResourceLocation(Minecoprocessors.MODID, NAME);


		INSTANCE.setRegistryName(resourceName);
		GameRegistry.register(INSTANCE);

		ITEM_INSTANCE = new ItemBlock(INSTANCE);
		ITEM_INSTANCE.setRegistryName(resourceName);
		GameRegistry.register(ITEM_INSTANCE);

		//GameRegistry.addRecipe(new ChessControlRecipe());
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	public static void registerRenders() {
		ModelResourceLocation model = new ModelResourceLocation(Minecoprocessors.MODID + ":" + NAME, "inventory");
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(ITEM_INSTANCE, 0, model);
	}

	public BlockMinecoprocessor() {
		super(Material.GROUND);
		setUnlocalizedName(NAME);
		setResistance(0.1f);
		setHardness(0.5f);
		setLightLevel(0);
		setCreativeTab(CreativeTabs.REDSTONE);
		isBlockContainer = true;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityMinecoprocessor();
	}

}