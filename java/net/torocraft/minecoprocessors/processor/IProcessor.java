package net.torocraft.minecoprocessors.processor;

import net.minecraft.nbt.NBTTagCompound;

public interface IProcessor {

  void reset();

  void tick();

  void wake();

  void load(String program);

  void readFromNBT(NBTTagCompound c);

  NBTTagCompound writeToNBT();

  byte[] getRegisters();
}
