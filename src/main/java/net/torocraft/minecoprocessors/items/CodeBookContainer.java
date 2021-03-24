/*
 * @file CodeBookContainer.java
 * @license GPL
 */
package net.torocraft.minecoprocessors.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.torocraft.minecoprocessors.ModContent;
import net.torocraft.minecoprocessors.network.Networking;


public class CodeBookContainer extends Container implements Networking.INetworkSynchronisableContainer
{
  private PlayerEntity player;
  private PlayerInventory inventory;
  private final ItemStack book;
  private CodeBookItem.Data data = new CodeBookItem.Data();
  public boolean saved = false;
  public boolean close = false;

  public CodeBookContainer(int cid, PlayerInventory player_inventory)
  { this(cid, player_inventory, player_inventory.player); }

  public CodeBookContainer(int cid, PlayerInventory player_inventory, PlayerEntity player)
  {
    super(ModContent.CT_CODEBOOK, cid);
    this.inventory = player_inventory;
    this.player = player;
    if((player_inventory.currentItem < 0) || (player_inventory.currentItem >= player_inventory.getSizeInventory())) {
      close = true;
      this.book = new ItemStack(Items.AIR);
    } else {
      this.book = player_inventory.getStackInSlot(player_inventory.currentItem);
    }
    if(book.getItem() instanceof CodeBookItem) {
      data = CodeBookItem.Data.loadFromStack(book);
    } else {
      close = true;
    }
  }

  @Override
  public boolean canInteractWith(PlayerEntity player)
  { return player == this.player; }

  // INetworkSynchronisableContainer ---------------------------------------------------------

  @OnlyIn(Dist.CLIENT)
  public void save()
  {
    saved = false;
    final CompoundNBT nbt = new CompoundNBT();
    nbt.put("bookdata", data.writeNBT(new CompoundNBT()));
    Networking.PacketContainerSyncClientToServer.sendToServer(this, nbt);
  }

  @Override
  public void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt)
  {
    if(nbt.contains("bookdata")) {
      CompoundNBT booknbt = nbt.getCompound("bookdata");
      data.readNBT(booknbt);
      if(book.getItem() instanceof CodeBookItem) {
        book.setTag(booknbt);
        player.inventory.markDirty();
        nbt = new CompoundNBT();
        nbt.putBoolean("saved", true);
        Networking.PacketContainerSyncServerToClient.sendToPlayer(player, this, nbt);
      }
    }
  }

  @Override
  public void onServerPacketReceived(int windowId, CompoundNBT nbt)
  {
    if(nbt.getBoolean("saved")) {
      saved = true;
      close = true; // currently we only save on close
    }
  }

  public CodeBookItem.Data getData()
  { return data; }

}
