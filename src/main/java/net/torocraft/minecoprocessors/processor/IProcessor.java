package net.torocraft.minecoprocessors.processor;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

public interface IProcessor {

  void reset();

  boolean tick();

  void wake();

  void load(List<String> program);

  void readFromNBT(NBTTagCompound c);

  NBTTagCompound writeToNBT();

  byte[] getRegisters();
}
