package slimeknights.mantle.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.fluid.transfer.FluidContainerTransferPacket;
import slimeknights.mantle.network.NetworkWrapper.NetworkDirection;
import slimeknights.mantle.network.packet.DropLecternBookPacket;
import slimeknights.mantle.network.packet.OpenLecternBookPacket;
import slimeknights.mantle.network.packet.OpenNamedBookPacket;
import slimeknights.mantle.network.packet.SwingArmPacket;
import slimeknights.mantle.network.packet.UpdateHeldPagePacket;
import slimeknights.mantle.network.packet.UpdateInventoryPagePacket;
import slimeknights.mantle.network.packet.UpdateLecternPagePacket;

public class MantleNetwork {
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.getResource("network"), "3");

  public static void registerPackets(final RegisterPayloadHandlersEvent event) {
    INSTANCE.registerPacket(OpenLecternBookPacket.class, OpenLecternBookPacket::new, NetworkDirection.PLAY_TO_CLIENT);
    INSTANCE.registerPacket(UpdateHeldPagePacket.class, UpdateHeldPagePacket::new, NetworkDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(UpdateInventoryPagePacket.class, UpdateInventoryPagePacket::new, NetworkDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(UpdateLecternPagePacket.class, UpdateLecternPagePacket::new, NetworkDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(DropLecternBookPacket.class, DropLecternBookPacket::new, NetworkDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(SwingArmPacket.class, SwingArmPacket::new, NetworkDirection.PLAY_TO_CLIENT);
    INSTANCE.registerPacket(OpenNamedBookPacket.class, OpenNamedBookPacket::new, NetworkDirection.PLAY_TO_CLIENT);
    INSTANCE.registerPacket(FluidContainerTransferPacket.class, FluidContainerTransferPacket::new, NetworkDirection.PLAY_TO_CLIENT);
    INSTANCE.onRegisterPayloadHandlers(event);
  }

  public static void registerPackets() {
    // Kept for backward compatibility if called before event, real registration happens in registerPackets(RegisterPayloadHandlersEvent)
  }
}
