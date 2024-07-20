package org.nguyendevs.suddendeath.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleProtocolLibHook extends PacketAdapter implements ProtocolLibHook {
    private static final double CHANGE_Y = -2.0D;
    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final Set<LivingEntity> toCheck = new HashSet<>();

    public SimpleProtocolLibHook(JavaPlugin plugin) {
        super(plugin, ListenerPriority.NORMAL, new PacketType[]{Server.SPAWN_ENTITY});
        this.plugin = plugin;
        this.protocolManager.addPacketListener(this);
    }

    public void addEntityToChange(LivingEntity entity) {
        this.toCheck.add(entity);
    }

    private void removeEntity(LivingEntity ent) {
        this.toCheck.remove(ent);
    }

    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == Server.SPAWN_ENTITY) {
            PacketContainer packet = event.getPacket();
            LivingEntity ent = this.identifyEntity(packet);
            if (ent != null) {
                try {
                    this.changeY(packet);
                } catch (Exception var5) {
                    this.plugin.getLogger().warning("Unable to change Y of spawning entity " + ent + "(" + ent.getEntityId() + "):");
                    var5.printStackTrace();
                }

                this.removeEntity(ent);
            }
        }
    }

    private LivingEntity identifyEntity(PacketContainer packet) {
        StructureModifier<Integer> ints = packet.getIntegers();
        int entityId = (Integer)ints.read(0);
        Iterator<LivingEntity> var4 = this.toCheck.iterator();

        LivingEntity ent;
        int curId;
        do {
            if (!var4.hasNext()) {
                return null;
            }

            ent = (LivingEntity)var4.next();
            curId = ent.getEntityId();
        } while(entityId != curId);

        return ent;
    }

    private void changeY(PacketContainer packet) {
        StructureModifier<Double> doubles = packet.getDoubles();
        double prev = (Double)doubles.read(1);
        doubles.write(1, prev + -2.0D);
    }
}