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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.IContainerListener;
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
  private final Processor processor_ = new Processor();
  private String name_ = new String();
  private String transl_ = new String();
  private String error_ = new String();
  private CompoundNBT nbt_ = new CompoundNBT();

  public MinecoprocessorContainer(int cid, PlayerInventory player_inventory)
  { this(cid, player_inventory, new Inventory(MinecoprocessorTileEntity.NUM_OF_SLOTS), IWorldPosCallable.DUMMY, new MinecoprocessorTileEntity.ContainerSyncFields()); }

  public MinecoprocessorContainer(int cid, PlayerInventory player_inventory, IInventory block_inventory, IWorldPosCallable wpc, MinecoprocessorTileEntity.ContainerSyncFields fields)
  {
    super(ModContent.CT_MINECOPROCESSOR, cid);
    fields_ = fields;
    wpc_ = wpc;
    player_ = player_inventory.player;
    inventory_ = block_inventory;
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

  public void putStackInSlot(int slotId, ItemStack stack)
  {
    super.putStackInSlot(slotId, stack);
    if(slotId==0) updateProgram(stack);
  }

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
      updateProgram(transferred);
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

  @Override
  public void addListener(IContainerListener listener)
  {
    super.addListener(listener);
    if((!(listener instanceof ServerPlayerEntity)) || (!(inventory_ instanceof MinecoprocessorTileEntity))) return;
    ServerPlayerEntity player = ((ServerPlayerEntity)listener);
    Networking.PacketContainerSyncServerToClient.sendToPlayer(player, this.windowId, getSyncData());
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
    if(!(inventory_ instanceof MinecoprocessorTileEntity)) return;
    MinecoprocessorTileEntity te = (MinecoprocessorTileEntity)inventory_;
    boolean dirty = false;
    if(nbt.contains("sleep")) { dirty = true; te.getProcessor().setWait(!te.getProcessor().isWait()); }
    if(nbt.contains("reset")) { dirty = true; te.resetProcessor(); }
    if(nbt.contains("step"))  { dirty = true; te.getProcessor().setStep(true); }
    if(dirty) te.markDirty();
  }

  @Override
  public void onServerPacketReceived(int windowId, CompoundNBT nbt)
  {
    // Data received from the server for display purposes
    if(nbt.contains("sync_data")) {
      nbt_ = nbt.getCompound("sync_data");
      if(nbt_.contains("name")) name_ = nbt_.getString("name");
      if(nbt_.contains("error")) error_ = nbt_.getString("error");
      if(nbt_.contains("transl")) transl_ = nbt_.getString("transl");
    }
  }

  // ---------------------------------------------------------------------------------------------------------------------

  public MinecoprocessorTileEntity.ContainerSyncFields getFields()
  { return fields_; }

  private void updateProgram(ItemStack stack)
  {
    name_ = MinecoprocessorTileEntity.loadBook(stack, processor_);
    wpc_.consume((world, pos)->{
      // Lambda executed only on server
      nbt_.putString("name", name_);
      Networking.PacketContainerSyncServerToClient.sendToListeners(world, this, nbt_);
    });
  }

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
    final MinecoprocessorTileEntity te = ((MinecoprocessorTileEntity)inventory_);
    nbt_.putString("name", te.hasCustomName() ? te.getCustomName().getString() : "");
    nbt_.putString("error", te.getProcessor().getError());
    nbt_.putString("transl", te.getBlockState().getBlock().getTranslationKey());
    CompoundNBT msg = new CompoundNBT();
    msg.put("sync_data", nbt_);
    return msg;
  }

}
