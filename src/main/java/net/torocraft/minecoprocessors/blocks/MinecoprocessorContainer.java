/*
 * @file MinecoprocessorContainer.java
 * @license GPL
 *
 * Container for synchronization between tile entity and GUI.
 */
package net.torocraft.minecoprocessors.blocks;

import net.torocraft.minecoprocessors.ModContent;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.IntArray;


public class MinecoprocessorContainer extends Container
{
  private static final int PLAYER_INV_START_SLOTNO = MinecoprocessorTileEntity.NUM_OF_SLOTS;
  private final IInventory inventory_;
  private final PlayerEntity player_;
  private final IWorldPosCallable wpc_;
  private final IIntArray fields_;

  public MinecoprocessorContainer(int cid, PlayerInventory player_inventory)
  { this(cid, player_inventory, new Inventory(MinecoprocessorTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new IntArray(MinecoprocessorTileEntity.NUM_OF_FIELDS)); }

  public MinecoprocessorContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, IIntArray fields)
  {
    super(ModContent.CT_MINECOPROCESSOR, cid);
    fields_ = fields;
    wpc_ = wpc;
    player_ = player_inventory.player;
    inventory_ = block_inventory;
    int i=-1;
    // Book slot
    addSlot(new Slot(inventory_, ++i, 80, 35){
      @Override public boolean isItemValid(ItemStack stack) { return stack.getItem() == ModContent.CODE_BOOK; }
    });
    // Player slots
    for(int x=0; x<9; ++x) {
      addSlot(new Slot(player_inventory, x, 8+x*18, 142)); // player slots: 0..8
    }
    for(int y=0; y<3; ++y) {
      for(int x=0; x<9; ++x) {
        addSlot(new Slot(player_inventory, x+y*9+9, 8+x*18, 84+y*18)); // player slots: 9..35
      }
    }
    this.trackIntArray(fields_); // === Add reference holders
  }

  @Override
  public boolean canInteractWith(PlayerEntity player)
  { return inventory_.isUsableByPlayer(player); }

  @Override
  public boolean canMergeSlot(ItemStack stack, Slot slot)
  { return false; }

  @Override
  public ItemStack transferStackInSlot(PlayerEntity player, int index)
  {
    Slot slot = getSlot(index);
    if((slot==null) || (!slot.getHasStack())) return ItemStack.EMPTY;
    ItemStack slot_stack = slot.getStack();
    ItemStack transferred = slot_stack.copy();
    if((index>=0) && (index<PLAYER_INV_START_SLOTNO)) {
      // Device slots
      if(!mergeItemStack(slot_stack, PLAYER_INV_START_SLOTNO, PLAYER_INV_START_SLOTNO+36, false)) return ItemStack.EMPTY;
    } else if( (index >= PLAYER_INV_START_SLOTNO) && (index <= PLAYER_INV_START_SLOTNO+36) ) {
      // Player slot
      if(!mergeItemStack(slot_stack, 0, PLAYER_INV_START_SLOTNO, false)) return ItemStack.EMPTY;
    } else {
      // invalid slot
      return ItemStack.EMPTY;
    }
    if(slot_stack.isEmpty()) {
      slot.putStack(ItemStack.EMPTY);
    } else {
      slot.onSlotChanged();
    }
    if(slot_stack.getCount() == transferred.getCount()) return ItemStack.EMPTY;
    slot.onTake(player, slot_stack);
    return transferred;
  }

}



/*
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class MinecoprocessorContainer extends Container {

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
    return te.isUsableByPlayer(player);
  }

  @Override
  public ItemStack transferStackInSlot(EntityPlayer playerIn, int fromSlot) {
    ItemStack previous = ItemStack.EMPTY;
    Slot slot = this.inventorySlots.get(fromSlot);

    if (slot != null && slot.getHasStack()) {
      ItemStack current = slot.getStack();
      previous = current.copy();

      if (fromSlot == 0) {
        if (!this.mergeItemStack(current, 1, 37, true)) {
          return ItemStack.EMPTY;
        }
      } else {
        if (!this.mergeItemStack(current, 0, 1, true)) {
          return ItemStack.EMPTY;
        }
      }

      slot.onSlotChange(current, previous);

      if (current.isEmpty()) {
        slot.putStack(ItemStack.EMPTY);
      } else {
        slot.onSlotChanged();
      }

      if (current.getCount() == previous.getCount()) {
        return ItemStack.EMPTY;
      }

      slot.onTake(playerIn, current);
    }

    return previous;
  }

}
*/