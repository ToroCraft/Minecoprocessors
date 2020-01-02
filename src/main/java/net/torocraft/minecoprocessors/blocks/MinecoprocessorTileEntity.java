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
  private boolean loaded = false;
  private byte prevPortsRegister = 0x0f;
  private boolean prevIsInactive;
  private boolean inventoryChanged = false;
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
      customName = nbt.getString("CustomName");
    }
  }

  @Override
  public CompoundNBT write(CompoundNBT nbt)
  {
    super.write(nbt);
    nbt.put("processor", processor.getNBT());
    nbt.putShort("loadTime", (short)loadTime);
    ItemStackHelper.saveAllItems(nbt, inventory);
    if(hasCustomName()) nbt.putString("CustomName", customName);
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
  { return isValidBook(stack); }

  @Override
  public void clear()
  { onInventoryChanged(); inventory.clear(); markDirty(); }

  // ITickable ---------------------------------------------------------------------------------------------------------

  @Override
  public void tick()
  {
    if((world.isRemote) || (--tickTimer > 0)) return;
    tickTimer = 2;
    final BlockState currentBlockState = world.getBlockState(pos); // @todo --> not sure if the caches getBlockState() would be good here.
    if(!(currentBlockState.getBlock() instanceof MinecoprocessorBlock)) return;
    final MinecoprocessorBlock block = (MinecoprocessorBlock)currentBlockState.getBlock();
    if((block.config & MinecoprocessorBlock.CONFIG_OVERCLOCKED)!=0) tickTimer = 1;
    boolean dirty = false;

    // @todo: ---> moved book loading/unloading to here to have it in sync with the tick.
    if(inventoryChanged) {
      inventoryChanged = false;
      if(inventory.get(0).isEmpty()) {
        unloadBook();
      } else {
        loadBook(inventory.get(0));
      }
      dirty = true;
    }

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

    if(dirty) markDirty();
  }

  // Class specific methods --------------------------------------------------------------------------------------------

  public static boolean isValidBook(ItemStack stack)
  { return (!stack.isEmpty()) && ((stack.getItem() == ModContent.CODE_BOOK) || (stack.getItem() == Items.WRITTEN_BOOK) || (stack.getItem() == Items.WRITABLE_BOOK)); }

  public void setCustomName(ITextComponent name) // Invoked from block
  { this.customName = name.getString(); }

  public void neighborChanged(BlockPos fromPos) // Invoked from block
  {} // Update redstone input state

  public int getPower(BlockState state, Direction side, boolean strong) //  Invoked from block
  {
    // @todo: Considering making a mapping here, where we have in here only a
    //        array with N,E,S,W -> p(N), p(E), p(S), p(W). Calculation of this
    //        cache values on port change?
    //        Does this make sense for you?
    if(RedstoneUtil.isFrontPort(state, side)) {
      return RedstoneUtil.portToPower(getPortSignal(0));
    }
    if(RedstoneUtil.isBackPort(state, side)) {
      return RedstoneUtil.portToPower(getPortSignal(1));
    }
    if(RedstoneUtil.isLeftPort(state, side)) {
      return RedstoneUtil.portToPower(getPortSignal(2));
    }
    if(RedstoneUtil.isRightPort(state, side)) {
      return RedstoneUtil.portToPower(getPortSignal(3));
    }
    return 0;
  }

  private void onInventoryChanged() // Invoked from inventory methods
  { inventoryChanged = true; tickTimer = 0; }

  private byte getPortSignal(int portIndex)
  {
    if(!isInOutputMode(processor.getRegisters()[Register.PORTS.ordinal()], portIndex)) {
      return 0;
    }
    byte signal = processor.getRegisters()[Register.PF.ordinal() + portIndex];
    return (byte)(isADCMode(processor.getRegisters()[Register.ADC.ordinal()], portIndex) ? (signal)
         : ((signal == 0) ? (0) : (signal & 0xff)));
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
  private void detectOutputChanges(BlockState state, Block block)
  {
    byte[] registers = processor.getRegisters();
    byte ports = registers[Register.PORTS.ordinal()];
    for(int portIndex=0; portIndex<4; ++portIndex) {
      byte curVal = registers[Register.PF.ordinal() + portIndex];
      if(isInOutputMode(ports, portIndex) && (prevPortValues[portIndex] != curVal)) {
        prevPortValues[portIndex] = curVal;
        Direction side = RedstoneUtil.convertPortIndexToFacing(getBlockState().get(MinecoprocessorBlock.HORIZONTAL_FACING).getOpposite(), portIndex);
        BlockPos neighborPos = pos.offset(side);
        if(ForgeEventFactory.onNeighborNotify(world, pos, state, java.util.EnumSet.of(side), false).isCanceled()) return;
        world.neighborChanged(neighborPos, block, pos);
        if(world.getTileEntity(neighborPos)==null) {
          // @todo: review: TEs will likely update neigbours themselves. Only strong power affects
          //        adjecent blocks of the neighbour.
          world.notifyNeighborsOfStateExcept(neighborPos, block, side.getOpposite());
        }
      }
    }
  }

  private boolean updateInputPorts(int[] values)
  {
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

  // @todo: check where this method is used.
  private void reset()
  {
    if(world.isRemote) return;
    processor.reset();
    detectOutputChanges(getBlockState(), getBlockState().getBlock());
    loaded = false;
  }

  private void unloadBook()
  {
    processor.load(null);
    loaded = false;
    customName = "";
    // updatePlayers();
  }

  private void loadBook(ItemStack stack)
  {
    if(!stack.hasTag()) return;
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
    loaded = processor.load(code);
    // updatePlayers();
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
//  protected void updateState(World worldIn, BlockPos pos, BlockState state) {
//    worldIn.updateBlockTick(pos, this, 0, -1);
//  }



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
//  public static void updateInputPorts(World world, BlockPos pos, BlockState state) {
//    if (world.isRemote) {
//      return;
//    }
//    EnumFacing facing = state.getValue(FACING).getOpposite();
//
//    int e = calculateInputStrength(world, pos.offset(EnumFacing.EAST), EnumFacing.EAST);
//    int w = calculateInputStrength(world, pos.offset(EnumFacing.WEST), EnumFacing.WEST);
//    int n = calculateInputStrength(world, pos.offset(EnumFacing.NORTH), EnumFacing.NORTH);
//    int s = calculateInputStrength(world, pos.offset(EnumFacing.SOUTH), EnumFacing.SOUTH);
//
//    int[] values = new int[4];
//
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.NORTH)] = n;
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.SOUTH)] = s;
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.WEST)] = w;
//    values[RedstoneUtil.convertFacingToPortIndex(facing, EnumFacing.EAST)] = e;
//
//    ((TileEntityMinecoprocessor) world.getTileEntity(pos)).updateInputPorts(values);
//  }
//
//  protected static int calculateInputStrength(World worldIn, BlockPos pos, EnumFacing enumfacing) {
//    BlockState adjacentState = worldIn.getBlockState(pos);
//    Block block = adjacentState.getBlock();
//
//    int i = worldIn.getRedstonePower(pos, enumfacing);
//
//    if (i >= 15) {
//      return 15;
//    }
//
//    int redstoneWirePower = 0;
//
//    if (block == Blocks.REDSTONE_WIRE) {
//      redstoneWirePower = adjacentState.getValue(BlockRedstoneWire.POWER);
//    }
//
//    return Math.max(i, redstoneWirePower);
//
//  }

