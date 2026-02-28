package org.nguyendevs.suddendeath.GUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.CustomItem;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.*;

public class CrafterInventory implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int RECIPE_INVENTORY_SIZE = 45;
    private static final int MAIN_INVENTORY_SIZE = 9;

    private final Player player;
    private final UUID playerUUID;
    private final Inventory mainInventory;
    private final Set<Inventory> trackedInventories = new HashSet<>();
    private final Map<Integer, ItemStack> originalPlayerItems = new HashMap<>();

    private volatile boolean isClosing = false;

    private static Component color(String message) {
        return message != null ? LEGACY.deserialize(message) : Component.empty();
    }

    private static Component getMainInventoryTitle() {
        String title = Utils.msg("gui-recipe-name");
        return color(title);
    }

    private static Component getRecipeTitlePrefix(String itemName) {
        String prefix = Utils.msg("gui-crafter-name");
        return LEGACY.deserialize(LEGACY.serialize(color(prefix + " ")) + itemName);
    }

    public CrafterInventory(Player player) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.mainInventory = Bukkit.createInventory(null, MAIN_INVENTORY_SIZE, getMainInventoryTitle());
        this.trackedInventories.add(mainInventory);
        storeOriginalPlayerInventory();

        populateMainInventory();
        openInventorySafely(mainInventory);
        SuddenDeath.getInstance().getServer().getPluginManager().registerEvents(this, SuddenDeath.getInstance());
    }

    private void storeOriginalPlayerInventory() {
        originalPlayerItems.clear();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                originalPlayerItems.put(i, item.clone());
            }
        }
    }

    private void openInventorySafely(Inventory inventory) {
        player.openInventory(inventory);
    }

    private boolean isOurInventory(Inventory inventory) {
        return inventory != null && trackedInventories.contains(inventory);
    }

    private boolean isOurGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !event.getWhoClicked().getUniqueId().equals(playerUUID)) {
            return false;
        }
        return isOurInventory(event.getInventory()) ||
                isOurInventory(event.getClickedInventory()) ||
                isOurInventory(event.getView().getTopInventory());
    }

    private void populateMainInventory() {
        FileConfiguration itemsConfig = SuddenDeath.getInstance().items.getConfig();
        int slot = 0;
        for (CustomItem customItem : CustomItem.values()) {
            if (itemsConfig.getBoolean(customItem.name() + ".craft-enabled", false)) {
                if (slot >= MAIN_INVENTORY_SIZE - 1)
                    break;
                mainInventory.setItem(slot++, customItem.a());
            }
        }
        String closeText = Utils.msg("gui-recipe-close");
        ItemStack closePane = createSecureItem(Material.ARROW, closeText);
        mainInventory.setItem(8, closePane);
    }

    private ItemStack createSecureItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(color(displayName));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            NamespacedKey key = Utils.nsk("secure_gui_item");
            if (key != null) {
                meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.BYTE,
                        (byte) 1);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isSecureGUIItem(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        NamespacedKey key = Utils.nsk("secure_gui_item");
        return key != null
                && meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isClosing || !isOurGUIClick(event))
            return;

        Player clickedPlayer = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && clickedInventory.equals(mainInventory)) {
            event.setCancelled(true);
            handleMainInventoryClick(event);
            return;
        }

        if (isOurInventory(clickedInventory) && !clickedInventory.equals(mainInventory)) {
            event.setCancelled(true);
            if (event.getSlot() == 44 && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
                handleRecipeInventoryClick(event);
                return;
            }
            Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> clickedPlayer.setItemOnCursor(new ItemStack(Material.AIR)));
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (isSecureGUIItem(cursor) || isSecureGUIItem(current)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                clickedPlayer.setItemOnCursor(new ItemStack(Material.AIR));
                cleanupPlayerInventory();
            });
        }
    }

    private void handleMainInventoryClick(InventoryClickEvent event) {
        Player clickedPlayer = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        if (slot == 8) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            closeGUI();
            return;
        }
        if (slot >= 0 && slot < 8) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR && !isSecureGUIItem(clickedItem)) {
                CustomItem customItem = CustomItem.fromItemStack(clickedItem);
                if (customItem != null) {
                    playSound(clickedPlayer, Sound.ITEM_BOOK_PAGE_TURN);
                    openRecipeInventory(customItem);
                }
            }
        }
    }

    private void handleRecipeInventoryClick(InventoryClickEvent event) {
        if (event.getSlot() == 44) {
            playSound(player, Sound.UI_BUTTON_CLICK);
            openInventorySafely(mainInventory);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isClosing || !event.getWhoClicked().getUniqueId().equals(playerUUID))
            return;

        Inventory topInventory = event.getView().getTopInventory();
        for (int slot : event.getRawSlots()) {
            if (slot < topInventory.getSize() && isOurInventory(topInventory)) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> player.setItemOnCursor(new ItemStack(Material.AIR)));
                return;
            }
        }
        if (isSecureGUIItem(event.getOldCursor()) || isSecureGUIItem(event.getCursor())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                player.setItemOnCursor(new ItemStack(Material.AIR));
                cleanupPlayerInventory();
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isOurInventory(event.getSource()) || isOurInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUUID))
            return;
        if (isOurInventory(event.getInventory())) {
            Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                if (!isPlayerInOurGUI()) {
                    cleanup();
                }
            }, 1L);
        }
    }

    private boolean isPlayerInOurGUI() {
        try {
            InventoryView view = player.getOpenInventory();
            return isOurInventory(view.getTopInventory());
        } catch (Exception ignored) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUUID))
            return;
        if (isSecureGUIItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            cleanupPlayerInventory();
        }
    }

    private void cleanupPlayerInventory() {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSecureGUIItem(item)) {
                player.getInventory().setItem(i, new ItemStack(Material.AIR));
            }
        }
        player.updateInventory();
    }

    private void openRecipeInventory(CustomItem customItem) {
        FileConfiguration itemsConfig = SuddenDeath.getInstance().items.getConfig();
        String itemKey = customItem.name();
        String itemDisplayName = Utils.displayName(customItem.a());
        if (itemDisplayName == null)
            itemDisplayName = customItem.a().getType().name();

        Inventory recipeInventory = Bukkit.createInventory(null, RECIPE_INVENTORY_SIZE,
                getRecipeTitlePrefix(itemDisplayName));
        trackedInventories.add(recipeInventory);

        ItemStack grayPane = createSecureItem(Material.GRAY_STAINED_GLASS_PANE, "&7 ");
        for (int slot : new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42,
                43 }) {
            recipeInventory.setItem(slot, grayPane);
        }

        String backText = Utils.msg("gui-crafter-back");
        ItemStack limePane = createSecureItem(Material.ARROW, backText);
        recipeInventory.setItem(44, limePane);

        int[] craftingSlots = { 11, 12, 13, 20, 21, 22, 29, 30, 31 };
        List<String> recipe = itemsConfig.getStringList(itemKey + ".craft");
        if (recipe.size() != 3)
            return;

        Map<String, String> materialNames = new HashMap<>();
        for (String materialEntry : itemsConfig.getStringList(itemKey + ".materials")) {
            String[] parts = materialEntry.split(":", 2);
            if (parts.length == 2) {
                materialNames.put(parts[0].trim().toUpperCase(), parts[1].trim());
            }
        }

        int slotIndex = 0;
        for (String row : recipe) {
            String[] materials = row.split(",");
            if (materials.length != 3)
                return;

            for (String material : materials) {
                if (slotIndex >= craftingSlots.length)
                    break;

                String trimmedMaterial = material.trim().toUpperCase();
                Material mat = Material.getMaterial(trimmedMaterial);
                ItemStack item;

                if (mat != null && mat != Material.AIR) {
                    item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        String itemName = materialNames.getOrDefault(trimmedMaterial, mat.name().replace("_", " "));
                        meta.displayName(color(itemName));
                        item.setItemMeta(meta);
                    }
                } else {
                    item = createSecureItem(Material.BLACK_STAINED_GLASS_PANE, " ");
                }
                recipeInventory.setItem(craftingSlots[slotIndex++], item);
            }
        }

        recipeInventory.setItem(24, customItem.a().clone());
        openInventorySafely(recipeInventory);
    }

    private void playSound(Player p, Sound sound) {
        try {
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private void closeGUI() {
        isClosing = true;
        Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
            player.closeInventory();
            cleanup();
        });
    }

    private void cleanup() {
        isClosing = true;
        cleanupPlayerInventory();
        trackedInventories.clear();
        originalPlayerItems.clear();
        try {
            org.bukkit.event.HandlerList.unregisterAll(this);
        } catch (Exception ignored) {
        }
    }
}