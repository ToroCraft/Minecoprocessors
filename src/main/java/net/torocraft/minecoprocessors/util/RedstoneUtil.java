package net.torocraft.minecoprocessors.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

public class RedstoneUtil
{
  public static Direction convertPortIndexToFacing(Direction facing, int portIndex)
  { return rotateFacing(Direction.byHorizontalIndex(portIndex), getRotation(facing)); } // (portIndex & 0x3) -> already checked in byHorizontalIndex()

  public static int convertFacingToPortIndex(Direction facing, Direction side)
  { return rotateFacing(side, -getRotation(facing)).getHorizontalIndex(); }

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
