package net.torocraft.minecoprocessors.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.Minecoprocessors;

public class BlockMinecoprocessor extends BlockRedstoneDiode implements ITileEntityProvider {

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

		// GameRegistry.addRecipe(new ChessControlRecipe());
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
		super(true);
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
		setCreativeTab(CreativeTabs.REDSTONE);
		setUnlocalizedName(NAME);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityMinecoprocessor();
	}

	public boolean canProvidePower(IBlockState state) {
		return true;
	}

	protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
		// TODO has changed logic here

		int priority = -1;

		// if facing priority = -3;
		// if powered priority = -2;

		worldIn.updateBlockTick(pos, this, this.getDelay(state), priority);

	}

	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		super.updateTick(world, pos, state, rand);

		boolean e = getPowerOnSide(world, pos.offset(EnumFacing.EAST), EnumFacing.EAST) > 0;
		boolean w = getPowerOnSide(world, pos.offset(EnumFacing.WEST), EnumFacing.WEST) > 0;
		boolean n = getPowerOnSide(world, pos.offset(EnumFacing.NORTH), EnumFacing.NORTH) > 0;
		boolean s = getPowerOnSide(world, pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH) > 0;

		System.out.println("updateTick E[" + e + "] W[" + w + "] N[" + n + "] S[" + s + "]");

		((TileEntityMinecoprocessor) world.getTileEntity(pos)).updatePorts(e, w, n, s); // removeTileEntity(pos);

	}

	/*
	 * protected int getPowerOnSide(IBlockAccess worldIn, BlockPos pos,
	 * EnumFacing side) { IBlockState iblockstate = worldIn.getBlockState(pos);
	 * 
	 * Block block = iblockstate.getBlock();
	 * 
	 * //return isAlternateInput(iblockstate) ?
	 * 
	 * (block == Blocks.REDSTONE_BLOCK ? 15 : (block == Blocks.REDSTONE_WIRE ?
	 * 
	 * ((Integer) iblockstate.getValue(BlockRedstoneWire.POWER)).intValue()
	 * 
	 * : worldIn.getStrongPower(pos, side))) // : 0; }
	 */

	/**
	 * Returns the blockstate with the given rotation from the passed
	 * blockstate. If inapplicable, returns the passed blockstate.
	 */
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		// TODO determine if the processor block should have a facing
		return state.withProperty(FACING, rot.rotate((EnumFacing) state.getValue(FACING)));
	}

	/**
	 * Returns the blockstate with the given mirror of the passed blockstate. If
	 * inapplicable, returns the passed blockstate.
	 */
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation((EnumFacing) state.getValue(FACING)));
	}

	/**
	 * Called when the block is right clicked by a player.
	 */
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
			float hitX, float hitY, float hitZ) {

		// TODO GUI!
		
		((TileEntityMinecoprocessor) worldIn.getTileEntity(pos)).reset();
		

		return false;
	}

	protected int getDelay(IBlockState state) {
		return 0;
	}

	protected IBlockState getPoweredState(IBlockState unpoweredState) {
		return unpoweredState;
	}

	protected IBlockState getUnpoweredState(IBlockState poweredState) {
		return poweredState;
	}

	/**
	 * Get the Item that this Block should drop when harvested.
	 */
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return ITEM_INSTANCE;
	}

	public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
		return new ItemStack(ITEM_INSTANCE);
	}

	public boolean isLocked(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
		return true;
	}

	protected boolean isAlternateInput(IBlockState state) {
		return true;
	}

	/**
	 * Called serverside after this block is replaced with another in Chunk, but
	 * before the Tile Entity is updated
	 */
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		super.breakBlock(worldIn, pos, state);
		this.notifyNeighbors(worldIn, pos, state);
	}

	/**
	 * Convert the given metadata into a BlockState for this Block
	 */
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
	}

	/**
	 * Convert the BlockState into the correct metadata value
	 */
	public int getMetaFromState(IBlockState state) {
		int i = 0;
		i = i | ((EnumFacing) state.getValue(FACING)).getHorizontalIndex();
		return i;
	}

	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { FACING });
	}

}