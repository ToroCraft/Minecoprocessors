/*
 * @file Networking.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2019 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * Main client/server message handling.
 */
package net.torocraft.minecoprocessors.network;

import net.torocraft.minecoprocessors.ModMinecoprocessors;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.network.NetworkDirection;
import java.util.function.Supplier;


public class Networking
{
  private static final String PROTOCOL = "1";

  private static final SimpleChannel DEFAULT_CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ModMinecoprocessors.MODID, "default_ch"))
            .clientAcceptedVersions(PROTOCOL::equals).serverAcceptedVersions(PROTOCOL::equals).networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();

  public static void init()
  {
    int discr = -1;
    DEFAULT_CHANNEL.registerMessage(++discr, PacketTileNotifyClientToServer.class, PacketTileNotifyClientToServer::compose, PacketTileNotifyClientToServer::parse, PacketTileNotifyClientToServer.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketTileNotifyServerToClient.class, PacketTileNotifyServerToClient::compose, PacketTileNotifyServerToClient::parse, PacketTileNotifyServerToClient.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketContainerSyncClientToServer.class, PacketContainerSyncClientToServer::compose, PacketContainerSyncClientToServer::parse, PacketContainerSyncClientToServer.Handler::handle);
    DEFAULT_CHANNEL.registerMessage(++discr, PacketContainerSyncServerToClient.class, PacketContainerSyncServerToClient::compose, PacketContainerSyncServerToClient::parse, PacketContainerSyncServerToClient.Handler::handle);
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Tile entity notifications
  //--------------------------------------------------------------------------------------------------------------------

  public interface IPacketTileNotifyReceiver
  {
    default void onServerPacketReceived(CompoundNBT nbt) {}
    default void onClientPacketReceived(PlayerEntity player, CompoundNBT nbt) {}
  }

  public static class PacketTileNotifyClientToServer
  {
    CompoundNBT nbt = null;
    BlockPos pos = BlockPos.ZERO;

    public static void sendToServer(BlockPos pos, CompoundNBT nbt)
    { if((pos!=null) && (nbt!=null)) DEFAULT_CHANNEL.sendToServer(new PacketTileNotifyClientToServer(pos, nbt)); }

    public static void sendToServer(TileEntity te, CompoundNBT nbt)
    { if((te!=null) && (nbt!=null)) DEFAULT_CHANNEL.sendToServer(new PacketTileNotifyClientToServer(te, nbt)); }

    public PacketTileNotifyClientToServer()
    {}

    public PacketTileNotifyClientToServer(BlockPos pos, CompoundNBT nbt)
    { this.nbt = nbt; this.pos = pos; }

    public PacketTileNotifyClientToServer(TileEntity te, CompoundNBT nbt)
    { this.nbt = nbt; pos = te.getPos(); }

    public static PacketTileNotifyClientToServer parse(final PacketBuffer buf)
    { return new PacketTileNotifyClientToServer(buf.readBlockPos(), buf.readCompoundTag()); }

    public static void compose(final PacketTileNotifyClientToServer pkt, final PacketBuffer buf)
    { buf.writeBlockPos(pkt.pos); buf.writeCompoundTag(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketTileNotifyClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          PlayerEntity player = ctx.get().getSender();
          World world = player.world;
          if(world == null) return;
          final TileEntity te = world.getTileEntity(pkt.pos);
          if(!(te instanceof IPacketTileNotifyReceiver)) return;
          ((IPacketTileNotifyReceiver)te).onClientPacketReceived(ctx.get().getSender(), pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  public static class PacketTileNotifyServerToClient
  {
    CompoundNBT nbt = null;
    BlockPos pos = BlockPos.ZERO;

    public static void sendToPlayer(PlayerEntity player, TileEntity te, CompoundNBT nbt)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer) || (te==null) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketTileNotifyServerToClient(te, nbt), ((ServerPlayerEntity)player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    public PacketTileNotifyServerToClient()
    {}

    public PacketTileNotifyServerToClient(BlockPos pos, CompoundNBT nbt)
    { this.nbt=nbt; this.pos=pos; }

    public PacketTileNotifyServerToClient(TileEntity te, CompoundNBT nbt)
    { this.nbt=nbt; pos=te.getPos(); }

    public static PacketTileNotifyServerToClient parse(final PacketBuffer buf)
    { return new PacketTileNotifyServerToClient(buf.readBlockPos(), buf.readCompoundTag()); }

    public static void compose(final PacketTileNotifyServerToClient pkt, final PacketBuffer buf)
    { buf.writeBlockPos(pkt.pos); buf.writeCompoundTag(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketTileNotifyServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          World world = ModMinecoprocessors.proxy.getWorldClientSide();
          if(world == null) return;
          final TileEntity te = world.getTileEntity(pkt.pos);
          if(!(te instanceof IPacketTileNotifyReceiver)) return;
          ((IPacketTileNotifyReceiver)te).onServerPacketReceived(pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // (GUI) Container synchrsonisation
  //--------------------------------------------------------------------------------------------------------------------

  public interface INetworkSynchronisableContainer
  {
    void onServerPacketReceived(int windowId, CompoundNBT nbt);
    void onClientPacketReceived(int windowId, PlayerEntity player, CompoundNBT nbt);
  }

  public static class PacketContainerSyncClientToServer
  {
    int id = -1;
    CompoundNBT nbt = null;

    public static void sendToServer(int windowId, CompoundNBT nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketContainerSyncClientToServer(windowId, nbt)); }

    public static void sendToServer(Container container, CompoundNBT nbt)
    { if(nbt!=null) DEFAULT_CHANNEL.sendToServer(new PacketContainerSyncClientToServer(container.windowId, nbt)); }

    public PacketContainerSyncClientToServer()
    {}

    public PacketContainerSyncClientToServer(int id, CompoundNBT nbt)
    { this.nbt = nbt; this.id = id; }

    public static PacketContainerSyncClientToServer parse(final PacketBuffer buf)
    { return new PacketContainerSyncClientToServer(buf.readInt(), buf.readCompoundTag()); }

    public static void compose(final PacketContainerSyncClientToServer pkt, final PacketBuffer buf)
    { buf.writeInt(pkt.id); buf.writeCompoundTag(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketContainerSyncClientToServer pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          PlayerEntity player = ctx.get().getSender();
          if(!(player.openContainer instanceof INetworkSynchronisableContainer)) return;
          if(player.openContainer.windowId != pkt.id) return;
          ((INetworkSynchronisableContainer)player.openContainer).onClientPacketReceived(pkt.id, player,pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  public static class PacketContainerSyncServerToClient
  {
    int id = -1;
    CompoundNBT nbt = null;

    public static void sendToPlayer(PlayerEntity player, int windowId, CompoundNBT nbt)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(windowId, nbt), ((ServerPlayerEntity)player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToPlayer(PlayerEntity player, Container container, CompoundNBT nbt)
    {
      if((!(player instanceof ServerPlayerEntity)) || (player instanceof FakePlayer) || (nbt==null)) return;
      DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(container.windowId, nbt), ((ServerPlayerEntity)player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    // Container listners are private, and add/remove listener overriding seems not too nice, as
    // removeListner is client only???

    //public static <C extends Container & INetworkSynchronisableContainer> void sendToListeners(C container, CompoundNBT nbt)
    //{
    //  for(IContainerListener listener: container.getListeners()) {
    //    if(!(listener instanceof ServerPlayerEntity)) continue;
    //    DEFAULT_CHANNEL.sendTo(new PacketContainerSyncServerToClient(container.windowId, nbt), ((ServerPlayerEntity)listener).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    //  }
    //}

    public PacketContainerSyncServerToClient()
    {}

    public PacketContainerSyncServerToClient(int id, CompoundNBT nbt)
    { this.nbt=nbt; this.id=id; }

    public static PacketContainerSyncServerToClient parse(final PacketBuffer buf)
    { return new PacketContainerSyncServerToClient(buf.readInt(), buf.readCompoundTag()); }

    public static void compose(final PacketContainerSyncServerToClient pkt, final PacketBuffer buf)
    { buf.writeInt(pkt.id); buf.writeCompoundTag(pkt.nbt); }

    public static class Handler
    {
      public static void handle(final PacketContainerSyncServerToClient pkt, final Supplier<NetworkEvent.Context> ctx)
      {
        ctx.get().enqueueWork(() -> {
          PlayerEntity player = ModMinecoprocessors.proxy.getPlayerClientSide();
          if(!(player.openContainer instanceof INetworkSynchronisableContainer)) return;
          if(player.openContainer.windowId != pkt.id) return;
          ((INetworkSynchronisableContainer)player.openContainer).onServerPacketReceived(pkt.id,pkt.nbt);
        });
        ctx.get().setPacketHandled(true);
      }
    }
  }

  //--------------------------------------------------------------------------------------------------------------------
  // Item data synchrsonisation
  //--------------------------------------------------------------------------------------------------------------------

  public interface INetworkSynchronisableItem
  {
    void onServerPacketReceived(int stackid, PlayerEntity player, CompoundNBT nbt);
    void onClientPacketReceived(int stackid, PlayerEntity player, CompoundNBT nbt);
  }

  public static class PacketItemSyncClientToServer
  {
    // @todo check and implement (may also be converted to a player slot sync, depending what MC already offers).
    /// Purpose / reference for this is 1.12 MessageBookCodeData:
    //
    //  public static final class Handler extends AbstractMessageHandler<MessageBookCodeData> {
    //
    //    @Override
    //    protected void onMessageSynchronized(final MessageBookCodeData message, final MessageContext context) {
    //      final EntityPlayer player = context.getServerHandler().player;
    //      if (player != null) {
    //        final ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
    //        if (CodeBookItem.isBookCode(stack)) {
    //          final CodeBookItem.Data data = CodeBookItem.Data.loadFromNBT(message.getNbt());
    //          CodeBookItem.Data.saveToStack(stack, data);
    //        }
    //      }
    //    }
    //  }
  }

  public static class PacketItemSyncServerToClient
  {
  }

}
