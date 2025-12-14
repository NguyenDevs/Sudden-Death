package org.nguyendevs.suddendeath.Features.items;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.nguyendevs.suddendeath.Features.base.AbstractFeature;
import org.nguyendevs.suddendeath.Player.PlayerData;
import org.nguyendevs.suddendeath.Utils.CustomItem;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

// Import mới của SkinsRestorer v15
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.logging.Level;
import java.util.Optional;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AdvancedPlayerDropsFeature extends AbstractFeature {

    private SkinsRestorer skinsRestorer;

    @Override
    public void onEnable() {
        super.onEnable();
        if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null) {
            try {
                skinsRestorer = SkinsRestorerProvider.get();
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6[&cSudden&4Death&6] &aHooked into SkinRestorer"));
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
                if (config.getBoolean("player-skull", false)) {
                    ItemStack skull = createPlayerSkull(player);
                    if (skull != null) {
                        player.getWorld().dropItemNaturally(player.getLocation(), skull);
                    }
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.SKELETON_SKULL));
                }
            }

            int boneAmount = config.getInt("dropped-bones", 0);
            if (boneAmount > 0) {
                ItemStack bone = CustomItem.HUMAN_BONE.a().clone();
                bone.setAmount(boneAmount);
                player.getWorld().dropItemNaturally(player.getLocation(), bone);
            }

            int fleshAmount = config.getInt("dropped-flesh", 0);
            if (fleshAmount > 0) {
                ItemStack flesh = CustomItem.RAW_HUMAN_FLESH.a().clone();
                flesh.setAmount(fleshAmount);
                player.getWorld().dropItemNaturally(player.getLocation(), flesh);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in AdvancedPlayerDropsFeature.onPlayerDeath", e);
        }
    }

    private ItemStack createPlayerSkull(Player player) {
        try {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            if (skullMeta == null) return skull;

            skullMeta.setDisplayName(ChatColor.RESET + player.getName() + "'s Head");

            if (skinsRestorer != null) {
                try {
                    Optional<SkinProperty> skinProp = skinsRestorer.getPlayerStorage().getSkinOfPlayer(player.getUniqueId());

                    if (skinProp.isPresent()) {
                        SkinProperty property = skinProp.get();
                        if (setSkullTexture(skullMeta, player.getName(), property.getValue())) {
                            skull.setItemMeta(skullMeta);
                            return skull;
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to get SkinsRestorer skin for " + player.getName() + ": " + e.getMessage());
                }
            }

            skullMeta.setOwningPlayer(player);
            skull.setItemMeta(skullMeta);
            return skull;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creating player skull", e);
            return new ItemStack(Material.SKELETON_SKULL);
        }
    }

    private boolean setSkullTexture(SkullMeta skullMeta, String playerName, String textureValue) {
        try {
            String decoded = new String(Base64.getDecoder().decode(textureValue));
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            String urlString = json.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString();

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), playerName);
            PlayerTextures textures = profile.getTextures();

            URL url = new URL(urlString);
            textures.setSkin(url);

            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);

            return true;
        } catch (MalformedURLException e) {
            plugin.getLogger().warning("Invalid texture URL for " + playerName);
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set skull texture", e);
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