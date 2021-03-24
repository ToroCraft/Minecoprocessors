/*
 * @file MinecoprocessorContainer.java
 * @license GPL
 *
 * Container for synchronization between tile entity and GUI.
 */
package net.torocraft.minecoprocessors.blocks;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.network.Networking;
import net.torocraft.minecoprocessors.ModContent;
import net.torocraft.minecoprocessors.processor.Processor;


public class MinecoprocessorContainer extends Container implements Networking.INetworkSynchronisableContainer
{
  private static final int PLAYER_INV_START_SLOTNO = MinecoprocessorTileEntity.NUM_OF_SLOTS;
  private final IInventory inventory_;
  private final PlayerEntity player_;
  private final IWorldPosCallable wpc_;
  private final MinecoprocessorTileEntity.ContainerSyncFields fields_;
  private final Processor processor_;
  private String name_ = new String();
  private String transl_ = new String();
  private String error_ = new String();
  private CompoundNBT nbt_ = new CompoundNBT();
  private byte fault_code_ = 0; // Client side cache value
  private boolean loaded_state_ = false; // Client side cache value
  private boolean resync_pending_ = false; // safety lock to prevent unneeded network transfer.

  public MinecoprocessorContainer(int cid, PlayerInventory player_inventory)
  { this(cid, player_inventory, new Inventory(MinecoprocessorTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new MinecoprocessorTileEntity.ContainerSyncFields(), new Processor()); }

  public MinecoprocessorContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, MinecoprocessorTileEntity.ContainerSyncFields fields, Processor processor)
  {
    super(ModContent.CT_MINECOPROCESSOR, cid);
    fields_ = fields;
    wpc_ = wpc;
    player_ = player_inventory.player;
    inventory_ = block_inventory;
    processor_ = processor;
    int i=-1;
    // Book slot
    addSlot(new Slot(inventory_, ++i, 80, 35){
      @Override
      public boolean isItemValid(ItemStack stack)
      { return MinecoprocessorTileEntity.isValidBook(stack); }
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
    trackIntArray(fields_); // <-- Add reference holders
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

  // INetworkSynchronisableContainer ---------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public void onGuiAction(CompoundNBT nbt)
  { Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt); }

  @OnlyIn(Dist.CLIENT)
  public void onGuiAction(String key, int value)
  {
    CompoundNBT nbt = new CompoundNBT();
    nbt.putInt(key, value);
    Networking.PacketContainerSyncClientToServer.sendToServer(windowId, nbt);
  }

  @Override
  public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
  {
    // Actions executed on the server from received GUI messages
    MinecoprocessorTileEntity te = getTe();
    if(te==null) return;
    boolean dirty = false;
    if(nbt.contains("sleep")) { dirty = true; te.getProcessor().setWait(!te.getProcessor().isWait()); }
    if(nbt.contains("reset")) { dirty = true; te.resetProcessor(); }
    if(nbt.contains("step"))  { dirty = true; te.getProcessor().setStep(true); }
    if(nbt.contains("sync"))  { Networking.PacketContainerSyncServerToClient.sendToPlayer(player, this.windowId, getSyncData()); }
    if(dirty) te.markDirty();
  }

  @Override
  public void onServerPacketReceived(int windowId, CompoundNBT nbt)
  {
    // Data received from the server for display purposes
    resync_pending_ = false;
    if(nbt.contains("sync_data")) {
      nbt_ = nbt.getCompound("sync_data");
      if(nbt_.contains("name")) name_ = nbt_.getString("name");
      if(nbt_.contains("error")) error_ = nbt_.getString("error");
      if(nbt_.contains("transl")) transl_ = nbt_.getString("transl");
      if(nbt_.contains("processor")) processor_.setNBT(nbt_.getCompound("processor"));
    }
  }

  // ---------------------------------------------------------------------------------------------------------------------

  public MinecoprocessorTileEntity getTe()
  { return (inventory_ instanceof MinecoprocessorTileEntity) ? ((MinecoprocessorTileEntity)inventory_) : null; }

  public MinecoprocessorTileEntity.ContainerSyncFields getFields()
  { return fields_; }

  public String getDisplayName()
  {
    // Called in client side GUI the translation is available here.
    if((!name_.isEmpty()) && (getFields().isLoaded())) return name_;
    if(transl_.isEmpty()) return "Processor";
    return (new TranslationTextComponent(transl_)).getUnformattedComponentText();
  }

  public Processor getProcessor()
  { return processor_; }

  public String getProcessorError()
  { return error_; }

  private CompoundNBT getSyncData()
  {
    // Server data composition
    CompoundNBT msg = new CompoundNBT();
    MinecoprocessorTileEntity te = getTe();
    if(te==null) return msg;
    CompoundNBT nbt = new CompoundNBT();
    nbt.putString("name", te.hasCustomName() ? te.getCustomName().getString() : "");
    nbt.putString("error", te.getProcessor().getError());
    nbt.putString("transl", te.getBlockState().getBlock().getTranslationKey());
    nbt.put("processor", processor_.getNBT());
    msg.put("sync_data", nbt);
    return msg;
  }

  @OnlyIn(Dist.CLIENT)
  public void checkResync()
  {
    // Basic approach is to actively request data from the clients and not to push bigger
    // data sets from the server.
    if(resync_pending_) return;
    if((fields_.isLoaded() != loaded_state_) || (fields_.fault() != fault_code_)) {
      fault_code_ = fields_.fault();
      loaded_state_ = fields_.isLoaded();
      if(fault_code_ == 0) error_ = "";
      resync_pending_ = true;
      onGuiAction("sync", 1); // request the full data set from the server.
    }
  }
}
