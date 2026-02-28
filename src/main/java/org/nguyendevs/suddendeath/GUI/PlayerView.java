package org.nguyendevs.suddendeath.GUI;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PlayerView extends BaseFeatureView {

    public PlayerView(Player player) {
        super(player);
    }

    @Override
    protected String getInventoryTitle() {
        return Utils.msg("gui-player-name");
    }

    @Override
    protected ItemStack createFeatureItem(Feature feature) {
        List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig()
                .getStringList(feature.getPath());
        boolean isEnabledInWorld = enabledWorlds.contains(player.getWorld().getName());

        Material material;
        if (visualMode && isEnabledInWorld) {
            Material vis = getRandomAnimatedMaterialFor(feature);
            material = (vis != null) ? vis : Material.LIME_DYE;
        } else {
            material = isEnabledInWorld ? Material.LIME_DYE : Material.GRAY_DYE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.displayName(color("&6" + feature.getName()));
        meta.lore(createFeatureLore(feature, enabledWorlds, isEnabledInWorld));
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> createFeatureLore(Feature feature, List<String> enabledWorlds, boolean isEnabledInWorld) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        List<String> featureLore = feature.getLore();
        if (featureLore != null) {
            for (String line : featureLore) {
                lore.add(color("&7" + statsInLore(feature, line)));
            }
        }
        if (!enabledWorlds.isEmpty()) {
            lore.add(Component.empty());
            lore.add(color(Utils.msg("gui-features")));
            for (String world : enabledWorlds) {
                lore.add(color("&f► &2" + world));
            }
        }
        lore.add(Component.empty());
        lore.add(isEnabledInWorld ? color(Utils.msg("gui-features-enabled"))
                : color(Utils.msg("gui-features-disabled")));
        return lore;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory())
            return;

        try {
            handleCommonClick(event);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling InventoryClickEvent for player: " + player.getName(), e);
            player.sendMessage(color(PREFIX + " &eAn error occurred while navigating the GUI."));
        }
    }
}