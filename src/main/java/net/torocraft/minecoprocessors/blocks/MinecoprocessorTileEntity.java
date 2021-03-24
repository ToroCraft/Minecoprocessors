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
  private final int[] outputPower = new int[Direction.values().length];
  private byte prevPortsRegister = -1;
  private byte prevAdcRegister = -1;
  private boolean inventoryChanged = false;
  private boolean inputsChanged = false;
  private boolean outputsChanged = false;
  private boolean initialized = false;
  private String customName = "";
  private int loadTime;
  private int tickTimer = 0;

  public MinecoprocessorTileEntity()
  { super(ModContent.TET_MINECOPROCESSOR); }

  public MinecoprocessorTileEntity(TileEntityType<?> te_type)
  { super(te_type); }

  // TileEntity --------------------------------------------------------------------------------------------------------

  @Override
  public void read(BlockState state, CompoundNBT nbt)
  {
    super.read(state, nbt);
    processor.setNBT(nbt.getCompound("processor"));
    inventory = NonNullList.<ItemStack>withSize(1, ItemStack.EMPTY);  // <<-- this.getSizeInventory() -> 1
    ItemStackHelper.loadAllItems(nbt, inventory);
    loadTime = nbt.getShort("loadTime");
    customName = nbt.getString("CustomName");
    if(customName==null) customName = "";
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
  { return !customName.isEmpty(); }

  @Override
  public ITextComponent getCustomName()
  { return new StringTextComponent(customName); }

  @Override
  public ITextComponent getDisplayName()
  { return INameable.super.getDisplayName(); }

  // INamedContainerProvider ------------------------------------------------------------------------------

  @Override
  public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player )
  { return new MinecoprocessorContainer(id, inventory, this, IWorldPosCallable.of(world, pos), fields, processor); }

  // Container/GUI synchronization fields
  public static final class ContainerSyncFields extends IntArray
  {
    // Note: seems like only s16 values are transferred over network, so stripped field value range from int to short.
    public static int NUM_OF_FIELDS = 8;

    public ContainerSyncFields()
    { super(NUM_OF_FIELDS); }

    public void writeServerSide(Processor processor)
    {
      final byte[] regs = processor.getRegisters();
      set(0, (int)0|processor.getIp()); // IP
      set(1, (((int)0|processor.getSp()) & 0xff) | ((((int)0|processor.getFaultCode()) & 0xff)<<8)); // SP FAULT
      set(2, (((int)0|regs[0]) & 0xff) | ((((int)0|regs[1])<<8))); // A B
      set(3, (((int)0|regs[2]) & 0xff) | ((((int)0|regs[3])<<8))); // C D
      set(4, (((int)0|regs[4]) & 0xff) | ((((int)0|regs[5])<<8))); // PF PB
      set(5, (((int)0|regs[6]) & 0xff) | ((((int)0|regs[7])<<8))); // PL PR
      set(6, (((int)0|regs[8]) & 0xff) | ((((int)0|regs[9])<<8))); // PORTS ADC
      set(7, ((int)0 // FLAGS
        | (processor.isFault()    ? 0x0001 : 0)
        | (processor.isZero()     ? 0x0002 : 0)
        | (processor.isOverflow() ? 0x0004 : 0)
        | (processor.isCarry()    ? 0x0008 : 0)
        | (processor.isWait()     ? 0x0010 : 0)
        | (processor.isStep()     ? 0x0020 : 0)
        | (processor.hasProgram() ? 0x0040 : 0)
      ));
    }

    // Client side getters
    public short ip()             { return (short)(get(0)); }
    public byte sp()              { return (byte)((get(0)) & 0xff); }
    public byte fault()           { return (byte)((get(0)>>8) & 0xff); }
    public byte register(int i)   { return (byte)((get(2+(i>>1)) >> (8*(i&0x1))) & 0xff); }
    public byte port(int i)       { return register(i+4); }
    public byte ports()           { return register(8); }
    public byte adc()             { return register(9); }
    public boolean isFault()      { return (get(7) & 0x0001) != 0; }
    public boolean isZero()       { return (get(7) & 0x0002) != 0; }
    public boolean isOverflow()   { return (get(7) & 0x0004) != 0; }
    public boolean isCarry()      { return (get(7) & 0x0008) != 0; }
    public boolean isWait()       { return (get(7) & 0x0010) != 0; }
    public boolean isStep()       { return (get(7) & 0x0020) != 0; }
    public boolean isLoaded()     { return (get(7) & 0x0040) != 0; }
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
    final BlockState state = world.getBlockState(pos);
    if(!(state.getBlock() instanceof MinecoprocessorBlock)) { tickTimer = 20; return; }
    final MinecoprocessorBlock block = (MinecoprocessorBlock)state.getBlock();
    tickTimer = ((block.config & MinecoprocessorBlock.CONFIG_OVERCLOCKED)!=0) ? 1 : 2;
    boolean dirty = false;
    final boolean hasBook = !inventory.get(0).isEmpty();
    // TE loading initialisation
    if(!initialized) {
      // Uncool that this has to be done in tick(), but TE::onLoad() is no help here.
      initialized = true;
      // This is needed because otherwise the processor will wakeup from sleep after
      // loading, because an input change is detected.
      updateInputPorts(state, true);
    }
    // "Firmware update"
    if(inventoryChanged) {
      inventoryChanged = false;
      customName = loadBook(inventory.get(0), processor);
      prevPortsRegister = processor.getRegister(Register.PORTS);
      prevAdcRegister = processor.getRegister(Register.ADC);
      for(int i=0; i<prevPortValues.length; i++) prevPortValues[i] = 0;
      for(int i=0; i<outputPower.length; i++) outputPower[i] = 0;
      inputsChanged = true;
      outputsChanged = true;
      dirty = true;
    } else if(hasBook && (!processor.isFault())) {
      // Input
      if(inputsChanged) {
        inputsChanged = false;
        updateInputPorts(state, false);
      }
      // Processor run
      if(processor.tick()) {
        outputsChanged = true;
      }
      // Port config update
      if((prevPortsRegister != processor.getRegister(Register.PORTS)) ||
         (prevAdcRegister != processor.getRegister(Register.ADC))) {
        prevPortsRegister = processor.getRegister(Register.PORTS);
        prevAdcRegister = processor.getRegister(Register.ADC);
        for(int i=0; i<prevPortValues.length; i++) prevPortValues[i] = 0;
        for(int i=0; i<outputPower.length; i++) outputPower[i] = 0;
        inputsChanged = true;
        outputsChanged = true;
      }
    }
    if(outputsChanged) {
      outputsChanged = false;
      updateOutputs(state, block);
    }
    BlockState new_state = state.with(MinecoprocessorBlock.POWERED, hasBook && (!processor.isWait()) && (!processor.isFault()));
    if(state != new_state) {
      world.setBlockState(pos, new_state, 1|2);
    }
    fields.writeServerSide(processor);
    if(dirty) markDirty();
  }

  // Class specific methods --------------------------------------------------------------------------------------------

  public static boolean isValidBook(ItemStack stack)
  { return (!stack.isEmpty()) && ((stack.getItem() == ModContent.CODE_BOOK) || (stack.getItem() == Items.WRITTEN_BOOK) || (stack.getItem() == Items.WRITABLE_BOOK)); }

  public static boolean isInInputMode(byte ports, int portIndex)
  { return (ByteUtil.getBit(ports, portIndex)) && (!ByteUtil.getBit(ports, portIndex + 4)); }

  public static boolean isInOutputMode(byte ports, int portIndex)
  { return (!ByteUtil.getBit(ports, portIndex)) && (!ByteUtil.getBit(ports, portIndex + 4)); }

  public static boolean isADCMode(byte adc, int portIndex)
  { return ByteUtil.getBit(adc, portIndex); }

  public static boolean isInResetMode(byte ports, int portIndex)
  { return ByteUtil.getBit(ports, portIndex) && ByteUtil.getBit(ports, portIndex + 4); }

  public Processor getProcessor()
  { return processor; }

  public void resetProcessor()
  { inventoryChanged = true; }

  public void setCustomName(ITextComponent name) // Invoked from block
  { this.customName = name.getString(); }

  public void neighborChanged(BlockPos fromPos) // Invoked from block
  { inputsChanged = true; } // Update redstone input state

  public int getPower(BlockState state, Direction side, boolean strong) //  Invoked from block
  { return outputPower[side.getOpposite().getIndex()]; } // The updated side of the adjacent block is the opposite side here.

  private void onInventoryChanged() // Invoked from inventory methods
  { inventoryChanged = true; tickTimer = 0; }

  private byte getPortSignal(int portIndex)
  {
    if(!isInOutputMode(processor.getRegister(Register.PORTS), portIndex)) {
      return 0;
    }
    byte signal = processor.getRegisters()[Register.PF.ordinal() + portIndex];
    return (byte)(isADCMode(processor.getRegister(Register.ADC), portIndex)
         ? MathHelper.clamp(signal, 0, 15)
         : ((signal == 0) ? (0) : (15)));
  }

  private boolean updateOutputs(final BlockState state, final Block block)
  {
    final Direction front = state.get(MinecoprocessorBlock.HORIZONTAL_FACING);
    final byte[] registers = processor.getRegisters();
    final byte ports = processor.getRegister(Register.PORTS);
    final boolean[] to_update = new boolean[Direction.values().length];
    boolean updated = false;
    // Side power value updates
    for(int portIndex=0; portIndex<4; ++portIndex) {
      byte value = registers[Register.PF.ordinal() + portIndex];
      if(prevPortValues[portIndex] == value) continue;
      prevPortValues[portIndex] = value;
      if(!isInOutputMode(ports, portIndex)) continue;
      Direction side = RedstoneUtil.convertPortIndexToFacing(front, portIndex);
      outputPower[side.getIndex()] = getPortSignal(portIndex);
      to_update[side.getIndex()] = true;
      updated = true;
      // testlog("out["+portIndex+"]=" + (((int)value)&0xff) + "=" + outputPower[side.getIndex()] + " -> " + side + " -> " + getPos().offset(side));
    }
    // Neighbour block updates
    for(Direction side: Direction.values()) {
      if(!to_update[side.getIndex()]) continue;
      to_update[side.getIndex()] = false;
      BlockPos neighborPos = pos.offset(side);
      if(net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(world, pos, state, java.util.EnumSet.of(side), false).isCanceled()) continue;
      world.neighborChanged(neighborPos, block, pos);
      world.notifyNeighborsOfStateExcept(neighborPos, block, side.getOpposite());
    }
    return updated;
  }

  private boolean updateInputPorts(BlockState state, boolean initialLoading)
  {
    //
    // @review: included updateInputPort() here to have for reusing local vars on the stack.
    //
    final Direction front = state.get(MinecoprocessorBlock.HORIZONTAL_FACING);
    final byte[] registers = processor.getRegisters();
    final byte ports = processor.getRegister(Register.PORTS);
    final byte adc = processor.getRegister(Register.ADC);
    boolean updated = false;
    boolean reset = false;
    for(int portIndex=0; portIndex<4; ++portIndex) {
      Direction side = RedstoneUtil.convertPortIndexToFacing(front, portIndex);
      int power = getInputPower(pos.offset(side), side);
      byte value = (byte)((isADCMode(adc, portIndex)) ? RedstoneUtil.powerToPort(power) : ((power == 0) ? (0) : (0xff)));
      if(prevPortValues[portIndex] == value) continue; // not changed
      prevPortValues[portIndex] = value;
      if(isInInputMode(ports, portIndex)) {
        registers[Register.PF.ordinal() + portIndex] = value;
        updated = true;
      } else if((value != 0) && isInResetMode(ports, portIndex)) {
        updated = true;
        reset = true;
      }
    }
    if(!initialLoading) {
      if(reset) processor.reset();
      if(updated) processor.wake();
    }
    return updated;
  }

  private int getInputPower(BlockPos pos, Direction side)
  {
    BlockState adjacent = world.getBlockState(pos);
    Block block = adjacent.getBlock();
    int p = world.getRedstonePower(pos, side);
    if (p >= 15) return 15;
    return Math.max(p, ((block instanceof RedstoneWireBlock) ? adjacent.get(RedstoneWireBlock.POWER) : 0));
  }

  public static String loadBook(ItemStack stack, Processor processor)
  {
    processor.reset();
    if(!isValidBook(stack)) {
      processor.load(null);
      return "";
    } else {
      String title = "";
      if(!stack.hasTag()) return title;
      List<String> code = new ArrayList<>();
      if(stack.getItem() instanceof CodeBookItem) {
        CodeBookItem.Data data = CodeBookItem.Data.loadFromStack(stack);
        code = data.getContinuousProgram();
        title = data.getProgramName();
      } else if (stack.getItem() instanceof WritableBookItem) {
        ListNBT pages = stack.getTag().getList("pages", 8);
        for(int i = 0; i < pages.size(); ++i) {
          Collections.addAll(code, pages.getString(i).split("\\r?\\n"));
        }
      } else if (stack.getItem() instanceof WrittenBookItem) {
        ListNBT pages = stack.getTag().getList("pages", 8);
        for(int i = 0; i < pages.size(); ++i) {
          try {
            ITextComponent tc = ITextComponent.Serializer.getComponentFromJsonLenient(pages.getString(i));
            Collections.addAll(code, tc.getUnformattedComponentText().split("\\r?\\n"));
          } catch(Exception e) {
            // don't load, will result in a processor fault flag displayed.
          }
        }
      }
      if(title.isEmpty()) {
        // Read TE display name from Book header comment
        try {
          List<String> nameSearch = InstructionUtil.regex("^\\s*;\\s*(.*)", code.get(0), Pattern.CASE_INSENSITIVE);
          if(nameSearch.size() == 1) {
            String name = nameSearch.get(0);
            if((name != null) && (!name.isEmpty())) {
              title = name;
            }
          }
        } catch (Exception e) {
          ModMinecoprocessors.proxy.handleUnexpectedException(e);
        }
      }
      processor.load(code);
      return title;
    }
  }

}
