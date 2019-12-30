package net.torocraft.minecoprocessors.processor;

import net.minecraft.nbt.CompoundNBT;
import java.util.List;

public interface IProcessor
{
  void reset();
  boolean tick();
  void wake();
  void load(List<String> program);
  void setNBT(CompoundNBT nbt);
  CompoundNBT getNBT();
  byte[] getRegisters();
}
