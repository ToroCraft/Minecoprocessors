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

public class MessageProcessorRequest implements IMessage {

  public BlockPos pos;

  public static void init(int packetId) {
    Minecoprocessors.NETWORK
        .registerMessage(MessageProcessorRequest.Handler.class, MessageProcessorRequest.class,
            packetId, Side.SERVER);
  }

  public MessageProcessorRequest() {

  }

  public MessageProcessorRequest(BlockPos controlBlockPos) {
    this.pos = controlBlockPos;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    try {
      pos = BlockPos.fromLong(buf.readLong());
    } catch (Exception e) {
      pos = null;
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    if (pos == null) {
      throw new NullPointerException("control block is null");
    }
    buf.writeLong(pos.toLong());
  }

  public static class Handler implements IMessageHandler<MessageProcessorRequest, IMessage> {

    @Override
    public IMessage onMessage(final MessageProcessorRequest message, MessageContext ctx) {
      if (message.pos == null) {
        return null;
      }
      final EntityPlayerMP payer = ctx.getServerHandler().playerEntity;
      payer.getServerWorld().addScheduledTask(new Worker(payer, message));
      return null;
    }
  }

  private static class Worker implements Runnable {

    private final EntityPlayerMP player;
    private final MessageProcessorRequest message;

    public Worker(EntityPlayerMP player, MessageProcessorRequest message) {
      this.player = player;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        TileEntityMinecoprocessor mp = (TileEntityMinecoprocessor) player.world.getTileEntity(message.pos);

        mp.startUpdatingPlayer(player);


      }catch(Exception e){
        e.printStackTrace();
      }

      //Minecoprocessors.NETWORK.sendTo(new MessageLegalMovesResponse(te.getPos(), moves.legalPositions), player);
    }

  }

}
