package slimeknights.mantle.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.packet.ISimplePacket;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A small network implementation/wrapper using CustomPacketPayload instead of legacy channels.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NetworkWrapper {
  private static final Map<Class<?>, CustomPacketPayload.Type<?>> TYPES = new ConcurrentHashMap<>();

  public static CustomPacketPayload.Type<? extends CustomPacketPayload> getType(Class<?> clazz) {
    return (CustomPacketPayload.Type<? extends CustomPacketPayload>) TYPES.computeIfAbsent(clazz, c ->
      new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Mantle.modId, "packet_" + c.getSimpleName().toLowerCase(Locale.ROOT)))
    );
  }

  public final NetworkWrapper network = this;

  public enum NetworkDirection {
    PLAY_TO_CLIENT,
    PLAY_TO_SERVER,
    BIDIRECTIONAL
  }

  private final ResourceLocation channelName;
  private final String version;
  private final List<PacketRegistration<?>> pendingRegistrations = new ArrayList<>();

  public NetworkWrapper(ResourceLocation channelName) {
    this(channelName, "1");
  }

  public NetworkWrapper(ResourceLocation channelName, String version) {
    this.channelName = channelName;
    this.version = version;
  }

  /**
   * Registers a new {@link ISimplePacket}
   */
  public <MSG extends ISimplePacket> void registerPacket(Class<MSG> clazz, Function<FriendlyByteBuf, MSG> decoder, @Nullable NetworkDirection direction) {
    ResourceLocation packetId = ResourceLocation.fromNamespaceAndPath(channelName.getNamespace(), channelName.getPath() + "/" + clazz.getSimpleName().toLowerCase(Locale.ROOT));
    CustomPacketPayload.Type<MSG> type = new CustomPacketPayload.Type<>(packetId);
    TYPES.put(clazz, type);

    StreamCodec<FriendlyByteBuf, MSG> codec = CustomPacketPayload.codec(ISimplePacket::encode, decoder::apply);
    IPayloadHandler<MSG> handler = (msg, ctx) -> msg.handle(ctx);

    pendingRegistrations.add(new PacketRegistration<>(type, codec, handler, direction));
  }

  public <MSG extends CustomPacketPayload> void registerPacket(Class<MSG> clazz, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, IPayloadContext> consumer, @Nullable NetworkDirection direction) {
    ResourceLocation packetId = ResourceLocation.fromNamespaceAndPath(channelName.getNamespace(), channelName.getPath() + "/" + clazz.getSimpleName().toLowerCase(Locale.ROOT));
    CustomPacketPayload.Type<MSG> type = new CustomPacketPayload.Type<>(packetId);
    TYPES.put(clazz, type);

    StreamCodec<FriendlyByteBuf, MSG> codec = CustomPacketPayload.codec(encoder::accept, decoder::apply);
    IPayloadHandler<MSG> handler = (msg, ctx) -> consumer.accept(msg, ctx);

    pendingRegistrations.add(new PacketRegistration<>(type, codec, handler, direction));
  }

  public void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar registrar = event.registrar(this.version);
    for (PacketRegistration<?> reg : pendingRegistrations) {
      reg.register(registrar);
    }
  }

  private record PacketRegistration<MSG extends CustomPacketPayload>(
    CustomPacketPayload.Type<MSG> type,
    StreamCodec<FriendlyByteBuf, MSG> codec,
    IPayloadHandler<MSG> handler,
    @Nullable NetworkDirection direction
  ) {
    void register(PayloadRegistrar registrar) {
      if (direction == NetworkDirection.PLAY_TO_SERVER) {
        registrar.playToServer(type, codec, handler);
      } else if (direction == NetworkDirection.PLAY_TO_CLIENT) {
        registrar.playToClient(type, codec, handler);
      } else {
        registrar.playBidirectional(type, codec, handler);
      }
    }
  }

  /* Sending packets */

  public void sendToServer(Object msg) {
    if (msg instanceof CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload);
    }
  }

  public void sendVanillaPacket(Packet<?> packet, Entity player) {
    if (player instanceof ServerPlayer sPlayer) {
      sPlayer.connection.send(packet);
    }
  }

  public void sendTo(Object msg, Player player) {
    if (player instanceof ServerPlayer serverPlayer) {
      sendTo(msg, serverPlayer);
    }
  }

  public void sendTo(Object msg, ServerPlayer player) {
    if (!(player instanceof FakePlayer) && msg instanceof CustomPacketPayload payload) {
      PacketDistributor.sendToPlayer(player, payload);
    }
  }

  public void sendToClientsAround(Object msg, ServerLevel serverWorld, BlockPos position) {
    if (msg instanceof CustomPacketPayload payload) {
      PacketDistributor.sendToPlayersNear(serverWorld, null, position.getX(), position.getY(), position.getZ(), 64.0, payload);
    }
  }

  public void sendToTrackingAndSelf(Object msg, Entity entity) {
    if (msg instanceof CustomPacketPayload payload) {
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }
  }

  public void sendToTracking(Object msg, Entity entity) {
    if (msg instanceof CustomPacketPayload payload) {
      PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }
  }
}
