package net.torocraft.minecoprocessors.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class RedstoneUtil
{
  private static final Direction[] fwd_port_mapping = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
  private static final int[] rev_port_mapping = { 1,2,0,3 }; // S-W-N-E

  public static Direction convertPortIndexToFacing(Direction facing, int portIndex)
  { return rotateFacing(fwd_port_mapping[portIndex & 0x3], getRotation(facing)); }

  public static int convertFacingToPortIndex(Direction facing, Direction side)
  { return rev_port_mapping[rotateFacing(side, -getRotation(facing)).getHorizontalIndex() & 0x3]; }

  private static Direction rotateFacing(Direction facing, int rotation)
  {
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

  private static int getRotation(Direction facing) {
    switch (facing) {
      case NORTH: return 0;
      case EAST:  return 1;
      case SOUTH: return 2;
      case WEST:  return 3;
      default:    return -1;
    }
  }

  public static int portToPower(byte port)
  { return port & 0x0f; }

  public static byte powerToPort(int powerValue)
  { return (byte) Math.min(powerValue, 15); }

  public static boolean isFrontPort(BlockState blockState, Direction side)
  { return blockState.get(HorizontalBlock.HORIZONTAL_FACING) == side; }

  public static boolean isBackPort(BlockState blockState, Direction side)
  { return blockState.get(HorizontalBlock.HORIZONTAL_FACING).getOpposite() == side; }

  public static boolean isLeftPort(BlockState blockState, Direction side)
  { return blockState.get(HorizontalBlock.HORIZONTAL_FACING).rotateYCCW() == side; }

  public static boolean isRightPort(BlockState blockState, Direction side)
  { return blockState.get(HorizontalBlock.HORIZONTAL_FACING).rotateY() == side; }

  public static BlockPos getFrontBlock(IBlockReader blockAccess, BlockPos pos)
  { return pos.offset(blockAccess.getBlockState(pos).get(HorizontalBlock.HORIZONTAL_FACING)); }

}
