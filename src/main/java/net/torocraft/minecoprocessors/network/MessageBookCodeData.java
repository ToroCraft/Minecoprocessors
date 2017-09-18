package net.torocraft.minecoprocessors.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.items.ItemBookCode;

import java.io.IOException;

public final class MessageBookCodeData implements IMessage {
    private NBTTagCompound nbt;

    public static void init(int packetId) {
        Minecoprocessors.NETWORK.registerMessage(MessageBookCodeData.Handler.class, MessageBookCodeData.class, packetId, Side.SERVER);
    }

    public MessageBookCodeData(final NBTTagCompound nbt) {
        this.nbt = nbt;
    }

    @SuppressWarnings("unused") // For deserialization.
    public MessageBookCodeData() {
    }

    // --------------------------------------------------------------------- //

    public NBTTagCompound getNbt() {
        return nbt;
    }

    // --------------------------------------------------------------------- //
    // IMessage

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer buffer = new PacketBuffer(buf);
        try {
            nbt = buffer.readCompoundTag();
        } catch (final IOException e) {
            Minecoprocessors.proxy.logger.warn("Invalid packet received.", e);
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeCompoundTag(nbt);
    }

    public static final class Handler extends AbstractMessageHandler<MessageBookCodeData> {
        @Override
        protected void onMessageSynchronized(final MessageBookCodeData message, final MessageContext context) {
            final EntityPlayer player = context.getServerHandler().player;
            if (player != null) {
                final ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
                if (ItemBookCode.isBookCode(stack)) {
                    final ItemBookCode.Data data = ItemBookCode.Data.loadFromNBT(message.getNbt());
                    ItemBookCode.Data.saveToStack(stack, data);
                }
            }
        }
    }
}
