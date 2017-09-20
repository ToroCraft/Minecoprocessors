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
    // TODO determine how the ADC will work
    if (port == 0) {
      return  0;
    }

    int i = Byte.toUnsignedInt(port);

    i = i >> 4;

    System.out.println(port + " -> " + i);
    return i;
  }

  private static void testPortToPower() {
    // TODO determine how the ADC will work
    portToPower((byte)0xf0);
//    assert portToPower((byte)0) == 0;
//    assert portToPower((byte)1) == 1;
//    assert portToPower((byte)17) == 1;
//    assert portToPower((byte)18) == 2;
//    assert portToPower((byte)33) == 2;
//    assert portToPower((byte)34) == 3;
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

  public static void test() {
    testRotateFacing();
    testConvertFacingToPortIndex();
    testConvertPortIndexToFacing();
    testPortToPower();
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
