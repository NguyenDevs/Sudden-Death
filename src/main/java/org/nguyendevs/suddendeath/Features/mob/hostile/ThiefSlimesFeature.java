package org.nguyendevs.suddendeath.Features.mob.hostile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Player.ExperienceCalculator;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.NoInteractItemEntity;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class ThiefSlimesFeature extends AbstractFeature {

    @Override
    public String getName() {
        return "Thief Slimes";
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamage() <= 0 || event.getEntity().hasMetadata("NPC")) return;

        try {
            if (event.getEntity() instanceof Player player &&
                    (event.getDamager() instanceof Slime || event.getDamager() instanceof MagmaCube) &&
                    Feature.THIEF_SLIMES.isEnabled(player)) {
                applyThiefEffect(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in ThiefSlimes event", e);
        }
    }

    private void applyThiefEffect(Player player) {
        double chance = Feature.THIEF_SLIMES.getDouble("chance-percent") / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        int exp = (int) Feature.THIEF_SLIMES.getDouble("exp");
        ExperienceCalculator calculator = new ExperienceCalculator(player);
        int currentExp = calculator.getTotalExperience();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2.0f, 1.0f);

        Component message = Component.text(Utils.msg("lost-exp").replace("#exp#", String.valueOf(exp)),
                NamedTextColor.DARK_RED);
        player.sendActionBar(message);

        for (int i = 0; i < 8; i++) {
            ItemStack stack = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;
            meta.displayName(Component.text("BOUNTYHUNTERS:chest " + player.getUniqueId() + " " + i));
            stack.setItemMeta(meta);

            NoInteractItemEntity item = new NoInteractItemEntity(player.getLocation(), stack);
            Bukkit.getScheduler().runTaskLater(plugin, item::close, 30 + RANDOM.nextInt(30));
        }

        calculator.setTotalExperience(Math.max(currentExp - exp, 0));
    }
}