package net.torocraft.minecoprocessors.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.gui.GuiMinecoprocessor;

public class MessageProcessorUpdate implements IMessage {

  public NBTTagCompound processorData;

  public static void init(int packetId) {
    Minecoprocessors.NETWORK.registerMessage(MessageProcessorUpdate.Handler.class, MessageProcessorUpdate.class, packetId, Side.CLIENT);
  }

  public MessageProcessorUpdate() {

  }

  public MessageProcessorUpdate(NBTTagCompound processorData) {
    this.processorData = processorData;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    processorData = ByteBufUtils.readTag(buf);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    ByteBufUtils.writeTag(buf, processorData);
  }

  public static class Handler implements IMessageHandler<MessageProcessorUpdate, IMessage> {

    @Override
    public IMessage onMessage(final MessageProcessorUpdate message, MessageContext ctx) {

      if (message.processorData == null || message.processorData == null) {
        return null;
      }

      System.out.println("MessageProcessorUpdate received on client");

      IThreadListener mainThread = Minecraft.getMinecraft();

      mainThread.addScheduledTask(new Runnable() {
        @Override
        public void run() {
          if(GuiMinecoprocessor.INSTANCE != null){
            GuiMinecoprocessor.INSTANCE.updateData(message.processorData);
          }
        }
      });

      return null;
    }
  }

}
