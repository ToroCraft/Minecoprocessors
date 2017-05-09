package net.torocraft.minecoprocessors.blocks;

import java.util.Random;

import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;

//TODO create recipe


public class BlockMinecoprocessor extends BlockRedstoneDiode implements ITileEntityProvider {
	protected static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.1875D, 1.0D);

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
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return AABB;
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

	@Override
	public boolean canProvidePower(IBlockState state) {
		return true;
	}

	@Override
	protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
		int priority = -1;
		worldIn.updateBlockTick(pos, this, 0, priority);
	}

	public void onPortChange(World worldIn, BlockPos pos, IBlockState state, int portIndex) {
		this.notifyNeighborsOnSide(worldIn, pos, state, convertPortIndexToFacing((EnumFacing) state.getValue(FACING).getOpposite(), portIndex));
	}

	protected void notifyNeighborsOnSide(World worldIn, BlockPos pos, IBlockState state, EnumFacing side) {
		BlockPos neighborPos = pos.offset(side);
		if (net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(worldIn, pos, worldIn.getBlockState(pos), java.util.EnumSet.of(side), false)
				.isCanceled())
			return;
		worldIn.neighborChanged(neighborPos, this, pos);
		worldIn.notifyNeighborsOfStateExcept(neighborPos, this, (EnumFacing) side.getOpposite());
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		super.updateTick(world, pos, state, rand);
		updateInputPorts(world, pos, state);
	}

	public void updateInputPorts(World world, BlockPos pos, IBlockState state) {
		EnumFacing facing = (EnumFacing) state.getValue(FACING).getOpposite();

		boolean e = getPowerOnSide(world, pos.offset(EnumFacing.EAST), EnumFacing.EAST) > 0;
		boolean w = getPowerOnSide(world, pos.offset(EnumFacing.WEST), EnumFacing.WEST) > 0;
		boolean n = getPowerOnSide(world, pos.offset(EnumFacing.NORTH), EnumFacing.NORTH) > 0;
		boolean s = getPowerOnSide(world, pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH) > 0;

		boolean[] values = new boolean[4];

		values[convertFacingToPortIndex(facing, EnumFacing.NORTH)] = n;
		values[convertFacingToPortIndex(facing, EnumFacing.SOUTH)] = s;
		values[convertFacingToPortIndex(facing, EnumFacing.WEST)] = w;
		values[convertFacingToPortIndex(facing, EnumFacing.EAST)] = e;

		//System.out.println("updateTick E[" + e + "] W[" + w + "] N[" + n + "] S[" + s + "] --- F[" + values[0] + "] B[" + values[1] + "] L["
		//		+ values[2] + "] R[" + values[3] + "]");

		((TileEntityMinecoprocessor) world.getTileEntity(pos)).updateInputPorts(values);
	}

	private static EnumFacing convertPortIndexToFacing(EnumFacing facing, int portIndex) {
		int rotation = getRotation(facing);
		return rotateFacing(EnumFacing.getFront(portIndex + 2), rotation);
	}

	private static int convertFacingToPortIndex(EnumFacing facing, EnumFacing side) {
		//TODO
		int rotation = getRotation(facing);
		return rotateFacing(side, -rotation).getIndex() - 2;
	}

	private static EnumFacing rotateFacing(EnumFacing facing, int rotation) {
		if (rotation >= 0) {
			for (int i = 0; i < rotation; i++) {
				facing = facing.rotateY();
			}
		} else {
			rotation = -rotation;
			for (int i = 0; i < rotation; i++) {
				facing = facing.rotateYCCW();
			}
		}
		return facing;
	}

	private static int getRotation(EnumFacing facing) {
		switch (facing) {
		case NORTH:
			return 0;
		case EAST:
			return 1;
		case SOUTH:
			return 2;
		case WEST:
			return 3;
		default:
			return -1;
		}
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		if (stack.hasDisplayName()) {
			TileEntity tileentity = worldIn.getTileEntity(pos);

			if (tileentity instanceof TileEntityMinecoprocessor) {
				((TileEntityMinecoprocessor) tileentity).setName(stack.getDisplayName());
			}
		}
	}

	@Override
	public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		return blockState.getWeakPower(blockAccess, pos, side);
	}

	@Override
	public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		TileEntityMinecoprocessor te = ((TileEntityMinecoprocessor) blockAccess.getTileEntity(pos));

		boolean powered = false;

		powered = powered || (isFrontPort(blockState, side) && te.getFrontPortSignal());
		powered = powered || (isBackPort(blockState, side) && te.getBackPortSignal());
		powered = powered || (isLeftPort(blockState, side) && te.getLeftPortSignal());
		powered = powered || (isRightPort(blockState, side) && te.getRightPortSignal());

		return powered ? getActiveSignal(blockAccess, pos, blockState) : 0;
	}

	public boolean isFrontPort(IBlockState blockState, EnumFacing side) {
		return blockState.getValue(FACING) == side;
	}

	public boolean isBackPort(IBlockState blockState, EnumFacing side) {
		return blockState.getValue(FACING).getOpposite() == side;
	}

	public boolean isLeftPort(IBlockState blockState, EnumFacing side) {
		return blockState.getValue(FACING).rotateYCCW() == side;
	}

	public boolean isRightPort(IBlockState blockState, EnumFacing side) {
		return blockState.getValue(FACING).rotateY() == side;
	}

	public BlockPos getFrontBlock(IBlockAccess blockAccess, BlockPos pos) {
		return pos.offset((EnumFacing) blockAccess.getBlockState(pos).getValue(FACING));
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate((EnumFacing) state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation((EnumFacing) state.getValue(FACING)));
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
			float hitY, float hitZ) {

		if (!world.isRemote) {
			player.openGui(Minecoprocessors.INSTANCE, MinecoprocessorGuiHandler.MINECOPROCESSOR_ENTITY_GUI, world, pos.getX(), pos.getY(),
					pos.getZ());
		}

		return true;
	}

	@Override
	protected int getDelay(IBlockState state) {
		return 0;
	}

	@Override
	protected IBlockState getPoweredState(IBlockState unpoweredState) {
		return unpoweredState;
	}

	@Override
	protected IBlockState getUnpoweredState(IBlockState poweredState) {
		return poweredState;
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return ITEM_INSTANCE;
	}

	@Override
	public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
		return new ItemStack(ITEM_INSTANCE);
	}

	@Override
	public boolean isLocked(IBlockAccess worldIn, BlockPos pos, IBlockState state) {
		return true;
	}

	@Override
	protected boolean isAlternateInput(IBlockState state) {
		return true;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		notifyNeighbors(worldIn, pos, state);

		TileEntity tileentity = worldIn.getTileEntity(pos);
		if (tileentity instanceof TileEntityMinecoprocessor) {
			InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityMinecoprocessor) tileentity);
		}
		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		int i = 0;
		i = i | ((EnumFacing) state.getValue(FACING)).getHorizontalIndex();
		return i;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] { FACING });
	}

	public static void test() {
		testRotateFacing();
		testConvertFacingToPortIndex();
		testConvertPortIndexToFacing();
	}

	private static void testConvertPortIndexToFacing() {
		int f = 0;
		int b = 1;
		int l = 2;
		int r = 3;
		assert convertPortIndexToFacing(EnumFacing.NORTH, f).equals(EnumFacing.NORTH);
		assert convertPortIndexToFacing(EnumFacing.NORTH, r).equals(EnumFacing.EAST);
		assert convertPortIndexToFacing(EnumFacing.NORTH, b).equals(EnumFacing.SOUTH);
		assert convertPortIndexToFacing(EnumFacing.EAST, f).equals(EnumFacing.EAST);
		assert convertPortIndexToFacing(EnumFacing.EAST, b).equals(EnumFacing.WEST);
		assert convertPortIndexToFacing(EnumFacing.EAST, l).equals(EnumFacing.NORTH);
		assert convertPortIndexToFacing(EnumFacing.SOUTH, b).equals(EnumFacing.NORTH);
		assert convertPortIndexToFacing(EnumFacing.WEST, r).equals(EnumFacing.NORTH);
	}

	private static void testConvertFacingToPortIndex() {
		int f = 0;
		int b = 1;
		int l = 2;
		int r = 3;
		assert convertFacingToPortIndex(EnumFacing.NORTH, EnumFacing.NORTH) == f;
		assert convertFacingToPortIndex(EnumFacing.NORTH, EnumFacing.SOUTH) == b;
		assert convertFacingToPortIndex(EnumFacing.EAST, EnumFacing.SOUTH) == r;
		assert convertFacingToPortIndex(EnumFacing.WEST, EnumFacing.NORTH) == r;
		assert convertFacingToPortIndex(EnumFacing.SOUTH, EnumFacing.EAST) == l;
	}

	private static void testRotateFacing() {
		assert rotateFacing(EnumFacing.NORTH, -3).equals(EnumFacing.EAST);
		assert rotateFacing(EnumFacing.NORTH, 0).equals(EnumFacing.NORTH);
		assert rotateFacing(EnumFacing.EAST, 0).equals(EnumFacing.EAST);
		assert rotateFacing(EnumFacing.WEST, -2).equals(EnumFacing.EAST);
	}

}