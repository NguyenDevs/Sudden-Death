package org.nguyendevs.suddendeath.packets.v1_8;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.nguyendevs.suddendeath.packets.PacketSender;
import org.bukkit.entity.Player;
import java.lang.reflect.InvocationTargetException;

public class LegacyProtocolLibImpl implements PacketSender {
    private final ProtocolManager protocolManager;

    public LegacyProtocolLibImpl(ProtocolManager protocolManager) {
        this.protocolManager = protocolManager;
    }

    public void fading(Player player, int distance) {
        PacketContainer fakeDistance = new PacketContainer(PacketType.Play.Server.WORLD_BORDER);
        fakeDistance.getWorldBorderActions().write(0, EnumWrappers.WorldBorderAction.SET_WARNING_BLOCKS);
        fakeDistance.getIntegers().write(2, Integer.valueOf(distance));
        try {
            this.protocolManager.sendServerPacket(player, fakeDistance);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet " + fakeDistance, e);
        }
    }
}
