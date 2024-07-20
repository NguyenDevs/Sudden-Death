package org.nguyendevs.suddendeath.packets.v1_17;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;
import org.nguyendevs.suddendeath.packets.PacketSender;

import java.lang.reflect.InvocationTargetException;

public class ProtocolLibImpl implements PacketSender {
    private final ProtocolManager protocolManager;

    public ProtocolLibImpl(ProtocolManager protocolManager) {
        this.protocolManager = protocolManager;
    }

    public void fading(Player player, int distance) {
        PacketContainer fakeDistance = new PacketContainer(PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE);
        fakeDistance.getIntegers().write(0, Integer.valueOf(distance));
        try {
            this.protocolManager.sendServerPacket(player, fakeDistance);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet " + fakeDistance, e);
        }
    }
}
