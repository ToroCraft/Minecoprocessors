package net.torocraft.minecoprocessors.blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.network.MessageProcessorUpdate;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.ByteUtil;

public class TileEntityMinecoprocessor extends TileEntity implements ITickable, IInventory {

  private static final String NAME = "minecoprocessor_tile_entity";
  private static final String NBT_PROCESSOR = "processor";
  private static final String NBT_LOAD_TIME = "loadTime";
  private static final String NBT_CUSTOM_NAME = "CustomName";

  private final Processor processor = new Processor();

  private ItemStack[] codeItemStacks = new ItemStack[1];
  private String customName;
  private int loadTime;
  private boolean loaded;
  private Set<EntityPlayerMP> playersToUpdate = new HashSet<>();

  private final boolean[] prevPortValues = new boolean[4];
  private byte prevPortsRegister = 0x0f;

  private boolean prevIsInactive;
  private boolean prevIsHot;

  public static void init() {
    GameRegistry.registerTileEntity(TileEntityMinecoprocessor.class, NAME);
  }

  public void onLoad() {
    //processor.wake();
  }

  @Override
  public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
    return BlockMinecoprocessor.INSTANCE != oldState.getBlock() || BlockMinecoprocessor.INSTANCE != newState.getBlock();
  }

  @Override
  public void readFromNBT(NBTTagCompound c) {
    super.readFromNBT(c);
    processor.readFromNBT(c.getCompoundTag(NBT_PROCESSOR));

    this.codeItemStacks = new ItemStack[this.getSizeInventory()];


    loadTime = c.getShort(NBT_LOAD_TIME);

    if (c.hasKey(NBT_CUSTOM_NAME, 8)) {
      this.customName = c.getString(NBT_CUSTOM_NAME);
    }


    //////// ItemStackHelper.(c, codeItemStacks);

    NBTTagList nbttaglist = c.getTagList("Items", 10);

    for (int i = 0; i < nbttaglist.tagCount(); ++i)
    {
      NBTTagCompound nbttagcompound = nbttaglist.getCompoundTagAt(i);
      int j = nbttagcompound.getByte("Slot") & 255;

      if (j >= 0 && j < this.codeItemStacks.length)
      {
        this.codeItemStacks[j] = ItemStack.loadItemStackFromNBT(nbttagcompound);
      }
    }



  }

  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound cIn) {
    NBTTagCompound c = super.writeToNBT(cIn);
    c.setTag(NBT_PROCESSOR, processor.writeToNBT());

    c.setShort(NBT_LOAD_TIME, (short) loadTime);


    if (this.hasCustomName()) {
      c.setString(NBT_CUSTOM_NAME, this.customName);
    }

    ///ItemStackHelper.saveAllItems(c, codeItemStacks);

    NBTTagList nbttaglist = new NBTTagList();

    for (int i = 0; i < this.codeItemStacks.length; ++i)
    {
      if (this.codeItemStacks[i] != null)
      {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        nbttagcompound.setByte("Slot", (byte)i);
        this.codeItemStacks[i].writeToNBT(nbttagcompound);
        nbttaglist.appendTag(nbttagcompound);
      }
    }

    c.setTag("Items", nbttaglist);

    return c;
  }

  @Override
  public void update() {
    if (worldObj.isRemote) {
      return;
    }

    if (worldObj.getTotalWorldTime() % 2 != 0) {
      return;
    }

    boolean isInactive = processor.isWait() || processor.isFault();

    if (prevIsInactive != isInactive) {
      prevIsInactive = isInactive;
      int priority = -1;
      worldObj.updateBlockTick(pos, BlockMinecoprocessor.INSTANCE, 0, priority);
    }

    if (prevIsHot != processor.isHot()) {
      prevIsHot = processor.isHot();
      int priority = -1;
      worldObj.updateBlockTick(pos, BlockMinecoprocessor.INSTANCE, 0, priority);
    }

    if (!loaded) {
      Processor.reset(prevPortValues);
      prevPortsRegister = 0x0f;
      BlockMinecoprocessor.INSTANCE.updateInputPorts(worldObj, pos, worldObj.getBlockState(pos));
      loaded = true;
    }

    if (processor.tick()) {
      updatePlayers();
      detectOutputChanges();
    }

    if (prevPortsRegister != processor.getRegisters()[Register.PORTS.ordinal()]) {
      BlockMinecoprocessor.INSTANCE.updateInputPorts(worldObj, pos, worldObj.getBlockState(pos));
      prevPortsRegister = processor.getRegisters()[Register.PORTS.ordinal()];
    }
  }

  public void updatePlayers() {
    if (playersToUpdate.size() < 1) {
      return;
    }

    for (EntityPlayerMP player : playersToUpdate) {
      Minecoprocessors.NETWORK.sendTo(new MessageProcessorUpdate(processor.writeToNBT(), pos), player);
    }
  }

  private void detectOutputChanges() {
    detectOutputChange(0);
    detectOutputChange(1);
    detectOutputChange(2);
    detectOutputChange(3);
  }

  private boolean detectOutputChange(int portIndex) {
    byte[] registers = processor.getRegisters();
    byte ports = registers[Register.PORTS.ordinal()];

    boolean curVal = ByteUtil.getBit(registers[Register.PF.ordinal() + portIndex], 0);

    if (isInOutputMode(ports, portIndex) && prevPortValues[portIndex] != curVal) {
      prevPortValues[portIndex] = curVal;
      BlockMinecoprocessor.INSTANCE.onPortChange(worldObj, pos, worldObj.getBlockState(pos), portIndex);
      return true;
    }
    return false;
  }

  public boolean updateInputPorts(boolean[] values) {
    boolean updated = false;
    for (int i = 0; i < 4; i++) {
      updated = updateInputPort(i, values[i]) || updated;
    }
    if (updated) {
      processor.wake();
    }
    return updated;
  }

  private static boolean isInInputMode(byte ports, int portIndex) {
    return ByteUtil.getBit(ports, portIndex) && !ByteUtil.getBit(ports, portIndex + 4);
  }

  private static boolean isInOutputMode(byte ports, int portIndex) {
    return !ByteUtil.getBit(ports, portIndex) && !ByteUtil.getBit(ports, portIndex + 4);
  }

  // TODO support clock mode
  @SuppressWarnings("unused")
  private static boolean isInClockMode(byte ports, int portIndex) {
    return !ByteUtil.getBit(ports, portIndex) && ByteUtil.getBit(ports, portIndex + 4);
  }

  private static boolean isInResetMode(byte ports, int portIndex) {
    return ByteUtil.getBit(ports, portIndex) && ByteUtil.getBit(ports, portIndex + 4);
  }
  
  /**
   * return true for positive edge changes
   */
  private boolean updateInputPort(int portIndex, boolean value) {
    byte[] registers = processor.getRegisters();
    byte ports = registers[Register.PORTS.ordinal()];

    if (isInInputMode(ports, portIndex) && prevPortValues[portIndex] != value) {
      prevPortValues[portIndex] = value;
      registers[Register.PF.ordinal() + portIndex] = value ? (byte) 1 : 0;
      return value;
    }

    if (isInResetMode(ports, portIndex) && prevPortValues[portIndex] != value) {
      prevPortValues[portIndex] = value;

      if (value) {
        processor.reset();
        return true;
      }
      return false;
    }

    return false;
  }

  private boolean getPortSignal(int portIndex) {
    if (!isInOutputMode(processor.getRegisters()[Register.PORTS.ordinal()], portIndex)) {
      return false;
    }
    return ByteUtil.getBit(processor.getRegisters()[Register.PF.ordinal() + portIndex], 0);
  }

  public boolean getFrontPortSignal() {
    return getPortSignal(0);
  }

  public boolean getBackPortSignal() {
    return getPortSignal(1);
  }

  public boolean getLeftPortSignal() {
    return getPortSignal(2);
  }

  public boolean getRightPortSignal() {
    return getPortSignal(3);
  }

  public void reset() {
    if (worldObj.isRemote) {
      return;
    }
    processor.reset();
    for (int portIndex = 0; portIndex < 4; portIndex++) {
      detectOutputChange(portIndex);
    }
    loaded = false;
  }

  @Override
  public ItemStack getStackInSlot(int index) {
    return index >= 0 && index < getSizeInventory() ? codeItemStacks[index] : null;
  }

  @Override
  public ItemStack decrStackSize(int index, int count) {
    markDirty();
    return ItemStackHelper.getAndSplit(codeItemStacks, index, count);
  }

  @Override
  public ItemStack removeStackFromSlot(int index) {
    markDirty();
    return ItemStackHelper.getAndRemove(codeItemStacks, index);
  }

  @Override
  public void setInventorySlotContents(int index, ItemStack stack) {
    if (index != 0) {
      return;
    }

    codeItemStacks[index] = stack;

    if (stack == null) {
      unloadBook();
    } else {
      loadBook(stack);
    }

    markDirty();

  }

  private void unloadBook() {
    if (worldObj.isRemote) {
      return;
    }
    processor.load(null);
    loaded = false;
    updatePlayers();
  }

  private void loadBook(ItemStack stack) {
    if (worldObj.isRemote) {
      return;
    }

    if (!isBook(stack.getItem()) || !stack.hasTagCompound()) {
      return;
    }

    NBTTagList pages = stack.getTagCompound().getTagList("pages", 8);

    if (pages == null) {
      return;
    }

    StringBuilder code = new StringBuilder();
    for (int i = 0; i < pages.tagCount(); ++i) {
      code.append(pages.getStringTagAt(i));
      code.append("\n");
    }
    processor.load(code.toString());
    loaded = false;
    updatePlayers();
  }

  @Override
  public int getInventoryStackLimit() {
    return 64;
  }

  @Override
  public boolean isUseableByPlayer(EntityPlayer player) {
    return worldObj.getTileEntity(pos) != this ? false : player.getDistanceSq((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D;
  }

  @Override
  public void openInventory(EntityPlayer player) {}

  @Override
  public void closeInventory(EntityPlayer player) {}

  public static boolean isBook(Item item) {
    return item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK;
  }

  @Override
  public boolean isItemValidForSlot(int index, ItemStack stack) {
    return isBook(stack.getItem());
  }

  @Override
  public int getField(int id) {
    return 0;
  }

  @Override
  public void setField(int id, int value) {

  }

  @Override
  public int getFieldCount() {
    return 1;
  }

  @Override
  public String getName() {
    return hasCustomName() ? this.customName : "container.minecoprocessor";
  }

  public void setName(String name) {
    this.customName = name;
  }

  @Override
  public boolean hasCustomName() {
    return customName != null && !customName.isEmpty();
  }

  @Override
  public ITextComponent getDisplayName() {
    return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
  }

  @Override
  public int getSizeInventory() {
    return 1;
  }
/*
  @Override
  public boolean isEmpty() {
    for (ItemStack itemstack : codeItemStacks) {
      if (itemstack != null) {
        return false;
      }
    }

    return true;
  }
*/
  @Override
  public void clear() {
    codeItemStacks = new ItemStack[getSizeInventory()];
    markDirty();
  }

  public void enablePlayerGuiUpdates(EntityPlayerMP player, boolean enable) {
    if (enable) {
      playersToUpdate.add(player);
      updatePlayers();
    } else {
      playersToUpdate.remove(player);
    }
  }

  public Processor getProcessor() {
    return processor;
  }

}
