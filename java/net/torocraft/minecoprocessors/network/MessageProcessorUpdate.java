package net.torocraft.minecoprocessors.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.gui.GuiMinecoprocessor;

public class MessageProcessorUpdate implements IMessage {

  public BlockPos pos;
  public NBTTagCompound processorData;

  public static void init(int packetId) {
    Minecoprocessors.NETWORK.registerMessage(MessageProcessorUpdate.Handler.class, MessageProcessorUpdate.class, packetId, Side.CLIENT);
  }

  public MessageProcessorUpdate() {

  }

  public MessageProcessorUpdate(NBTTagCompound processorData, BlockPos pos) {
    this.processorData = processorData;
    this.pos = pos;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    processorData = ByteBufUtils.readTag(buf);
    pos = BlockPos.fromLong(buf.readLong());
  }

  @Override
  public void toBytes(ByteBuf buf) {
    ByteBufUtils.writeTag(buf, processorData);
    buf.writeLong(pos.toLong());
  }

  public static class Handler implements IMessageHandler<MessageProcessorUpdate, IMessage> {

    @Override
    public IMessage onMessage(final MessageProcessorUpdate message, MessageContext ctx) {

      if (message.processorData == null || message.processorData == null) {
        return null;
      }

      IThreadListener mainThread = Minecraft.getMinecraft();

      mainThread.addScheduledTask(new Runnable() {
        @Override
        public void run() {
          if (GuiMinecoprocessor.INSTANCE == null) {
            return;
          }

          if (!GuiMinecoprocessor.INSTANCE.getPos().equals(message.pos)) {
            System.out.println("wrong proc");

            Minecoprocessors.NETWORK.sendToServer(new MessageEnableGuiUpdates(message.pos, false));

            return;
          }

          GuiMinecoprocessor.INSTANCE.updateData(message.processorData);
        }
      });

      return null;
    }
  }

}
