package net.torocraft.minecoprocessors.blocks;

import java.util.Random;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;

// smoke particles

public class BlockMinecoprocessor extends BlockRedstoneDiode implements ITileEntityProvider {

  protected static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.1875D, 1.0D);

  public static final PropertyBool ACTIVE = PropertyBool.create("active");
  public static final PropertyBool HOT = PropertyBool.create("hot");

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

    GameRegistry.addRecipe(new ItemStack(BlockMinecoprocessor.INSTANCE), "tct", "crc", "tct", 't', Blocks.REDSTONE_TORCH, 'r', Blocks.REDSTONE_BLOCK, 'c', Items.COMPARATOR

    );
  }

  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    smokeTick(state, world, pos, rand);
  }

  private void smokeTick(IBlockState state, World world, BlockPos pos, Random rand) {
    boolean isHot = state.getValue(HOT);
    if (isHot) {
      double x, y, z;
      for (int i = 0; i < rand.nextInt(4) + 4; i++) {
        x = (double) ((float) pos.getX() + 0.5F) + (double) (rand.nextFloat() - 0.5F) * 0.5D;
        y = (double) ((float) pos.getY() + 0.4F) + (double) (rand.nextFloat() - 0.5F) * 0.2D;
        z = (double) ((float) pos.getZ() + 0.5F) + (double) (rand.nextFloat() - 0.5F) * 0.5D;
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, 0.0D, 0.0D, new int[0]);
      }
    }
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, new IProperty[] {FACING, ACTIVE, HOT});
  }

  protected IBlockState getPoweredState(IBlockState unpoweredState) {
    return getUnpoweredState(unpoweredState);
  }

  protected IBlockState getUnpoweredState(IBlockState poweredState) {
    Boolean obool = (Boolean) poweredState.getValue(ACTIVE);
    EnumFacing enumfacing = (EnumFacing) poweredState.getValue(FACING);
    return INSTANCE.getDefaultState().withProperty(FACING, enumfacing).withProperty(ACTIVE, obool);
  }

  /**
   * Convert the given metadata into a BlockState for this Block
   */
  public IBlockState getStateFromMeta(int meta) {
    return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta)).withProperty(ACTIVE, Boolean.valueOf((meta & 8) > 0)).withProperty(HOT,
        Boolean.valueOf((meta & 4) > 0));
  }

  /**
   * Convert the BlockState into the correct metadata value
   */
  public int getMetaFromState(IBlockState state) {
    int i = 0;
    i = i | ((EnumFacing) state.getValue(FACING)).getHorizontalIndex();

    if (((Boolean) state.getValue(ACTIVE)).booleanValue()) {
      i |= 8;
    }

    if (((Boolean) state.getValue(HOT)).booleanValue()) {
      i |= 4;
    }

    return i;
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
    this.setDefaultState(
        this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(HOT, Boolean.valueOf(false)).withProperty(ACTIVE, Boolean.valueOf(false)));
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
    worldIn.notifyNeighborsOfStateChange(pos, this);
    this.notifyNeighborsOnSide(worldIn, pos, state, convertPortIndexToFacing((EnumFacing) state.getValue(FACING).getOpposite(), portIndex));
  }

  protected void notifyNeighborsOnSide(World worldIn, BlockPos pos, IBlockState state, EnumFacing side) {
    BlockPos neighborPos = pos.offset(side);
    worldIn.notifyNeighborsOfStateChange(neighborPos, this);
    worldIn.notifyNeighborsOfStateExcept(neighborPos, this, (EnumFacing) side.getOpposite());
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    super.updateTick(world, pos, state, rand);
    updateInputPorts(world, pos, state);

    if (world.isRemote) {
      return;
    }

    TileEntityMinecoprocessor te = (TileEntityMinecoprocessor) world.getTileEntity(pos);

    boolean changed = false;

    boolean blockActive = ((Boolean) state.getValue(ACTIVE)).booleanValue();
    boolean processorActive = !te.getProcessor().isWait() && !te.getProcessor().isFault();

    if (blockActive && !processorActive) {
      state = state.withProperty(ACTIVE, Boolean.valueOf(false));
      changed = true;
    } else if (!blockActive && processorActive) {
      state = state.withProperty(ACTIVE, Boolean.valueOf(true));
      changed = true;
    }

    boolean blockIsHot = ((Boolean) state.getValue(HOT)).booleanValue();
    boolean processorIsHot = te.getProcessor().isHot();

    if (blockIsHot && !processorIsHot) {
      state = state.withProperty(HOT, Boolean.valueOf(false));
      changed = true;
    } else if (!blockIsHot && processorIsHot) {
      state = state.withProperty(HOT, Boolean.valueOf(true));
      changed = true;
    }

    if (changed) {
      world.setBlockState(pos, state, 2);
    }

  }

  public void updateInputPorts(World world, BlockPos pos, IBlockState state) {
    if (world.isRemote) {
      return;
    }
    EnumFacing facing = (EnumFacing) state.getValue(FACING).getOpposite();

    boolean e = calculateInputStrength(world, pos.offset(EnumFacing.EAST), EnumFacing.EAST) > 0;
    boolean w = calculateInputStrength(world, pos.offset(EnumFacing.WEST), EnumFacing.WEST) > 0;
    boolean n = calculateInputStrength(world, pos.offset(EnumFacing.NORTH), EnumFacing.NORTH) > 0;
    boolean s = calculateInputStrength(world, pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH) > 0;

    boolean[] values = new boolean[4];

    values[convertFacingToPortIndex(facing, EnumFacing.NORTH)] = n;
    values[convertFacingToPortIndex(facing, EnumFacing.SOUTH)] = s;
    values[convertFacingToPortIndex(facing, EnumFacing.WEST)] = w;
    values[convertFacingToPortIndex(facing, EnumFacing.EAST)] = e;

    ((TileEntityMinecoprocessor) world.getTileEntity(pos)).updateInputPorts(values);
  }

  protected int calculateInputStrength(World worldIn, BlockPos pos, EnumFacing enumfacing) {
    BlockPos blockpos = pos;
    IBlockState adjacentState = worldIn.getBlockState(blockpos);
    Block block = adjacentState.getBlock();
    
    int i = worldIn.getRedstonePower(blockpos, enumfacing);

    if (i >= 15) {
      return i;
    }

    int redstoneWirePower = 0;

    if (block == Blocks.REDSTONE_WIRE) {
      redstoneWirePower = ((Integer) adjacentState.getValue(BlockRedstoneWire.POWER)).intValue();
    }

    return Math.max(i, redstoneWirePower);

  }

  private static EnumFacing convertPortIndexToFacing(EnumFacing facing, int portIndex) {
    int rotation = getRotation(facing);
    return rotateFacing(EnumFacing.getFront(portIndex + 2), rotation);
  }

  private static int convertFacingToPortIndex(EnumFacing facing, EnumFacing side) {
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
  public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(HOT, Boolean.valueOf(false))
        .withProperty(ACTIVE, Boolean.valueOf(false));
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
    
    if(te.getWorld().isRemote){
      return 0;
    }

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
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      player.openGui(Minecoprocessors.INSTANCE, MinecoprocessorGuiHandler.MINECOPROCESSOR_ENTITY_GUI, world, pos.getX(), pos.getY(), pos.getZ());
    }

    return true;
  }

  @Override
  protected int getDelay(IBlockState state) {
    return 0;
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
