package net.torocraft.minecoprocessors.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.processor.IProcessor;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.ByteUtil;

@SuppressWarnings("unused")
public class TileEntityMinecoprocessor extends TileEntity implements ITickable, IInventory {

	private static final String NAME = "minecoprocessor_tile_entity";
	private static final String NBT_PROCESSOR = "processor";
	private static final String NBT_LOAD_TIME = "loadTime";
	private static final String NBT_CUSTOM_NAME = "CustomName";

	private final IProcessor processor = new Processor();
	private NonNullList<ItemStack> codeItemStacks = NonNullList.<ItemStack> withSize(1, ItemStack.EMPTY);
	private String customName;
	private int loadTime;

	private final boolean[] prevPortValues = new boolean[4];

	public static void init() {
		GameRegistry.registerTileEntity(TileEntityMinecoprocessor.class, NAME);
	}

	public TileEntityMinecoprocessor() {
		loadSampleProgramOutput();
	}

	private void loadSampleProgramInput() {
		String program = "";
		program += "; ouput: none      \n";
		program += "	mov ports, 0   \n";
		program += "	 			   \n";
		program += "start: 			   \n";
		program += "	jmp start 	   \n";
		program += "	 			   \n";
		processor.load(program);
	}

	private void loadSampleProgramOutput() {
		String program = "";
		program += "; ouput: all        \n";
		program += "	mov ports, 15   \n";
		program += "	 			   \n";
		program += "start: 			   \n";
		program += "	mov pf, 1	   \n";
		program += "	mov pr, 1	   \n";
		program += "	mov pb, 1	   \n";
		program += "	mov pl, 1	   \n";
		program += "	mov pf, 0	   \n";
		program += "	mov pr, 0	   \n";
		program += "	mov pb, 0	   \n";
		program += "	mov pl, 0	   \n";
		program += "	jmp start 	   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		processor.load(program);
	}

	private void loadSampleProgram1() {
		String program = "";
		program += "; ouput: pf pr     \n";
		program += "	mov ports, 9   \n";
		program += "	 			   \n";
		program += "start: 			   \n";
		program += "	mov c, 10 	   \n";
		program += "	 			   \n";
		program += "label: 			   \n";
		program += "	mov pf, 0	   \n";
		program += "	mov pr, 1	   \n";
		program += "	sub c, 1 	   \n";
		program += "	mov pf, 1	   \n";
		program += "	mov pr, 0	   \n";
		program += "	jnz label 	   \n";
		program += "			 	   \n";
		program += "	mov n, 1 	   \n";
		program += "	jmp start 	   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		program += "	 			   \n";
		processor.load(program);
	}

	@Override
	public void readFromNBT(NBTTagCompound c) {
		super.readFromNBT(c);
		processor.readFromNBT(c.getCompoundTag(NBT_PROCESSOR));

		codeItemStacks = NonNullList.<ItemStack> withSize(this.getSizeInventory(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(c, codeItemStacks);

		loadTime = c.getShort(NBT_LOAD_TIME);

		if (c.hasKey(NBT_CUSTOM_NAME, 8)) {
			this.customName = c.getString("CustomName");
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound cIn) {
		NBTTagCompound c = super.writeToNBT(cIn);
		c.setTag(NBT_PROCESSOR, processor.writeToNBT());

		c.setShort(NBT_LOAD_TIME, (short) loadTime);
		ItemStackHelper.saveAllItems(c, codeItemStacks);

		if (this.hasCustomName()) {
			c.setString(NBT_CUSTOM_NAME, this.customName);
		}

		return c;
	}

	@Override
	public void update() {
		if (world.isRemote || world.getTotalWorldTime() % 10 != 0) {
			return;
		}
		processor.tick();
		detectOutputChanges();
	}

	private boolean detectOutputChanges() {
		boolean updated = false;
		updated = updated || detectOutputChange(0);
		updated = updated || detectOutputChange(1);
		updated = updated || detectOutputChange(2);
		updated = updated || detectOutputChange(3);

		return updated;
	}

	private boolean detectOutputChange(int portIndex) {
		byte[] registers = processor.getRegisters();
		byte ports = registers[Register.PORTS.ordinal()];

		boolean curVal = ByteUtil.getBit(registers[Register.PF.ordinal() + portIndex], 0);

		if (ByteUtil.getBit(ports, portIndex) && prevPortValues[portIndex] != curVal) {
			prevPortValues[portIndex] = curVal;
			BlockMinecoprocessor.INSTANCE.onPortChange(world, pos, world.getBlockState(pos), portIndex);
			return true;
		}
		return false;
	}

	public boolean updateInputPorts(boolean[] values) {
		boolean updated = false;
		for (int i = 0; i < 4; i++) {
			updated = updated || updateInputPort(i, values[i]);
		}
		if (updated) {
			// TODO edge trigger support
			// TODO interrupt processor
		}
		return updated;
	}

	private boolean updateInputPort(int portIndex, boolean value) {
		byte[] registers = processor.getRegisters();
		byte ports = registers[Register.PORTS.ordinal()];
		if (!ByteUtil.getBit(ports, portIndex) && prevPortValues[portIndex] != value) {
			prevPortValues[portIndex] = value;
			registers[Register.PF.ordinal() + portIndex] = value ? (byte) 1 : 0;
			return true;
		}
		return false;
	}

	private boolean getPortSignal(int portIndex) {
		boolean outputMode = ByteUtil.getBit(processor.getRegisters()[Register.PORTS.ordinal()], portIndex);
		if (!outputMode) {
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
		if (world.isRemote) {
			return;
		}
		System.out.println("reset");
		processor.reset();
		for (int portIndex = 0; portIndex < 4; portIndex++) {
			detectOutputChange(portIndex);
		}
	}
	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */

	// private static final int[] SLOTS_FOR_UP = new int[] { 3 };
	// private static final int[] SLOTS_FOR_DOWN = new int[] { 0, 1, 2, 3 };
	/// ** an array of the output slot indices */
	// private static final int[] OUTPUT_SLOTS = new int[] { 0, 1, 2, 4 };

	/**
	 * an integer with each bit specifying whether that slot of the stand
	 * contains a potion
	 */
	// private boolean[] filledSlots;

	/**
	 * Returns the stack in the given slot.
	 */
	@Override
	public ItemStack getStackInSlot(int index) {
		return index >= 0 && index < codeItemStacks.size() ? (ItemStack) codeItemStacks.get(index) : ItemStack.EMPTY;
	}

	/**
	 * Removes up to a specified number of items from an inventory slot and
	 * returns them in a new stack.
	 */
	@Override
	public ItemStack decrStackSize(int index, int count) {
		markDirty();
		return ItemStackHelper.getAndSplit(codeItemStacks, index, count);
	}

	/**
	 * Removes a stack from the given slot and returns it.
	 */
	@Override
	public ItemStack removeStackFromSlot(int index) {
		markDirty();
		return ItemStackHelper.getAndRemove(codeItemStacks, index);
	}

	/**
	 * Sets the given item stack to the specified slot in the inventory (can be
	 * crafting or armor sections).
	 */
	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		if (index >= 0 && index < this.codeItemStacks.size()) {
			codeItemStacks.set(index, stack);

			// TODO setup with load time delay
			loadBook(stack);

			markDirty();
		}
	}

	private void loadBook(ItemStack stack) {

		if (world.isRemote) {
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
			String page = pages.getStringTagAt(i);
			System.out.println("-----------------------------------");
			System.out.println(page);
			System.out.println("-----------------------------------");
			code.append(page);
		}
		
		processor.load(code.toString());
	}

	/**
	 * Returns the maximum stack size for a inventory slot. Seems to always be
	 * 64, possibly will be extended.
	 */
	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	/**
	 * Don't rename this method to canInteractWith due to conflicts with
	 * Container
	 */
	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return world.getTileEntity(pos) != this ? false
				: player.getDistanceSq((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	public static boolean isBook(Item item) {
		return item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK;
	}

	/**
	 * Returns true if automation is allowed to insert the given stack (ignoring
	 * stack size) into the given slot. For guis use Slot.isItemValid
	 */
	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return isBook(stack.getItem());
	}

	@Override
	public int getField(int id) {
		switch (id) {
		case 0:
			return loadTime;
		default:
			return 0;
		}
	}

	@Override
	public void setField(int id, int value) {
		switch (id) {
		case 0:
			loadTime = value;
			break;
		}
	}

	/**
	 * Get the name of this object. For players this returns their username
	 */
	@Override
	public String getName() {
		return hasCustomName() ? this.customName : "container.minecoprocessor";
	}

	public void setName(String name) {
		this.customName = name;
	}

	/**
	 * Returns true if this thing is named
	 */
	@Override
	public boolean hasCustomName() {
		return customName != null && !customName.isEmpty();
	}

	@Override
	public ITextComponent getDisplayName() {
		return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
	}

	/**
	 * Returns the number of slots in the inventory.
	 */
	@Override
	public int getSizeInventory() {
		return codeItemStacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : codeItemStacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int getFieldCount() {
		return 2;
	}

	@Override
	public void clear() {
		codeItemStacks.clear();
		markDirty();
	}

}
