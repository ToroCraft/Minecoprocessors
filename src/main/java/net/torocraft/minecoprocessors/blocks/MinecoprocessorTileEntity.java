/*
 * @file MinecoprocessorTileEntity.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.WritableBookItem;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ForgeEventFactory;
import net.torocraft.minecoprocessors.ModContent;
import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.torocraft.minecoprocessors.items.CodeBookItem;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.ByteUtil;
import net.torocraft.minecoprocessors.util.InstructionUtil;
import net.torocraft.minecoprocessors.util.RedstoneUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


public class MinecoprocessorTileEntity extends TileEntity implements ITickableTileEntity, INameable, IInventory, INamedContainerProvider
{
  public static final int NUM_OF_SLOTS = 1;

  private NonNullList<ItemStack> inventory = NonNullList.withSize(NUM_OF_SLOTS, ItemStack.EMPTY);
  private final Processor processor = new Processor();
  private final byte[] prevPortValues = new byte[4];
  private String customName = new String();
  private int loadTime;
  private byte prevPortsRegister = 0x0f;
  private boolean inventoryChanged = false;
  private boolean inputsChanged = false;
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
    customName = nbt.getString("CustomName");
  }

  @Override
  public CompoundNBT write(CompoundNBT nbt)
  {
    super.write(nbt);
    ItemStackHelper.saveAllItems(nbt, inventory);
    if(hasCustomName()) nbt.putString("CustomName", customName);
    nbt.put("processor", processor.getNBT());
    nbt.putShort("loadTime", (short)loadTime);
    return nbt;
  }

  // INameable ---------------------------------------------------------------------------

  @Override
  public ITextComponent getName()
  {
    if(hasCustomName()) return new StringTextComponent(customName);
    final BlockState state = getBlockState();
    return new StringTextComponent((state!=null) ? (state.getBlock().getTranslationKey()) : ("Minecoprocessor"));
  }

  @Override
  public boolean hasCustomName()
  { return (customName != null) && (!customName.isEmpty()); }

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

  // Container/GUI synchronization fields
  public static final class ContainerSyncFields extends IntArray
  {
    public static int NUM_OF_FIELDS = 4;  //  [0,1,2]: (sizeof(regs)/sizeof(int))+1 and flags. [3]: ip,sp,fault

    public ContainerSyncFields()
    { super(NUM_OF_FIELDS); }

    public void writeServerSide(Processor processor)
    {
      final byte[] regs = processor.getRegisters();
      set(0, 0 // IP SP FAULT
        | (((int)processor.getIp()) & 0xffff)
        | ((((int)processor.getSp())<<16) & 0xff)
        | ((((int)processor.getFaultCode())<<24) & 0xffff)
      );
      set(1, (((int)regs[0])) | (((int)regs[1])<<8) | (((int)regs[2])<<16) | (((int)regs[3])<<24)); // A B C D
      set(2, (((int)regs[4])) | (((int)regs[5])<<8) | (((int)regs[6])<<16) | (((int)regs[7])<<24)); // PF PB PL PR
      set(3, (((int)regs[8])) | (((int)regs[9])<<8) | (0 // PORTS ADC FLAGS
        | (processor.isFault()    ? 0x0100 : 0)
        | (processor.isZero()     ? 0x0200 : 0)
        | (processor.isOverflow() ? 0x0400 : 0)
        | (processor.isCarry()    ? 0x0800 : 0)
        | (processor.isWait()     ? 0x1000 : 0)
        | (processor.isStep()     ? 0x2000 : 0)
      ));
    }

    // Client side getters
    public short ip()             { return (short)((get(0)>> 0) & 0xffff); }
    public byte sp()              { return (byte)((get(0)>>16) & 0xff); }
    public byte fault()           { return (byte)((get(0)>>24) & 0xff); }
    public byte a()               { return (byte)((get(1)>> 0) & 0xff); }
    public byte b()               { return (byte)((get(1)>> 8) & 0xff); }
    public byte c()               { return (byte)((get(1)>>16) & 0xff); }
    public byte d()               { return (byte)((get(1)>>24) & 0xff); }
    public byte pf()              { return (byte)((get(2)>> 0) & 0xff); }
    public byte pb()              { return (byte)((get(2)>> 8) & 0xff); }
    public byte pl()              { return (byte)((get(2)>>16) & 0xff); }
    public byte pr()              { return (byte)((get(2)>>24) & 0xff); }
    public byte ports()           { return (byte)((get(3)>> 0) & 0xff); }
    public byte adc()             { return (byte)((get(3)>> 8) & 0xff); }
    public boolean isFault()      { return (get(3) & 0x0100) != 0; }
    public boolean isZero()       { return (get(3) & 0x0200) != 0; }
    public boolean isOverflow()   { return (get(3) & 0x0400) != 0; }
    public boolean isCarry()      { return (get(3) & 0x0800) != 0; }
    public boolean isWait()       { return (get(3) & 0x1000) != 0; }
    public boolean isStep()       { return (get(3) & 0x2000) != 0; }
  }

  protected final ContainerSyncFields fields = new ContainerSyncFields();

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
  { return isValidBook(stack); }

  @Override
  public void clear()
  { onInventoryChanged(); inventory.clear(); markDirty(); }

  // ITickable ---------------------------------------------------------------------------------------------------------

  @Override
  public void tick()
  {
    if((world.isRemote()) || (--tickTimer > 0)) return;
    final BlockState blockState = world.getBlockState(pos); // @todo --> not sure if the caches getBlockState() would be good here.
    if(!(blockState.getBlock() instanceof MinecoprocessorBlock)) { tickTimer = 20; return; }
    final MinecoprocessorBlock block = (MinecoprocessorBlock)blockState.getBlock();
    tickTimer = ((block.config & MinecoprocessorBlock.CONFIG_OVERCLOCKED)!=0) ? 1 : 2;
    boolean dirty = false;
    boolean outputsChanged = false;
    boolean portConfigChanged = false;

    // "Firmware update"
    if(inventoryChanged) {
      // @todo: ---> moved book loading/unloading to here to have it in sync with the tick.
System.out.println("INVENTORY CHANGED");
      inventoryChanged = false;
      loadBook(inventory.get(0));
      prevPortsRegister = 0x0f;
      for(int i=0; i<prevPortValues.length; i++) prevPortValues[i] = 0;
      portConfigChanged = true;
      outputsChanged = true;
      dirty = true;
    } else if(!inventory.get(0).isEmpty()) {
      // Processor run
      if(processor.tick()) {
        outputsChanged = true;
      }
//System.out.println("CPU: " + processor.stateLineDump());
      // Port config update
      if(prevPortsRegister != processor.getRegisters()[Register.PORTS.ordinal()]) {
        prevPortsRegister = processor.getRegisters()[Register.PORTS.ordinal()];
        portConfigChanged = true;
        outputsChanged = false; // don't output if the port config has changed
      }
    }
    // Conditional data updates
    if(inputsChanged || portConfigChanged) {
System.out.println("(inputsChanged || portConfigChanged)");
      inputsChanged = false;
      updateInputPorts(blockState);
    }
    if(outputsChanged) {
//System.out.println("outputsChanged");
      updateOutputs(blockState, block);
    }
    BlockState state = blockState.with(MinecoprocessorBlock.ACTIVE, (!inventory.get(0).isEmpty()) && (!processor.isWait()) && (!processor.isFault()));
    if(state != blockState) {
System.out.println("(state != blockState)");
      world.setBlockState(pos, state, 2);
    }
    fields.writeServerSide(processor);
    if(dirty) markDirty();
  }

  // Class specific methods --------------------------------------------------------------------------------------------

  public static boolean isValidBook(ItemStack stack)
  { return (!stack.isEmpty()) && ((stack.getItem() == ModContent.CODE_BOOK) || (stack.getItem() == Items.WRITTEN_BOOK) || (stack.getItem() == Items.WRITABLE_BOOK)); }

  public Processor getProcessor()
  { return processor; }

  public void resetProcessor()
  { inventoryChanged = true; }

  public void setCustomName(ITextComponent name) // Invoked from block
  { this.customName = name.getString(); }

  public void neighborChanged(BlockPos fromPos) // Invoked from block
  { inputsChanged = true; } // Update redstone input state

  public int getPower(BlockState state, Direction side, boolean strong) //  Invoked from block
  {
    // @todo: Considering making a mapping here, where we have in here only a
    //        array with N,E,S,W -> p(N), p(E), p(S), p(W). Calculation of this
    //        cache values on port change?
    //        Does this make sense for you?
    // @review: getPortSignal() already clamps 0..15, so removed RedstoneUtil.portToPower()
    int p = (RedstoneUtil.isFrontPort(state, side)) ? getPortSignal(0) : (
            (RedstoneUtil.isBackPort(state, side))  ? getPortSignal(1) : (
            (RedstoneUtil.isLeftPort(state, side))  ? getPortSignal(2) : (
            (RedstoneUtil.isRightPort(state, side)) ? getPortSignal(3) : (
            0))));
System.out.println("getPower("+side+") = " + p);
    return p;
  }

  private void onInventoryChanged() // Invoked from inventory methods
  { inventoryChanged = true; tickTimer = 0; }

  private byte getPortSignal(int portIndex)
  {
    if(!isInOutputMode(processor.getRegisters()[Register.PORTS.ordinal()], portIndex)) {
      return 0;
    }
    byte signal = processor.getRegisters()[Register.PF.ordinal() + portIndex];
    return (byte)(isADCMode(processor.getRegisters()[Register.ADC.ordinal()], portIndex)
         ? MathHelper.clamp(signal, 0, 15)
         : ((signal == 0) ? (0) : (15)));
  }

  private static boolean isInInputMode(byte ports, int portIndex)
  { return (ByteUtil.getBit(ports, portIndex)) && (!ByteUtil.getBit(ports, portIndex + 4)); }

  private static boolean isInOutputMode(byte ports, int portIndex)
  { return (!ByteUtil.getBit(ports, portIndex)) && (!ByteUtil.getBit(ports, portIndex + 4)); }

  private static boolean isADCMode(byte adc, int portIndex)
  { return ByteUtil.getBit(adc, portIndex); }

  private static boolean isInResetMode(byte ports, int portIndex)
  { return ByteUtil.getBit(ports, portIndex) && ByteUtil.getBit(ports, portIndex + 4); }

  //
  // @review: Combined block methods and detectOutputChange() (were only a few lines in summary after porting).
  //
  private void updateOutputs(final BlockState state, final Block block)
  {
    byte[] registers = processor.getRegisters();
    byte ports = registers[Register.PORTS.ordinal()];
    for(int portIndex=0; portIndex<4; ++portIndex) {
      byte curVal = registers[Register.PF.ordinal() + portIndex];
      if(isInOutputMode(ports, portIndex) && (prevPortValues[portIndex] != curVal)) {
        prevPortValues[portIndex] = curVal;
        Direction side = RedstoneUtil.convertPortIndexToFacing(state.get(MinecoprocessorBlock.HORIZONTAL_FACING).getOpposite(), portIndex);
        BlockPos neighborPos = pos.offset(side);
System.out.println("port["+portIndex+"]=" + curVal + " -> " + side + " -> " + neighborPos);
        if(ForgeEventFactory.onNeighborNotify(world, pos, state, java.util.EnumSet.of(side), false).isCanceled()) continue;
        world.neighborChanged(neighborPos, block, pos);
        world.notifyNeighborsOfStateExcept(neighborPos, block, side.getOpposite());
      }
    }
  }

  private boolean updateInputPorts(BlockState state)
  {
    Direction facing = state.get(MinecoprocessorBlock.HORIZONTAL_FACING).getOpposite();
    int[] values = new int[4];
    values[RedstoneUtil.convertFacingToPortIndex(facing, Direction.NORTH)] = calculateInputStrength(pos.offset(Direction.NORTH), Direction.NORTH);
    values[RedstoneUtil.convertFacingToPortIndex(facing, Direction.SOUTH)] = calculateInputStrength(pos.offset(Direction.SOUTH), Direction.SOUTH);
    values[RedstoneUtil.convertFacingToPortIndex(facing, Direction.WEST)]  = calculateInputStrength(pos.offset(Direction.WEST), Direction.WEST);
    values[RedstoneUtil.convertFacingToPortIndex(facing, Direction.EAST)]  = calculateInputStrength(pos.offset(Direction.EAST), Direction.EAST);
    boolean updated = false;
    for(int i = 0; i < 4; i++) {
      updated = updateInputPort(i, values[i]) || updated;
    }
    if (updated) {
      processor.wake();
    }
    return updated;
  }

  /**
   * return true for positive edge changes
   */
  private boolean updateInputPort(int portIndex, int powerValue)
  {
    byte[] registers = processor.getRegisters();
    byte ports = registers[Register.PORTS.ordinal()];
    byte adc = registers[Register.ADC.ordinal()];
    byte value = (byte)((isADCMode(adc, portIndex)) ? RedstoneUtil.powerToPort(powerValue) : ((powerValue == 0) ? (0) : (0xff)));
    if(isInInputMode(ports, portIndex) && (prevPortValues[portIndex] != value)) {
      prevPortValues[portIndex] = value;
      registers[Register.PF.ordinal() + portIndex] = value;
      return true;
    } else if(isInResetMode(ports, portIndex) && (prevPortValues[portIndex] != value)) {
      prevPortValues[portIndex] = value;
      if(value != 0) {
        processor.reset();
        return true;
      }
    }
    return false;
  }

  /**
   * Loads or unloads a book. Returns true if a book was loaded, false otherwise.
   */
  private boolean loadBook(ItemStack stack)
  {
    processor.reset();
    if(stack.isEmpty()) {
      processor.load(null);
      customName = "";
      return false;
    } else {
      if(!stack.hasTag()) return false;
      List<String> code = new ArrayList<>();
      if(stack.getItem() instanceof CodeBookItem) {
        code = CodeBookItem.Data.loadFromStack(stack).getContinuousProgram();
      } else if (stack.getItem() instanceof WritableBookItem) {
        ListNBT pages = stack.getTag().getList("pages", 8);
        for(int i = 0; i < pages.size(); ++i) {
          Collections.addAll(code, pages.getString(i).split("\\r?\\n"));
        }
      } else if (stack.getItem() instanceof WrittenBookItem) {
        // @todo: check if this is really the same
        ListNBT pages = stack.getTag().getList("pages", 8);
        for(int i = 0; i < pages.size(); ++i) {
          Collections.addAll(code, pages.getString(i).split("\\r?\\n"));
        }
      }
      customName = readNameFromHeader(code);
      return processor.load(code);
    }
  }

  private static String readNameFromHeader(List<String> code)
  {
    try {
      List<String> nameSearch = InstructionUtil.regex("^\\s*;\\s*(.*)", code.get(0), Pattern.CASE_INSENSITIVE);
      if (nameSearch.size() != 1) {
        return "";
      }
      String name = nameSearch.get(0);
      if((name != null) && (!name.isEmpty())) {
        return name;
      }
    } catch (Exception e) {
      ModMinecoprocessors.proxy.handleUnexpectedException(e);
    }
    return "";
  }

  private int calculateInputStrength(BlockPos pos, Direction enumfacing)
  {
    BlockState adjacentState = world.getBlockState(pos);
    Block block = adjacentState.getBlock();
    int p = world.getRedstonePower(pos, enumfacing);
    if (p >= 15) return 15;
    return Math.max(p, ((block == Blocks.REDSTONE_WIRE) ? adjacentState.get(RedstoneWireBlock.POWER) : 0));
  }

}


// ---------------------------------------------------------------------------------------------------------------------
// PORTING: LEFT CODE FROM 1.12 TILE ENTITY
// ---------------------------------------------------------------------------------------------------------------------

//  private Set<ServerPlayerEntity> playersToUpdate = new HashSet<ServerPlayerEntity>();
//
//  public void updatePlayers() {
//    for (EntityPlayerMP player : playersToUpdate) {
//      Minecoprocessors.NETWORK.sendTo(new MessageProcessorUpdate(processor.writeToNBT(), pos, getName()), player);
//    }
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


// ---------------------------------------------------------------------------------------------------------------------
// FUNCTIONALITY THAT IS PROPOSED TO BE MOVED FROM THE BLOCK TO HERE
// ---------------------------------------------------------------------------------------------------------------------


//
//  @Override
//  public void updateTick(World world, BlockPos pos, BlockState state, Random rand) {
//    super.updateTick(world, pos, state, rand);
//    updateInputPorts(world, pos, state);
//
//    if (world.isRemote) {
//      return;
//    }
//
//    TileEntityMinecoprocessor te = (TileEntityMinecoprocessor) world.getTileEntity(pos);
//
//    boolean changed = false;
//
//    boolean blockActive = state.getValue(ACTIVE);
//    boolean processorActive = !te.getProcessor().isWait() && !te.getProcessor().isFault();
//
//    if (blockActive && !processorActive) {
//      state = state.withProperty(ACTIVE, Boolean.valueOf(false));
//      changed = true;
//    } else if (!blockActive && processorActive) {
//      state = state.withProperty(ACTIVE, Boolean.valueOf(true));
//      changed = true;
//    }
//
//    if (changed) {
//      world.setBlockState(pos, state, 2);
//    }
//  }
//
