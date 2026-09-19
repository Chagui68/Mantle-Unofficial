package slimeknights.mantle.network.packet;

import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.item.ILecternBookItem;

/**
 * Packet to open a book on a lectern
 */
@AllArgsConstructor
public class OpenLecternBookPacket implements IThreadsafePacket {
  private final BlockPos pos;
  private final ItemStack book;

  public OpenLecternBookPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    net.minecraft.network.RegistryFriendlyByteBuf reg = (buffer instanceof net.minecraft.network.RegistryFriendlyByteBuf r) ? r : new net.minecraft.network.RegistryFriendlyByteBuf(buffer, net.minecraft.core.RegistryAccess.EMPTY);
    this.book = ItemStack.OPTIONAL_STREAM_CODEC.decode(reg);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    net.minecraft.network.RegistryFriendlyByteBuf reg = (buffer instanceof net.minecraft.network.RegistryFriendlyByteBuf r) ? r : new net.minecraft.network.RegistryFriendlyByteBuf(buffer, net.minecraft.core.RegistryAccess.EMPTY);
    ItemStack.OPTIONAL_STREAM_CODEC.encode(reg, book);
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    if (book.getItem() instanceof ILecternBookItem) {
      ((ILecternBookItem)book.getItem()).openLecternScreenClient(pos, book);
    }
  }
}
