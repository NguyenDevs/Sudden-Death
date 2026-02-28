package org.nguyendevs.suddendeath.GUI;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.Feature;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class AdminView extends BaseFeatureView {
    private static final long SHIFT_CLICK_COOLDOWN_MS = 500L;
    private long lastShiftClickMs = 0L;

    public AdminView(Player player) {
        super(player);
    }

    @Override
    protected String getInventoryTitle() {
        return Utils.msg("gui-admin-name");
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
            material = isEnabledInWorld ? (feature.isEvent() ? Material.LIGHT_BLUE_DYE : Material.LIME_DYE)
                    : Material.GRAY_DYE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.displayName(Utils.color("&6" + feature.getName()));
        meta.getPersistentDataContainer().set(Objects.requireNonNull(Utils.nsk("featureId")), PersistentDataType.STRING, feature.name());
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
                lore.add(Utils.color("&7" + statsInLore(feature, line)));
            }
        }

        if (!enabledWorlds.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.color(Utils.msg("gui-features")));
            for (String world : enabledWorlds) {
                lore.add(Utils.color("&f► &2" + world));
            }
        }
        lore.add(Component.empty());
        lore.add(isEnabledInWorld ? Utils.color(Utils.msg("gui-features-enabled"))
                : Utils.color(Utils.msg("gui-features-disabled")));
        lore.add(Utils.color("&eClick to " + (isEnabledInWorld ? "disable." : "enable.")));
        return lore;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getInventory())
            return;

        ItemStack item = event.getCurrentItem();
        if (!Utils.isPluginItem(item, false))
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        try {
            handleCommonClick(event);

            String featureId = meta.getPersistentDataContainer().get(Objects.requireNonNull(Utils.nsk("featureId")), PersistentDataType.STRING);
            if (featureId == null || featureId.isEmpty())
                return;

            Feature feature = Feature.valueOf(featureId);
            List<String> enabledWorlds = SuddenDeath.getInstance().getConfiguration().getConfig()
                    .getStringList(feature.getPath());
            String worldName = player.getWorld().getName();

            if (event.isShiftClick()) {
                long now = System.currentTimeMillis();
                if (now - lastShiftClickMs < SHIFT_CLICK_COOLDOWN_MS)
                    return;
                lastShiftClickMs = now;

                List<String> allWorldNames = Bukkit.getWorlds().stream().map(WorldInfo::getName)
                        .toList();
                boolean allDisabled = allWorldNames.stream().noneMatch(enabledWorlds::contains);

                if (allDisabled) {
                    for (String w : allWorldNames) {
                        if (!enabledWorlds.contains(w))
                            enabledWorlds.add(w);
                    }
                    player.sendMessage(
                            Utils.color(PREFIX + " &eYou enabled &6" + feature.getName() + " &ein &6ALL &eworlds."));
                } else {
                    enabledWorlds.removeAll(allWorldNames);
                    player.sendMessage(
                            Utils.color(PREFIX + " &eYou disabled &6" + feature.getName() + " &ein &6ALL &eworlds."));
                }
            } else {
                if (enabledWorlds.contains(worldName)) {
                    enabledWorlds.remove(worldName);
                    player.sendMessage(
                            Utils.color(PREFIX + " &eYou disabled &6" + feature.getName() + " &ein &6" + worldName + "&e."));
                } else {
                    enabledWorlds.add(worldName);
                    player.sendMessage(
                            Utils.color(PREFIX + " &eYou enabled &6" + feature.getName() + " &ein &6" + worldName + "&e."));
                }
            }

            SuddenDeath.getInstance().getConfiguration().getConfig().set(feature.getPath(), enabledWorlds);
            SuddenDeath.getInstance().getConfiguration().save();
            SuddenDeath.getInstance().getConfiguration().reload();
            open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error handling InventoryClickEvent for player: " + player.getName(), e);
            player.sendMessage(Utils.color(PREFIX + " An error occurred while processing your action."));
        }
    }
}