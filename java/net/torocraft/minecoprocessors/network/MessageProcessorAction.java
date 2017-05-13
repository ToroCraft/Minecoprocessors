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

public class MessageProcessorAction implements IMessage {

  public enum Action {
    RESET, PAUSE, STEP
  };

  public Action action;
  public BlockPos pos;

  public static void init(int packetId) {
    Minecoprocessors.NETWORK.registerMessage(MessageProcessorAction.Handler.class, MessageProcessorAction.class, packetId, Side.SERVER);
  }

  public MessageProcessorAction() {

  }

  public MessageProcessorAction(BlockPos pos, Action action) {
    this.action = action;
    this.pos = pos;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    action = Action.values()[buf.readInt()];
    pos = BlockPos.fromLong(buf.readLong());
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeInt(action.ordinal());
    buf.writeLong(pos.toLong());
  }

  public static class Handler implements IMessageHandler<MessageProcessorAction, IMessage> {

    @Override
    public IMessage onMessage(final MessageProcessorAction message, MessageContext ctx) {
      if (message.action == null) {
        return null;
      }
      final EntityPlayerMP payer = ctx.getServerHandler().playerEntity;
      payer.getServerWorld().addScheduledTask(new Worker(payer, message));
      return null;
    }
  }

  private static class Worker implements Runnable {

    private final EntityPlayerMP player;
    private final MessageProcessorAction message;

    public Worker(EntityPlayerMP player, MessageProcessorAction message) {
      this.player = player;
      this.message = message;
    }

    @Override
    public void run() {
      try {
        TileEntityMinecoprocessor mp = (TileEntityMinecoprocessor) player.world.getTileEntity(message.pos);

        switch (message.action) {
          case PAUSE:
            mp.getProcessor().setWait(!mp.getProcessor().isWait());
            mp.updatePlayers();
            break;
          case RESET:
            mp.reset();
            break;
          case STEP:
            mp.getProcessor().setStep(true);
            break;
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

}
