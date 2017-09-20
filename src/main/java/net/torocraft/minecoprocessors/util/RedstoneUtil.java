package net.torocraft.minecoprocessors.util;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class RedstoneUtil {

  public static EnumFacing convertPortIndexToFacing(EnumFacing facing, int portIndex) {
    int rotation = getRotation(facing);
    return rotateFacing(EnumFacing.getFront(portIndex + 2), rotation);
  }

  public static int convertFacingToPortIndex(EnumFacing facing, EnumFacing side) {
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

  public static int portToPower(byte port) {
    return port & 0x0f;
  }

  public static byte powerToPort(int powerValue) {
    if (powerValue > 15) {
      powerValue = 15;
    }
    return (byte) powerValue;
  }

  public static boolean isFrontPort(IBlockState blockState, EnumFacing side) {
    return blockState.getValue(BlockHorizontal.FACING) == side;
  }

  public static boolean isBackPort(IBlockState blockState, EnumFacing side) {
    return blockState.getValue(BlockHorizontal.FACING).getOpposite() == side;
  }

  public static boolean isLeftPort(IBlockState blockState, EnumFacing side) {
    return blockState.getValue(BlockHorizontal.FACING).rotateYCCW() == side;
  }

  public static boolean isRightPort(IBlockState blockState, EnumFacing side) {
    return blockState.getValue(BlockHorizontal.FACING).rotateY() == side;
  }

  public static BlockPos getFrontBlock(IBlockAccess blockAccess, BlockPos pos) {
    return pos.offset(blockAccess.getBlockState(pos).getValue(BlockHorizontal.FACING));
  }

}
