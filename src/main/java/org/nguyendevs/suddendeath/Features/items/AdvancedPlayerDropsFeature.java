package org.nguyendevs.suddendeath.Features.items;

import net.kyori.adventure.text.Component;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.CustomItem;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import static org.nguyendevs.suddendeath.Utils.Utils.color;

public class AdvancedPlayerDropsFeature extends AbstractFeature {

    private SkinsRestorer skinsRestorer;

    @Override
    public void onEnable() {
        super.onEnable();
        if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null) {
            try {
                skinsRestorer = SkinsRestorerProvider.get();
                Bukkit.getConsoleSender().sendMessage(
                        color("&6[&cSudden&4Death&6] &aHooked into SkinsRestorer"));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into SkinsRestorer: " + e.getMessage());
                skinsRestorer = null;
            }
        }
    }

    @Override
    public String getName() {
        return "Advanced Player Drops";
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasMetadata("NPC")) return;
        if (!Feature.ADVANCED_PLAYER_DROPS.isEnabled(player)) return;

        try {
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                data.setBleeding(false);
                data.setInfected(false);
            }

            FileConfiguration config = Feature.ADVANCED_PLAYER_DROPS.getConfigFile().getConfig();

            if (config.getBoolean("drop-skull", false)) {
                ItemStack skull = config.getBoolean("player-skull", false)
                        ? createPlayerSkull(player)
                        : new ItemStack(Material.SKELETON_SKULL);
                player.getWorld().dropItemNaturally(player.getLocation(), skull);
            }

            dropConfiguredItem(player, config, "dropped-bones", CustomItem.HUMAN_BONE);
            dropConfiguredItem(player, config, "dropped-flesh", CustomItem.RAW_HUMAN_FLESH);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in AdvancedPlayerDropsFeature.onPlayerDeath", e);
        }
    }

    private void dropConfiguredItem(Player player, FileConfiguration config, String key, CustomItem customItem) {
        int amount = config.getInt(key, 0);
        if (amount <= 0) return;
        ItemStack item = customItem.a().clone();
        item.setAmount(amount);
        player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

    private ItemStack createPlayerSkull(Player player) {
        try {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta == null) return skull;

            meta.displayName(Component.text(player.getName() + "'s Head"));

            if (skinsRestorer != null) {
                try {
                    Optional<SkinProperty> skinProp = skinsRestorer.getPlayerStorage()
                            .getSkinOfPlayer(player.getUniqueId());
                    if (skinProp.isPresent() && setSkullTexture(meta, player.getName(), skinProp.get().getValue())) {
                        skull.setItemMeta(meta);
                        return skull;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to get SkinsRestorer skin for " + player.getName() + ": " + e.getMessage());
                }
            }

            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
            return skull;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creating player skull", e);
            return new ItemStack(Material.SKELETON_SKULL);
        }
    }

    private boolean setSkullTexture(SkullMeta meta, String playerName, String textureValue) {
        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), playerName);
            profile.setProperty(new ProfileProperty("textures", textureValue));
            meta.setPlayerProfile(profile);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set skull texture for " + playerName, e);
            return false;
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        try {
            ItemStack item = event.getSource();
            if (Utils.isPluginItem(item, false) && item.isSimilar(CustomItem.RAW_HUMAN_FLESH.a())) {
                event.setResult(CustomItem.COOKED_HUMAN_FLESH.a());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error handling FurnaceSmeltEvent", e);
        }
    }
}