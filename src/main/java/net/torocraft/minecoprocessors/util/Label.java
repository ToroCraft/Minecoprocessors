package net.torocraft.minecoprocessors.util;

import net.minecraft.nbt.NBTTagCompound;

public class Label {

  private static final String NBT_ADDRESS = "address";
  private static final String NBT_NAME = "name";

  public short address;
  public String name;

  public Label(short address, String name) {
    this.address = address;
    this.name = name;
  }

  public NBTTagCompound toNbt() {
    NBTTagCompound c = new NBTTagCompound();
    c.setShort(NBT_ADDRESS, address);
    c.setString(NBT_NAME, name);
    return c;
  }

  public static Label fromNbt(NBTTagCompound c) {
    short address = c.getShort(NBT_ADDRESS);
    String name = c.getString(NBT_NAME);
    return new Label(address, name);
  }

  @Override
  public String toString() {
    return name + "[" + address + "]";
  }
}