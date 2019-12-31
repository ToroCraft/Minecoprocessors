/*
 * @file MinecoprocessorTileEntity.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.torocraft.minecoprocessors.ModContent;
import net.torocraft.minecoprocessors.processor.Processor;


public class MinecoprocessorTileEntity extends TileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider
{
  public static final int NUM_OF_SLOTS = 1;

  private NonNullList<ItemStack> inventory = NonNullList.withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
  private final Processor processor = new Processor();
  private final byte[] prevPortValues = new byte[4];
  private String customName;
  private int loadTime;
  private boolean loaded;
  private byte prevPortsRegister = 0x0f;
  private boolean prevIsInactive;
  private int tickTimer = 0;

  public MinecoprocessorTileEntity()
  { super(ModContent.TET_MINECOPROCESSOR); }

  public MinecoprocessorTileEntity(TileEntityType<?> te_type)
  { super(te_type); }

  // TileEntity --------------------------------------------------------------------------------------------------------

  @Override
  public void read(CompoundNBT nbt)
  {
    super.read(nbt);
    processor.setNBT(nbt.getCompound("processor"));
    inventory = NonNullList.<ItemStack>withSize(1, ItemStack.EMPTY);  // <<-- this.getSizeInventory() -> 1
    ItemStackHelper.loadAllItems(nbt, inventory);
    loadTime = nbt.getShort("loadTime");
    if(nbt.contains("CustomName", 8)) {
      this.customName = nbt.getString("CustomName");
    }
  }

  @Override
  public CompoundNBT write(CompoundNBT nbt)
  {
    super.write(nbt);
    nbt.put("processor", processor.getNBT());
    nbt.putShort("loadTime", (short)loadTime);
    ItemStackHelper.saveAllItems(nbt, inventory);
    if(this.hasCustomName()) nbt.putString("CustomName", this.customName);
    return nbt;
  }

  // INameable ---------------------------------------------------------------------------

  @Override
  public ITextComponent getName()
  { final Block block=getBlockState().getBlock(); return new StringTextComponent((block!=null) ? block.getTranslationKey() : "Minecoprocessor"); }

  @Override
  public boolean hasCustomName()
  { return false; }

  @Override
  public ITextComponent getCustomName()
  { return getName(); }

  @Override
  public ITextComponent getDisplayName()
  { return INameable.super.getDisplayName(); }

  // INamedContainerProvider ------------------------------------------------------------------------------

  @Override
  public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
  { return new MinecoprocessorContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields); }

  public static final int NUM_OF_FIELDS = 1; // Container/GUI synchronization fields

  protected final IIntArray fields = new IntArray(NUM_OF_FIELDS)
  {
    @Override
    public int get(int id)
    {
      switch(id) {
        case  0: return 0;
        default: return 0;
      }
    }
    @Override
    public void set(int id, int value)
    {
      switch(id) {
        case  0: break;
        default: return;
      }
    }
  };

  // IInventory -------------------------------------------------------------------------------------------

  @Override
  public int getSizeInventory()
  { return inventory.size(); }

  @Override
  public boolean isEmpty()
  { for(ItemStack stack: inventory) { if(!stack.isEmpty()) return false; } return true; }

  @Override
  public ItemStack getStackInSlot(int index)
  { return (index < getSizeInventory()) ? inventory.get(index) : ItemStack.EMPTY; }

  @Override
  public ItemStack decrStackSize(int index, int count)
  { onInventoryChanged(); return ItemStackHelper.getAndSplit(inventory, index, count); }

  @Override
  public ItemStack removeStackFromSlot(int index)
  { onInventoryChanged(); return ItemStackHelper.getAndRemove(inventory, index); }

  @Override
  public void setInventorySlotContents(int index, ItemStack stack)
  {
    onInventoryChanged();
    inventory.set(index, stack);
    if(stack.getCount() > getInventoryStackLimit()) stack.setCount(getInventoryStackLimit());
  }

  @Override
  public int getInventoryStackLimit()
  { return NUM_OF_SLOTS; }

  @Override
  public void markDirty()
  { super.markDirty(); }

  @Override
  public boolean isUsableByPlayer(PlayerEntity player)
  { return getPos().distanceSq(player.getPosition()) < 36; }

  @Override
  public void openInventory(PlayerEntity player)
  {}

  @Override
  public void closeInventory(PlayerEntity player)
  { markDirty(); }

  @Override
  public boolean isItemValidForSlot(int index, ItemStack stack)
  { return (!stack.isEmpty()) && (stack.getItem() == ModContent.CODE_BOOK); }

  @Override
  public void clear()
  { onInventoryChanged(); inventory.clear(); }

  // ITickable ---------------------------------------------------------------------------------------------------------

  @Override
  public void tick()
  {
    if((world.isRemote) || (--tickTimer > 0)) return;
    tickTimer = 2;
    final BlockState currentBlockState = world.getBlockState(pos);
    if(!(currentBlockState.getBlock() instanceof MinecoprocessorBlock)) return;
    final MinecoprocessorBlock block = (MinecoprocessorBlock)currentBlockState.getBlock();
    if((block.config & MinecoprocessorBlock.CONFIG_OVERCLOCKED)!=0) tickTimer = 1;

    ///------------------------------------------------------------------------------------------------------
    // test tick
    tickTimer = 20;
    if(!inventory.get(0).isEmpty()) {
      world.setBlockState(getPos(), currentBlockState.cycle(MinecoprocessorBlock.ACTIVE), 2);
    } else if(currentBlockState.get(MinecoprocessorBlock.ACTIVE)) {
      world.setBlockState(getPos(), currentBlockState.with(MinecoprocessorBlock.ACTIVE, false), 2);
    }
    ///------------------------------------------------------------------------------------------------------




    /*
    boolean isInactive = processor.isWait() || processor.isFault();
    if(prevIsInactive != isInactive) {
      prevIsInactive = isInactive;
      int priority = -1;
      world.updateBlockTick(pos, BlockMinecoprocessor.INSTANCE, 0, priority);
    }

    if (!loaded) {
      Processor.reset(prevPortValues);
      prevPortsRegister = 0x0f;
      BlockMinecoprocessor.updateInputPorts(world, pos, world.getBlockState(pos));
      loaded = true;
    }

    if (processor.tick()) {
      updatePlayers();
      detectOutputChanges();
    }

    if (prevPortsRegister != processor.getRegisters()[Register.PORTS.ordinal()]) {
      BlockMinecoprocessor.updateInputPorts(world, pos, world.getBlockState(pos));
      prevPortsRegister = processor.getRegisters()[Register.PORTS.ordinal()];
    }
    */
  }

  // Class private -----------------------------------------------------------------------------------------------------

  private void onInventoryChanged() //
  { tickTimer = 0; }


//  private Set<ServerPlayerEntity> playersToUpdate = new HashSet<ServerPlayerEntity>();
//
//  public void updatePlayers() {
//    for (EntityPlayerMP player : playersToUpdate) {
//      Minecoprocessors.NETWORK.sendTo(new MessageProcessorUpdate(processor.writeToNBT(), pos, getName()), player);
//    }
//  }
//
//  private void detectOutputChanges() {
//    detectOutputChange(0);
//    detectOutputChange(1);
//    detectOutputChange(2);
//    detectOutputChange(3);
//  }
//
//  private boolean detectOutputChange(int portIndex) {
//    byte[] registers = processor.getRegisters();
//    byte ports = registers[Register.PORTS.ordinal()];
//
//    byte curVal = registers[Register.PF.ordinal() + portIndex];
//
//    if (isInOutputMode(ports, portIndex) && prevPortValues[portIndex] != curVal) {
//      prevPortValues[portIndex] = curVal;
//      BlockMinecoprocessor.INSTANCE.onPortChange(world, pos, world.getBlockState(pos), portIndex);
//      return true;
//    }
//    return false;
//  }
//
//  public boolean updateInputPorts(int[] values) {
//    boolean updated = false;
//    for (int i = 0; i < 4; i++) {
//      updated = updateInputPort(i, values[i]) || updated;
//    }
//    if (updated) {
//      processor.wake();
//    }
//    return updated;
//  }
//
//  public static boolean isInInputMode(byte ports, int portIndex) {
//    return ByteUtil.getBit(ports, portIndex) && !ByteUtil.getBit(ports, portIndex + 4);
//  }
//
//  public static boolean isInOutputMode(byte ports, int portIndex) {
//    return !ByteUtil.getBit(ports, portIndex) && !ByteUtil.getBit(ports, portIndex + 4);
//  }
//
//  public static boolean isADCMode(byte adc, int portIndex) {
//    return ByteUtil.getBit(adc, portIndex);
//  }
//
//  public static boolean isInResetMode(byte ports, int portIndex) {
//    return ByteUtil.getBit(ports, portIndex) && ByteUtil.getBit(ports, portIndex + 4);
//  }
//
//  /**
//   * return true for positive edge changes
//   */
//  private boolean updateInputPort(int portIndex, int powerValue) {
//    byte[] registers = processor.getRegisters();
//    byte ports = registers[Register.PORTS.ordinal()];
//    byte adc = registers[Register.ADC.ordinal()];
//    byte value;
//
//    if (isADCMode(adc, portIndex)) {
//      value = RedstoneUtil.powerToPort(powerValue);
//    } else if (powerValue == 0) {
//      value = 0;
//    } else {
//      value = (byte) 0xff;
//    }
//
//    if (isInInputMode(ports, portIndex) && prevPortValues[portIndex] != value) {
//      prevPortValues[portIndex] = value;
//      registers[Register.PF.ordinal() + portIndex] = value;
//      return true;
//    }
//
//    if (isInResetMode(ports, portIndex) && prevPortValues[portIndex] != value) {
//      prevPortValues[portIndex] = value;
//
//      if (value != 0) {
//        processor.reset();
//        return true;
//      }
//    }
//
//    return false;
//  }
//
//  private byte getPortSignal(int portIndex) {
//    if (!isInOutputMode(processor.getRegisters()[Register.PORTS.ordinal()], portIndex)) {
//      return 0;
//    }
//    byte signal = processor.getRegisters()[Register.PF.ordinal() + portIndex];
//
//    if (!isADCMode(processor.getRegisters()[Register.ADC.ordinal()], portIndex)) {
//      return signal == 0 ? 0 : (byte) 0xff;
//    }
//
//    return signal;
//  }
//
//  public byte getFrontPortSignal() {
//    return getPortSignal(0);
//  }
//
//  public byte getBackPortSignal() {
//    return getPortSignal(1);
//  }
//
//  public byte getLeftPortSignal() {
//    return getPortSignal(2);
//  }
//
//  public byte getRightPortSignal() {
//    return getPortSignal(3);
//  }
//
//  public void reset() {
//    if (world.isRemote) {
//      return;
//    }
//    processor.reset();
//    for (int portIndex = 0; portIndex < 4; portIndex++) {
//      detectOutputChange(portIndex);
//    }
//    loaded = false;
//  }
//
//  @Override
//  public ItemStack getStackInSlot(int index) {
//    return index >= 0 && index < codeItemStacks.size() ? codeItemStacks.get(index) : ItemStack.EMPTY;
//  }
//
//  @Override
//  public ItemStack decrStackSize(int index, int count) {
//    markDirty();
//    return ItemStackHelper.getAndSplit(codeItemStacks, index, count);
//  }
//
//  @Override
//  public ItemStack removeStackFromSlot(int index) {
//    markDirty();
//    return ItemStackHelper.getAndRemove(codeItemStacks, index);
//  }
//
//  @Override
//  public void setInventorySlotContents(int index, ItemStack stack) {
//    if (index != 0) {
//      return;
//    }
//
//    codeItemStacks.set(index, stack);
//
//    if (stack.isEmpty()) {
//      unloadBook();
//    } else {
//      loadBook(stack);
//    }
//
//    markDirty();
//
//  }
//
//  private void unloadBook() {
//    if (world.isRemote) {
//      return;
//    }
//    processor.load(null);
//    loaded = false;
//    setName(null);
//    updatePlayers();
//  }
//
//  private static void addLines(List<String> lines, String toAdd) {
//    for (String s : toAdd.split("\\n\\r?")) {
//      lines.add(s);
//    }
//  }
//
//  private void loadBook(ItemStack stack) {
//    if (world.isRemote) {
//      return;
//    }
//
//    if (!isBook(stack.getItem()) || !stack.hasTagCompound()) {
//      return;
//    }
//
//    NBTTagList pages = stack.getTagCompound().getTagList("pages", 8);
//
//    if (pages == null) {
//      return;
//    }
//
//    boolean signed = stack.getTagCompound().hasKey("author");
//    JsonParser parser = null;
//
//    List<String> code;
//    if (CodeBookItem.isBookCode(stack)) {
//      code = CodeBookItem.Data.loadFromStack(stack).getContinuousProgram();
//    } else {
//      code = new ArrayList<>(pages.tagCount());
//      for (int i = 0; i < pages.tagCount(); ++i) {
//        if (signed) {
//          if (parser == null) {
//            parser = new JsonParser();
//          }
//          JsonObject o = parser.parse(pages.getStringTagAt(i)).getAsJsonObject();
//          addLines(code, o.get("text").getAsString());
//        } else {
//          addLines(code, pages.getStringTagAt(i));
//        }
//      }
//    }
//    updateNameFromCode(code);
//    processor.load(code);
//    loaded = false;
//    updatePlayers();
//  }
//
//  private void updateNameFromCode(List<String> code) {
//    String name = readNameFromHeader(code);
//    if ("".equals(name)) {
//      setName(null);
//    } else {
//      setName(name);
//    }
//  }
//
//  public static String readNameFromHeader(List<String> code) {
//    try {
//      List<String> nameSearch = InstructionUtil.regex("^\\s*;\\s*(.*)", code.get(0), Pattern.CASE_INSENSITIVE);
//      if (nameSearch.size() != 1) {
//        return null;
//      }
//      String name = nameSearch.get(0);
//      if (name != null && !name.isEmpty()) {
//        return name;
//      }
//    } catch (Exception e) {
//      Minecoprocessors.proxy.handleUnexpectedException(e);
//    }
//    return null;
//  }
//
//  @Override
//  public int getInventoryStackLimit() {
//    return 64;
//  }
//
//  @Override
//  public boolean isUsableByPlayer(EntityPlayer player) {
//    return world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
//  }
//
//  @Override
//  public void openInventory(EntityPlayer player) {
//  }
//
//  @Override
//  public void closeInventory(EntityPlayer player) {
//  }
//
//  public static boolean isBook(Item item) {
//    return item == CodeBookItem.INSTANCE || item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK;
//  }
//
//  @Override
//  public boolean isItemValidForSlot(int index, ItemStack stack) {
//    return isBook(stack.getItem());
//  }
//
//  @Override
//  public int getField(int id) {
//    return 0;
//  }
//
//  @Override
//  public void setField(int id, int value) {
//
//  }
//
//  @Override
//  public int getFieldCount() {
//    return 1;
//  }
//
//  @Override
//  public String getName() {
//    return hasCustomName() ? this.customName : "container.minecoprocessor";
//  }
//
//  public void setName(String name) {
//    this.customName = name;
//  }
//
//    //@Override
//    public boolean hasCustomName() {
//      return customName != null && !customName.isEmpty();
//    }
//
//  @Override
//  public ITextComponent getDisplayName() {
//    return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
//  }
//
//  @Override
//  public int getSizeInventory() {
//    return codeItemStacks.size();
//  }
//
//  @Override
//  public boolean isEmpty() {
//    for (ItemStack itemstack : codeItemStacks) {
//      if (!itemstack.isEmpty()) {
//        return false;
//      }
//    }
//
//    return true;
//  }
//
//  @Override
//  public void clear() {
//    codeItemStacks.clear();
//    markDirty();
//  }
//
//  public void enablePlayerGuiUpdates(EntityPlayerMP player, boolean enable) {
//    if (enable) {
//      playersToUpdate.add(player);
//      updatePlayers();
//    } else {
//      playersToUpdate.remove(player);
//    }
//  }
//
//  public Processor getProcessor() {
//    return processor;
//  }

}
