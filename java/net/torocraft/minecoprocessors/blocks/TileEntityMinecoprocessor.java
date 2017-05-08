package net.torocraft.minecoprocessors.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.torocraft.minecoprocessors.processor.IProcessor;
import net.torocraft.minecoprocessors.processor.Processor;
import net.torocraft.minecoprocessors.processor.Register;
import net.torocraft.minecoprocessors.util.ByteUtil;

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

	@Override
	public void readFromNBT(NBTTagCompound c) {
		super.readFromNBT(c);
		processor.readFromNBT(c.getCompoundTag(NBT_PROCESSOR));

		codeItemStacks = NonNullList.<ItemStack> withSize(this.getSizeInventory(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(c, codeItemStacks);

		loadTime = c.getShort(NBT_LOAD_TIME);

		if (c.hasKey(NBT_CUSTOM_NAME, 8)) {
			this.customName = c.getString(NBT_CUSTOM_NAME);
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
		if (world.isRemote) {
			return;
		}
		processor.tick();
		detectOutputChanges();
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

	@Override
	public ItemStack getStackInSlot(int index) {
		return index >= 0 && index < codeItemStacks.size() ? (ItemStack) codeItemStacks.get(index) : ItemStack.EMPTY;
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
			code.append(pages.getStringTagAt(i));
		}
		processor.load(code.toString());
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

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
	public void clear() {
		codeItemStacks.clear();
		markDirty();
	}

}
