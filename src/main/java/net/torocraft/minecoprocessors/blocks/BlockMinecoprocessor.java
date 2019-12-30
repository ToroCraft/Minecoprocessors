/*
 * @file BlockMinecoprocessor.java
 * @license GPL
 *
 * Block of redstone processors.
 */
package net.torocraft.minecoprocessors.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.ModContent;
import javax.annotation.Nullable;


public class BlockMinecoprocessor extends Block
{
  public static final long CONFIG_DEFAULT     = 0x0000000000000000L;
  public static final long CONFIG_OVERCLOCKED = 0x0000000000000001L;  // It's the overclocked version

  public final long config;

  public BlockMinecoprocessor(long config, Block.Properties properties)
  { super(properties); this.config = config; }

  // Block -------------------------------------------------------------------------------------------------------------

  protected static final VoxelShape SHAPE = Block.makeCuboidShape(0,0,0, 16,2,16);
  public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
  public static final DirectionProperty HORIZONTAL_FACING = HorizontalBlock.HORIZONTAL_FACING;

  @Override
  @OnlyIn(Dist.CLIENT)
  public BlockRenderLayer getRenderLayer()
  { return BlockRenderLayer.CUTOUT; }

  @Override
  @SuppressWarnings("deprecation")
  public VoxelShape getShape(BlockState state, IBlockReader source, BlockPos pos, ISelectionContext selectionContext)
  { return SHAPE; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos)
  { return false; }

  @Override
  public boolean canSpawnInBlock()
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canEntitySpawn(BlockState state, IBlockReader worldIn, BlockPos pos, EntityType<?> type)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.DESTROY; }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(HORIZONTAL_FACING, ACTIVE); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos)
  { return func_220064_c(world, pos.down()); } // <-- isTopSolid()

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    Direction facing = context.getPlacementHorizontalFacing();
    if(!context.getPlayer().isSneaking()) {
      facing = facing.getOpposite();
    }
    return super.getStateForPlacement(context).with(HORIZONTAL_FACING, facing).with(ACTIVE, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
  { super.onReplaced(state, world, pos, newState, isMoving); } // @todo might be possible that we have to explicitly notify adjacent blocks due for strong power.

  @Override
  public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(BlockState state)
  { return true; }

  @Override
  public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
  { return (side.getAxis() != Direction.Axis.Y); }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(BlockState state, IBlockReader world, BlockPos pos, Direction side)
  { return getStrongPower(state, world, pos, side); }

  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(BlockState state, IBlockReader world, BlockPos pos, Direction side)
  {
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof TileEntityMinecoprocessor)) return 0; // @sw: not sure if that is still needed.
    return 2; // ((TileEntityMinecoprocessor)te).getPower(side);
  }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new TileEntityMinecoprocessor(ModContent.TET_MINECOPROCESSOR); }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    super.neighborChanged(state, world, pos, block, fromPos, isMoving);
    final Vec3i directionVector = fromPos.subtract(pos);
    if(isMoving || (!state.get(ACTIVE)) || (directionVector.getY() != 0)) {
      return; // nothing to do then.
    }
    final TileEntity te = world.getTileEntity(pos);
    if(te instanceof TileEntityMinecoprocessor) {
      // ((TileEntityMinecoprocessor)te).neighborChanged(fromPos);
      return;
    }
  }

  // -------------------------------------------------------------------------------------------------------------------

//
//  @Override
//  protected void updateState(World worldIn, BlockPos pos, BlockState state) {
//    worldIn.updateBlockTick(pos, this, 0, -1);
//  }

//  public void onPortChange(World worldIn, BlockPos pos, BlockState state, int portIndex) {
//    notifyNeighborsOnSide(worldIn, pos, RedstoneUtil.convertPortIndexToFacing(state.getValue(FACING).getOpposite(), portIndex));
//  }
//
//  protected void notifyNeighborsOnSide(World worldIn, BlockPos pos, EnumFacing side) {
//    BlockPos neighborPos = pos.offset(side);
//    if (net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(worldIn, pos, worldIn.getBlockState(pos), java.util.EnumSet.of(side), false)
//        .isCanceled()) {
//      return;
//    }
//    worldIn.neighborChanged(neighborPos, this, pos);
//    worldIn.notifyNeighborsOfStateExcept(neighborPos, this, side.getOpposite());
//  }
//
//  @Override
//  public void updateTick(World world, BlockPos pos, BlockState state, Random rand) {
//    super.updateTick(world, pos, state, rand);
//    updateInputPorts(world, pos, state);
//
//    if (world.isRemote) {
//      return;
//    }
//
//    TileEntityMinecoprocessor te = (TileEntityMinecoprocessor) world.getTileEntity(pos);
//
//    boolean changed = false;
//
//    boolean blockActive = state.getValue(ACTIVE);
//    boolean processorActive = !te.getProcessor().isWait() && !te.getProcessor().isFault();
//
//    if (blockActive && !processorActive) {
//      state = state.withProperty(ACTIVE, Boolean.valueOf(false));
//      changed = true;
//    } else if (!blockActive && processorActive) {
//      state = state.withProperty(ACTIVE, Boolean.valueOf(true));
//      changed = true;
//    }
//
//    if (changed) {
//      world.setBlockState(pos, state, 2);
//    }
//  }
//
//  public static void updateInputPorts(World world, BlockPos pos, BlockState state) {
//    if (world.isRemote) {
//      return;
//    }
//    EnumFacing facing = state.getValue(FACING).getOpposite();
//
//    int e = calculateInputStrength(world, pos.offset(EnumFacing.EAST), EnumFacing.EAST);
//    int w = calculateInputStrength(world, pos.offset(EnumFacing.WEST), EnumFacing.WEST);
//    int n = calculateInputStrength(world, pos.offset(EnumFacing.NORTH), EnumFacing.NORTH);
//    int s = calculateInputStrength(world, pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH);
//
//    int[] values = new int[4];
//
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.NORTH)] = n;
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.SOUTH)] = s;
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.WEST)] = w;
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.EAST)] = e;
//
//    ((TileEntityMinecoprocessor) world.getTileEntity(pos)).updateInputPorts(values);
//  }
//
//  protected static int calculateInputStrength(World worldIn, BlockPos pos, EnumFacing enumfacing) {
//    BlockState adjacentState = worldIn.getBlockState(pos);
//    Block block = adjacentState.getBlock();
//
//    int i = worldIn.getRedstonePower(pos, enumfacing);
//
//    if (i >= 15) {
//      return 15;
//    }
//
//    int redstoneWirePower = 0;
//
//    if (block == Blocks.REDSTONE_WIRE) {
//      redstoneWirePower = adjacentState.getValue(BlockRedstoneWire.POWER);
//    }
//
//    return Math.max(i, redstoneWirePower);
//
//  }
//
//  @Override
//  public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, EntityLivingBase placer, ItemStack stack) {
//    super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
//    if (stack.hasDisplayName()) {
//      TileEntity tileentity = worldIn.getTileEntity(pos);
//
//      if (tileentity instanceof TileEntityMinecoprocessor) {
//        ((TileEntityMinecoprocessor) tileentity).setName(stack.getDisplayName());
//      }
//    }
//  }
//
//  @Override
//  public int getStrongPower(BlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
//    return state.getWeakPower(blockAccess, pos, side);
//  }
//
//  @Override
//  public int getWeakPower(BlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
//    TileEntityMinecoprocessor te = ((TileEntityMinecoprocessor) blockAccess.getTileEntity(pos));
//
//    if (te.getWorld().isRemote) {
//      return 0;
//    }
//
//    if (RedstoneUtil.isFrontPort(state, side)) {
//      return RedstoneUtil.portToPower(te.getFrontPortSignal());
//    }
//
//    if (RedstoneUtil.isBackPort(state, side)) {
//      return RedstoneUtil.portToPower(te.getBackPortSignal());
//    }
//
//    if (RedstoneUtil.isLeftPort(state, side)) {
//      return RedstoneUtil.portToPower(te.getLeftPortSignal());
//    }
//
//    if (RedstoneUtil.isRightPort(state, side)) {
//      return RedstoneUtil.portToPower(te.getRightPortSignal());
//    }
//
//    return 0;
//  }
//
//  @Override
//  public BlockState withRotation(BlockState state, Rotation rot) {
//    return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
//  }
//
//  @Override
//  public BlockState withMirror(BlockState state, Mirror mirrorIn) {
//    return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
//  }
//
//  @Override
//  public boolean onBlockActivated(World world, BlockPos pos, BlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
//      float hitY, float hitZ) {
//
//    if (!world.isRemote) {
//      player.openGui(Minecoprocessors.INSTANCE, MinecoprocessorGuiHandler.MINECOPROCESSOR_ENTITY_GUI, world, pos.getX(), pos.getY(),
//          pos.getZ());
//    }
//
//    return true;
//  }
//
//  @Override
//  protected int getDelay(BlockState state) {
//    return 0;
//  }
//
//  @Override
//  public boolean isLocked(IBlockAccess worldIn, BlockPos pos, BlockState state) {
//    return true;
//  }
//
//  @Override
//  protected boolean isAlternateInput(BlockState state) {
//    return true;
//  }
//
//  @Override
//  public void breakBlock(World worldIn, BlockPos pos, BlockState state) {
//    notifyNeighbors(worldIn, pos, state);
//
//    TileEntity tileentity = worldIn.getTileEntity(pos);
//    if (tileentity instanceof TileEntityMinecoprocessor) {
//      InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityMinecoprocessor) tileentity);
//    }
//    super.breakBlock(worldIn, pos, state);
//  }
//
//  @Override
//  public String getSpecialName(ItemStack stack) {
//    return getUnlocalizedName() + ((stack.getItemDamage() & 4) == 0 ? "" : "_overclocked");
//  }
}
