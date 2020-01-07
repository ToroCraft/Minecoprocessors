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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkHooks;
import net.torocraft.minecoprocessors.ModContent;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class MinecoprocessorBlock extends HorizontalBlock
{
  public static final long CONFIG_DEFAULT     = 0x0000000000000000L;
  public static final long CONFIG_OVERCLOCKED = 0x0000000000000001L;  // It's the overclocked version
  public final long config;

  public MinecoprocessorBlock(long config, Block.Properties properties)
  { super(properties); this.config = config; }

  // Block -------------------------------------------------------------------------------------------------------------

  protected static final VoxelShape SHAPE = Block.makeCuboidShape(0,0,0, 16,3,16);
  public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

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
  public boolean isSolid(BlockState state)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public PushReaction getPushReaction(BlockState state)
  { return PushReaction.DESTROY; }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
  { super.fillStateContainer(builder); builder.add(HORIZONTAL_FACING, POWERED); }

  @Override
  @SuppressWarnings("deprecation")
  public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos)
  { return func_220064_c(world, pos.down()); } // <-- isTopSolid()

  @Override
  @Nullable
  public BlockState getStateForPlacement(BlockItemUseContext context)
  {
    Direction facing = context.getPlacementHorizontalFacing();
    return super.getStateForPlacement(context).with(HORIZONTAL_FACING, facing).with(POWERED, false);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
  {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    if(world.isRemote()) return;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof MinecoprocessorTileEntity)) return;
    if(stack.hasDisplayName()) {
      ((MinecoprocessorTileEntity)te).setCustomName(stack.getDisplayName());
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
  {
    super.onReplaced(state, world, pos, newState, isMoving);
    if((!world.isRemote()) && (newState.getBlock() != state.getBlock())) {
      world.notifyNeighbors(pos, newState.getBlock());
      for(Direction side: Direction.values()) {
        world.notifyNeighborsOfStateExcept(pos.offset(side), newState.getBlock(), side.getOpposite());
      }
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState rotate(BlockState state, Rotation rot)
  { return state.with(HORIZONTAL_FACING, rot.rotate(state.get(HORIZONTAL_FACING))); }

  @Override
  @SuppressWarnings("deprecation")
  public BlockState mirror(BlockState state, Mirror mirrorIn)
  { return state; }

  @Override
  public boolean shouldCheckWeakPower(BlockState state, IWorldReader world, BlockPos pos, Direction side)
  { return false; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(BlockState state)
  { return true; }

  @Override
  public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public boolean hasComparatorInputOverride(BlockState state)
  { return false; }

  @Override
  public boolean getWeakChanges(BlockState state, IWorldReader world, BlockPos pos)
  { return true; }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(BlockState state, IBlockReader world, BlockPos pos, Direction side)
  { return getPower(state, world, pos, side, false); }

  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(BlockState state, IBlockReader world, BlockPos pos, Direction side)
  { return getPower(state, world, pos, side, true); }

  @Override
  public boolean hasTileEntity(BlockState state)
  { return true; }

  @Override
  public TileEntity createTileEntity(BlockState state, IBlockReader world)
  { return new MinecoprocessorTileEntity(ModContent.TET_MINECOPROCESSOR); }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
  {
    if(world.isRemote()) return;
    super.neighborChanged(state, world, pos, block, fromPos, isMoving);
    final Vec3i directionVector = fromPos.subtract(pos);
    if(isMoving || (!state.get(POWERED)) || (directionVector.getY() != 0)) return; // nothing to do then.
    final TileEntity te = world.getTileEntity(pos);
    if(te instanceof MinecoprocessorTileEntity) ((MinecoprocessorTileEntity)te).neighborChanged(fromPos);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
  {
    if(world.isRemote) return true;
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof MinecoprocessorTileEntity)) return true;
    if((!(player instanceof ServerPlayerEntity) && (!(player instanceof FakePlayer)))) return true;
    NetworkHooks.openGui((ServerPlayerEntity)player,(INamedContainerProvider)te);
    return true;
  }

  @Override
  public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, IFluidState fluid)
  { return dropBlock(state, world, pos, player); }

  @Override
  public void onExplosionDestroy(World world, BlockPos pos, Explosion explosion)
  { dropBlock(world.getBlockState(pos), world, pos, null); }

  @Override
  @SuppressWarnings("deprecation")
  public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
  { return Collections.singletonList(ItemStack.EMPTY); }

  @Override
  public int tickRate(IWorldReader world)
  { return 2; }

  @Override
  @SuppressWarnings("deprecation")
  public void tick(BlockState state, World world, BlockPos pos, Random random)
  {}

  // -------------------------------------------------------------------------------------------------------------------

  private int getPower(BlockState state, IBlockReader world, BlockPos pos, Direction side, boolean strong)
  {
    final TileEntity te = world.getTileEntity(pos);
    if(!(te instanceof MinecoprocessorTileEntity)) return 0;
    return ((MinecoprocessorTileEntity)te).getPower(state, side, strong);
  }

  private static boolean dropBlock(BlockState state, World world, BlockPos pos, @Nullable PlayerEntity player)
  {
    if(!(state.getBlock() instanceof MinecoprocessorBlock)) {
      world.removeBlock(pos, false);
      return true;
    }
    if(!world.isRemote()) {
      if((player==null) || (!player.isCreative())) {
        world.addEntity(new ItemEntity(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, new ItemStack(state.getBlock().asItem())));
      }
      if(world.getTileEntity(pos) instanceof MinecoprocessorTileEntity) {
        ItemStack book = ((MinecoprocessorTileEntity)world.getTileEntity(pos)).getStackInSlot(0);
        if(!book.isEmpty()) world.addEntity(new ItemEntity(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, book));
      }
    }
    world.removeTileEntity(pos);
    world.removeBlock(pos, false);
    return true;
  }
}
