/*
 * @file MinecoprocessorContainer.java
 * @license GPL
 *
 * Container for synchronization between tile entity and GUI.
 */
package net.torocraft.minecoprocessors.blocks;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.ModContent;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.IWorldPosCallable;
import net.torocraft.minecoprocessors.network.Networking;


public class MinecoprocessorContainer extends Container implements Networking.INetworkSynchronisableContainer
{
  private static final int PLAYER_INV_START_SLOTNO = MinecoprocessorTileEntity.NUM_OF_SLOTS;
  private final IInventory inventory_;
  private final PlayerEntity player_;
  private final IWorldPosCallable wpc_;
  private final MinecoprocessorTileEntity.ContainerSyncFields fields_;

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

  public MinecoprocessorTileEntity.ContainerSyncFields getFields()
  { return fields_; }

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
  public void onServerPacketReceived(int windowId, CompoundNBT nbt)
  {
    // Reference/purpose: Porting of 1.12 MessageProcessorUpdate
    // On the client the TE data can be updated here to have the full synchronised state of the server TE.
    // May be needed for displaying error string, and sycning the loaded code for stepping.
  }

  @Override
  public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
  {
    if(!(inventory_ instanceof MinecoprocessorTileEntity)) return;
    MinecoprocessorTileEntity te = (MinecoprocessorTileEntity)inventory_;
System.out.println("onClientPacketReceived: " + nbt.toString());
    if(nbt.contains("sleep")) te.getProcessor().setWait(!te.getProcessor().isWait());
    if(nbt.contains("reset")) te.resetProcessor();
    if(nbt.contains("step")) te.getProcessor().setStep(true);
    te.markDirty();
  }
}

// ---------------------------------------------------------------------------------------------------------------------
// MessageEnableGuiUpdates from 1.12 branch.
// -> TE data sync for clients that have the processor GUI open should be somehow done via the container in 1.14.
// ---------------------------------------------------------------------------------------------------------------------

//public class MessageEnableGuiUpdates implements IMessage {
//
//  public BlockPos pos;
//  public Boolean enable;
//
//  public static void init(int packetId) {
//    Minecoprocessors.NETWORK.registerMessage(MessageEnableGuiUpdates.Handler.class, MessageEnableGuiUpdates.class, packetId, Side.SERVER);
//  }
//
//  public MessageEnableGuiUpdates() {
//
//  }
//
//  public MessageEnableGuiUpdates(BlockPos controlBlockPos, Boolean enable) {
//    this.pos = controlBlockPos;
//    this.enable = enable;
//  }
//
//  @Override
//  public void fromBytes(ByteBuf buf) {
//    pos = BlockPos.fromLong(buf.readLong());
//    enable = buf.readBoolean();
//  }
//
//  @Override
//  public void toBytes(ByteBuf buf) {
//    buf.writeLong(pos.toLong());
//    buf.writeBoolean(enable);
//  }
//
//  public static class Handler implements IMessageHandler<MessageEnableGuiUpdates, IMessage> {
//
//    @Override
//    public IMessage onMessage(final MessageEnableGuiUpdates message, MessageContext ctx) {
//      if (message.pos == null) {
//        return null;
//      }
//      final EntityPlayerMP payer = ctx.getServerHandler().player;
//      payer.getServerWorld().addScheduledTask(new Worker(payer, message));
//      return null;
//    }
//  }
//
//  private static class Worker implements Runnable {
//
//    private final EntityPlayerMP player;
//    private final MessageEnableGuiUpdates message;
//
//    public Worker(EntityPlayerMP player, MessageEnableGuiUpdates message) {
//      this.player = player;
//      this.message = message;
//    }
//
//    @Override
//    public void run() {
//      try {
//        TileEntityMinecoprocessor mp = (TileEntityMinecoprocessor) player.world.getTileEntity(message.pos);
//        mp.enablePlayerGuiUpdates(player, message.enable);
//      } catch (Exception e) {
//        Minecoprocessors.proxy.handleUnexpectedException(e);
//      }
//    }
//
//  }
//
//}
