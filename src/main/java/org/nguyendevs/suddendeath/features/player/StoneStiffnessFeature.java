package org.nguyendevs.suddendeath.features.player;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.nguyendevs.suddendeath.features.base.AbstractFeature;
import org.nguyendevs.suddendeath.util.Feature;
import org.nguyendevs.suddendeath.util.Utils;
import java.util.Set;
import java.util.logging.Level;

public class StoneStiffnessFeature extends AbstractFeature {

    private static final Set<Material> STIFFNESS_MATERIALS = Set.of(
            Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.NETHER_QUARTZ_ORE,
            Material.GOLD_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE,
            Material.EMERALD_ORE, Material.COBBLESTONE, Material.STONE_SLAB, Material.COBBLESTONE_SLAB,
            Material.BRICK_STAIRS, Material.BRICK, Material.MOSSY_COBBLESTONE
    );

    @Override
    public String getName() {
        return "Stone Stiffness";
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!Feature.STONE_STIFFNESS.isEnabled(player)) return;
        if (!event.hasBlock()) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (Utils.hasCreativeGameMode(player)) return;
        if (event.hasItem()) return;

        try {
            Block block = event.getClickedBlock();
            if (block != null && STIFFNESS_MATERIALS.contains(block.getType())) {
                Utils.damage(player, Feature.STONE_STIFFNESS.getDouble("damage"), true);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in StoneStiffnessFeature.onPlayerInteract", e);
        }
    }
}