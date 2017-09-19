package net.torocraft.minecoprocessors.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.blocks.TileEntityMinecoprocessor;

public class MessageEnableGuiUpdates implements IMessage {

  public BlockPos pos;
  public Boolean enable;

  public static void init(int packetId) {
    Minecoprocessors.NETWORK.registerMessage(MessageEnableGuiUpdates.Handler.class, MessageEnableGuiUpdates.class, packetId, Side.SERVER);
  }

  public MessageEnableGuiUpdates() {

  }

  public MessageEnableGuiUpdates(BlockPos controlBlockPos, Boolean enable) {
    this.pos = controlBlockPos;
    this.enable = enable;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    enable = buf.readBoolean();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeBoolean(enable);
  }

  public static class Handler implements IMessageHandler<MessageEnableGuiUpdates, IMessage> {

    @Override
    public IMessage onMessage(final MessageEnableGuiUpdates message, MessageContext ctx) {
      if (message.pos == null) {
        return null;
      }
      final EntityPlayerMP payer = ctx.getServerHandler().player;
      payer.getServerWorld().addScheduledTask(new Worker(payer, message));
      return null;
    }
  }

  private static class Worker implements Runnable {

    private final EntityPlayerMP player;
    private final MessageEnableGuiUpdates message;

    public Worker(EntityPlayerMP player, MessageEnableGuiUpdates message) {
      this.player = player;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        TileEntityMinecoprocessor mp = (TileEntityMinecoprocessor) player.world.getTileEntity(message.pos);
        mp.enablePlayerGuiUpdates(player, message.enable);
      } catch (Exception e) {
        Minecoprocessors.proxy.handleUnexpectedException(e);
      }
    }

  }

}
