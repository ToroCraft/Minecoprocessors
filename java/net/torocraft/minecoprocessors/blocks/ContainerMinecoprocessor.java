package net.torocraft.minecoprocessors.blocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMinecoprocessor extends Container {

  private final IInventory te;
  @SuppressWarnings("unused")
  private final Slot codeBookSlot;

  public ContainerMinecoprocessor(IInventory playerInventory, IInventory te) {
    this.te = te;

    this.codeBookSlot = this.addSlotToContainer(new CodeBookSlot(te, 0, 80, 35));

    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 9; ++j) {
        this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
      }
    }

    for (int k = 0; k < 9; ++k) {
      this.addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
    }

  }

  static class CodeBookSlot extends Slot {

    public CodeBookSlot(IInventory iInventoryIn, int index, int xPosition, int yPosition) {
      super(iInventoryIn, index, xPosition, yPosition);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
      return TileEntityMinecoprocessor.isBook(stack.getItem());
    }

    @Override
    public int getSlotStackLimit() {
      return 1;
    }
  }
  
  @Override
  public void detectAndSendChanges() {
    
  }

  @Override
  public boolean canInteractWith(EntityPlayer player) {
    return te.isUseableByPlayer(player);
  }

  @Override
  public ItemStack transferStackInSlot(EntityPlayer playerIn, int fromSlot) {
    ItemStack previous = null;
    Slot slot = (Slot) this.inventorySlots.get(fromSlot);

    if (slot != null && slot.getHasStack()) {
      ItemStack current = slot.getStack();
      previous = current.copy();

      if (fromSlot == 0) {
        if (!this.mergeItemStack(current, 1, 37, true)) {
          return null;
        }
      } else {
        if (!this.mergeItemStack(current, 0, 1, true)) {
          return null;
        }
      }

      slot.onSlotChange(current, previous);

      if (current == null) {
        slot.putStack(null);
      } else {
        slot.onSlotChanged();
      }

      if (current.stackSize == previous.stackSize) {
        return null;
      }

      slot.onPickupFromSlot(playerIn, current);
    }

    return previous;
  }

}
