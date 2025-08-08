package org.nguyendevs.suddendeath.gui;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.util.CustomItem;
import org.nguyendevs.suddendeath.util.Utils;

import java.util.*;

public class CrafterInventory implements Listener {
    private static final int RECIPE_INVENTORY_SIZE = 45;
    private static final int MAIN_INVENTORY_SIZE = 9;
    private static final String MAIN_INVENTORY_TITLE = translateColors(Utils.msg("gui-recipe-name") != null ? Utils.msg("gui-recipe-name") : "&5SuddenDeath Recipes");
    private static final String RECIPE_TITLE_PREFIX = translateColors(Utils.msg("gui-crafter-name") != null ? Utils.msg("gui-crafter-name") + " " : "&8Recipe: ");

    private final Player player;
    private final UUID playerUUID;
    private final Inventory mainInventory;
    private final Set<Inventory> trackedInventories = new HashSet<>();
    private final Map<Integer, ItemStack> originalPlayerItems = new HashMap<>();

    private volatile boolean isClosing = false;
    private Inventory currentOpenInventory = null;

    private static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public CrafterInventory(Player player) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.mainInventory = Bukkit.createInventory(null, MAIN_INVENTORY_SIZE, MAIN_INVENTORY_TITLE);
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
        currentOpenInventory = inventory;
        player.openInventory(inventory);
    }

    private boolean isOurInventory(Inventory inventory) {
        if (inventory == null) return false;
        if (trackedInventories.contains(inventory)) return true;
        String title = "";
        try {
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory().equals(inventory)) {
                title = player.getOpenInventory().getTitle();
            }
        } catch (Exception e) {
        }
        return title.equals(MAIN_INVENTORY_TITLE) ||
                title.startsWith(RECIPE_TITLE_PREFIX) ||
                inventory.getSize() == MAIN_INVENTORY_SIZE ||
                inventory.getSize() == RECIPE_INVENTORY_SIZE;
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
            String itemKey = customItem.name();
            if (itemsConfig.getBoolean(itemKey + ".craft-enabled", false)) {
                if (slot >= MAIN_INVENTORY_SIZE - 1) {
                    break;
                }
                mainInventory.setItem(slot++, customItem.a());
            }
        }
        ItemStack closePane = createSecureItem(Material.LIME_STAINED_GLASS_PANE,
                Utils.msg("gui-recipe-close") != null ? Utils.msg("gui-recipe-close") : "&cClose", true);
        mainInventory.setItem(8, closePane);
    }

    private ItemStack createSecureItem(Material material, String displayName, boolean isSecure) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateColors(displayName));
            if (isSecure) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(SuddenDeath.getInstance(), "secure_gui_item"),
                        org.bukkit.persistence.PersistentDataType.BYTE,
                        (byte) 1
                );
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isSecureGUIItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(
                new NamespacedKey(SuddenDeath.getInstance(), "secure_gui_item"),
                org.bukkit.persistence.PersistentDataType.BYTE
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isClosing) return;
        if (!isOurGUIClick(event)) return;
        Player clickedPlayer = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null && clickedInventory.equals(mainInventory)) {
            event.setCancelled(true);
            handleMainInventoryClick(event);
            return;
        }
        if (clickedInventory != null &&
                clickedInventory.getSize() == RECIPE_INVENTORY_SIZE &&
                isOurInventory(clickedInventory)) {
            if (event.getSlot() == 44 &&
                    (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
                event.setCancelled(true);
                handleRecipeInventoryClick(event);
                return;
            }
            if (event.getAction() == InventoryAction.PICKUP_ALL ||
                    event.getAction() == InventoryAction.PICKUP_SOME ||
                    event.getAction() == InventoryAction.PICKUP_HALF ||
                    event.getAction() == InventoryAction.PICKUP_ONE ||
                    event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    event.getAction() == InventoryAction.COLLECT_TO_CURSOR ||
                    event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.PLACE_SOME ||
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
                    event.getAction() == InventoryAction.DROP_ALL_CURSOR ||
                    event.getAction() == InventoryAction.DROP_ONE_CURSOR ||
                    event.getAction() == InventoryAction.DROP_ALL_SLOT ||
                    event.getAction() == InventoryAction.DROP_ONE_SLOT ||
                    event.getClick().isShiftClick()) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                    clickedPlayer.setItemOnCursor(new ItemStack(Material.AIR));
                });
                return;
            }
            event.setCancelled(true);
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
            playSound(clickedPlayer, Sound.ITEM_BOOK_PAGE_TURN);
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
        Player clickedPlayer = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        if (slot == 44) {
            playSound(clickedPlayer, Sound.ITEM_BOOK_PAGE_TURN);
            openInventorySafely(mainInventory);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isClosing) return;
        if (event.getWhoClicked().getUniqueId().equals(playerUUID)) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize() &&
                        isOurInventory(event.getView().getTopInventory())) {
                    event.setCancelled(true);
                    Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    });
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
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isOurInventory(event.getSource()) || isOurInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUUID)) return;
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
            return player.getOpenInventory() != null &&
                    isOurInventory(player.getOpenInventory().getTopInventory());
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUUID)) return;
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
        String title = translateColors(RECIPE_TITLE_PREFIX + Utils.displayName(customItem.a()));
        Inventory recipeInventory = Bukkit.createInventory(null, RECIPE_INVENTORY_SIZE, title);
        trackedInventories.add(recipeInventory);
        int[] graySlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43};
        ItemStack grayPane = createSecureItem(Material.GRAY_STAINED_GLASS_PANE, "&7 ", true);
        for (int slot : graySlots) {
            recipeInventory.setItem(slot, grayPane);
        }
        ItemStack limePane = createSecureItem(Material.LIME_STAINED_GLASS_PANE,
                Utils.msg("gui-crafter-back") != null ? Utils.msg("gui-crafter-back") : "&aBack", true);
        recipeInventory.setItem(44, limePane);

        int[] craftingSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};
        List<String> recipe = itemsConfig.getStringList(itemKey + ".craft");

        Map<String, String> materialNames = new HashMap<>();
        List<String> materialsList = itemsConfig.getStringList(itemKey + ".materials");
        for (String materialEntry : materialsList) {
            String[] parts = materialEntry.split(":", 2);
            if (parts.length == 2) {
                String materialKey = parts[0].trim().toUpperCase();
                String displayName = parts[1].trim();
                materialNames.put(materialKey, displayName);
            }
        }

        if (recipe.size() != 3) {
            return;
        }

        int slotIndex = 0;
        for (String row : recipe) {
            String[] materials = row.split(",");
            if (materials.length != 3) {
                return;
            }

            for (String material : materials) {
                if (slotIndex >= craftingSlots.length) {
                    break;
                }

                String trimmedMaterial = material.trim();
                Material mat = Material.getMaterial(trimmedMaterial.toUpperCase());
                ItemStack item;

                if (mat != null && mat != Material.AIR) {
                    item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        String displayName = materialNames.getOrDefault(
                                trimmedMaterial.toUpperCase(),
                                translateColors(mat.name().replace("_", " "))
                        );
                        meta.setDisplayName(translateColors(displayName));
                        item.setItemMeta(meta);
                    }
                } else {
                    item = createSecureItem(Material.BLACK_STAINED_GLASS_PANE, " ", true);
                }

                recipeInventory.setItem(craftingSlots[slotIndex], item);
                slotIndex++;
            }
        }

        ItemStack resultItem = customItem.a().clone();
        recipeInventory.setItem(24, resultItem);
        openInventorySafely(recipeInventory);
    }

    private void playSound(Player player, Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
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
        currentOpenInventory = null;
        try {
            org.bukkit.event.HandlerList.unregisterAll(this);
        } catch (Exception e) {
        }
    }
}