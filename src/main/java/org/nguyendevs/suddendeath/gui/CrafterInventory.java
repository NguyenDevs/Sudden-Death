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

/**
 * Manages the crafting recipe GUI for the SuddenDeath plugin.
 * Displays craftable items in a 9-slot inventory and shows recipes in a 45-slot inventory upon item click.
 *
 * Enhanced security version that prevents all forms of item extraction.
 */
public class CrafterInventory implements Listener {
    private static final int RECIPE_INVENTORY_SIZE = 45;
    private static final int MAIN_INVENTORY_SIZE = 9;
    private static final String MAIN_INVENTORY_TITLE = translateColors(Utils.msg("gui-recipe-name") != null ? Utils.msg("gui-recipe-name") : "&5SuddenDeath Recipes");
    private static final String RECIPE_TITLE_PREFIX = translateColors(Utils.msg("gui-crafter-name") != null ? Utils.msg("gui-crafter-name") + " " : "&8Recipe: ");

    private final Player player;
    private final UUID playerUUID;
    private final Inventory mainInventory;

    // Track all inventories created by this instance
    private final Set<Inventory> trackedInventories = new HashSet<>();
    private final Map<Integer, ItemStack> originalPlayerItems = new HashMap<>();

    // Security flags
    private volatile boolean isClosing = false;
    private Inventory currentOpenInventory = null;

    private static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Constructs and opens a 9-slot inventory displaying craftable items for the player.
     *
     * @param player The player to open the inventory for.
     */
    public CrafterInventory(Player player) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.mainInventory = Bukkit.createInventory(null, MAIN_INVENTORY_SIZE, MAIN_INVENTORY_TITLE);
        this.trackedInventories.add(mainInventory);

        // Store original player inventory state for security check
        storeOriginalPlayerInventory();

        populateMainInventory();
        openInventorySafely(mainInventory);

        // Register events with HIGHEST priority to intercept everything
        SuddenDeath.getInstance().getServer().getPluginManager().registerEvents(this, SuddenDeath.getInstance());
    }

    /**
     * Store player's original inventory state to detect unauthorized changes
     */
    private void storeOriginalPlayerInventory() {
        originalPlayerItems.clear();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                originalPlayerItems.put(i, item.clone());
            }
        }
    }

    /**
     * Safely open inventory and track it
     */
    private void openInventorySafely(Inventory inventory) {
        currentOpenInventory = inventory;
        player.openInventory(inventory);
    }

    /**
     * Check if an inventory belongs to this GUI system
     */
    private boolean isOurInventory(Inventory inventory) {
        if (inventory == null) return false;

        // Check if it's tracked
        if (trackedInventories.contains(inventory)) return true;

        // Check by title patterns
        String title = "";
        try {
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory().equals(inventory)) {
                title = player.getOpenInventory().getTitle();
            }
        } catch (Exception e) {
            // Fallback checks
        }

        return title.equals(MAIN_INVENTORY_TITLE) ||
                title.startsWith(RECIPE_TITLE_PREFIX) ||
                inventory.getSize() == MAIN_INVENTORY_SIZE ||
                inventory.getSize() == RECIPE_INVENTORY_SIZE;
    }

    /**
     * Check if a click event is related to our GUI
     */
    private boolean isOurGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !event.getWhoClicked().getUniqueId().equals(playerUUID)) {
            return false;
        }

        return isOurInventory(event.getInventory()) ||
                isOurInventory(event.getClickedInventory()) ||
                isOurInventory(event.getView().getTopInventory());
    }

    /**
     * Populates the main inventory with craftable items from items.yml and a close button.
     */
    private void populateMainInventory() {
        FileConfiguration itemsConfig = SuddenDeath.getInstance().items.getConfig();
        int slot = 0;

        for (CustomItem customItem : CustomItem.values()) {
            String itemKey = customItem.name();
            if (itemsConfig.getBoolean(itemKey + ".craft-enabled", false)) {
                if (slot >= MAIN_INVENTORY_SIZE - 1) { // Reserve slot 8 for close button
                    break;
                }
                mainInventory.setItem(slot++, customItem.a());
            }
        }

        // Set LIME_STAINED_GLASS_PANE as close button in slot 8
        ItemStack closePane = createSecureItem(Material.LIME_STAINED_GLASS_PANE,
                Utils.msg("gui-recipe-close") != null ? Utils.msg("gui-recipe-close") : "&cClose", true);
        mainInventory.setItem(8, closePane);
    }

    /**
     * Create a secure item that cannot be extracted, with lore only for specific secure items
     */
    private ItemStack createSecureItem(Material material, String displayName, boolean isSecure) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(translateColors(displayName));
            if (isSecure) {
                // Không thêm lore để ẩn thông tin
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES); // Tùy chọn ẩn thông tin
                // Đánh dấu bằng PersistentDataContainer thay vì lore
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


    /**
     * Check if an item is one of our secure GUI items
     */
    private boolean isSecureGUIItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(
                new NamespacedKey(SuddenDeath.getInstance(), "secure_gui_item"),
                org.bukkit.persistence.PersistentDataType.BYTE
        );
    }


    /**
     * Handle all inventory clicks with targeted restrictions
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isClosing) return;

        if (!isOurGUIClick(event)) return;

        Player clickedPlayer = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        // Handle clicks in main inventory (9-slot)
        if (clickedInventory != null && clickedInventory.equals(mainInventory)) {
            event.setCancelled(true); // Prevent item extraction
            handleMainInventoryClick(event);
            return;
        }

        // Handle clicks in recipe inventory (45-slot)
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

        // ✅ Dọn dẹp nếu có secure item trên tay hoặc bị click
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


    /**
     * Handle clicks in the main inventory (9-slot)
     */
    private void handleMainInventoryClick(InventoryClickEvent event) {
        Player clickedPlayer = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 8) {
            // Close button clicked
            playSound(clickedPlayer, Sound.ITEM_BOOK_PAGE_TURN);
            closeGUI();
            return;
        }

        // Handle recipe item clicks (slots 0-7)
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

    /**
     * Handle clicks in the recipe inventory (45-slot)
     */
    private void handleRecipeInventoryClick(InventoryClickEvent event) {
        Player clickedPlayer = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 44) {
            // Back button clicked
            playSound(clickedPlayer, Sound.ITEM_BOOK_PAGE_TURN);
            openInventorySafely(mainInventory);
        }
        // Other slots in recipe inventory are for viewing only, no action needed
    }

    /**
     * Prevent all drag operations
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isClosing) return;

        if (event.getWhoClicked().getUniqueId().equals(playerUUID)) {
            // Check if any slot belongs to our inventory
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize() &&
                        isOurInventory(event.getView().getTopInventory())) {
                    event.setCancelled(true);

                    // Force clear cursor
                    Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    });
                    return;
                }
            }

            // Check if dragging our secure items
            if (isSecureGUIItem(event.getOldCursor()) || isSecureGUIItem(event.getCursor())) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    cleanupPlayerInventory();
                });
            }
        }
    }

    /**
     * Prevent inventory move events
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isOurInventory(event.getSource()) || isOurInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    /**
     * Monitor for any unauthorized inventory changes
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUUID)) return;

        if (isOurInventory(event.getInventory())) {
            // Delay cleanup to allow for inventory switching
            Bukkit.getScheduler().runTaskLater(SuddenDeath.getInstance(), () -> {
                if (!isPlayerInOurGUI()) {
                    cleanup();
                }
            }, 1L);
        }
    }

    /**
     * Check if player is currently in our GUI
     */
    private boolean isPlayerInOurGUI() {
        try {
            return player.getOpenInventory() != null &&
                    isOurInventory(player.getOpenInventory().getTopInventory());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Additional security: Monitor for dropped items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUUID)) return;

        if (isSecureGUIItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            cleanupPlayerInventory();
        }
    }

    /**
     * Clean up any secure items that somehow ended up in player inventory
     */
    private void cleanupPlayerInventory() {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSecureGUIItem(item)) {
                player.getInventory().setItem(i, new ItemStack(Material.AIR));
            }
        }
        player.updateInventory();
    }

    /**
     * Opens a 45-slot inventory displaying the crafting recipe for the specified item.
     */
    private void openRecipeInventory(CustomItem customItem) {
        FileConfiguration itemsConfig = SuddenDeath.getInstance().items.getConfig();
        String itemKey = customItem.name();
        String title = translateColors(RECIPE_TITLE_PREFIX + Utils.displayName(customItem.a()));
        Inventory recipeInventory = Bukkit.createInventory(null, RECIPE_INVENTORY_SIZE, title);
        trackedInventories.add(recipeInventory);

        // Set GRAY_STAINED_GLASS_PANE in fixed slots
        int[] graySlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43};
        ItemStack grayPane = createSecureItem(Material.GRAY_STAINED_GLASS_PANE, "&7 ", true);

        for (int slot : graySlots) {
            recipeInventory.setItem(slot, grayPane);
        }

        // Set LIME_STAINED_GLASS_PANE in slot 44 (Back button)
        ItemStack limePane = createSecureItem(Material.LIME_STAINED_GLASS_PANE,
                Utils.msg("gui-crafter-back") != null ? Utils.msg("gui-crafter-back") : "&aBack", true);
        recipeInventory.setItem(44, limePane);

        // Define crafting grid slots
        int[] craftingSlots = {11, 12, 13, 20, 21, 22, 29, 30, 31};
        List<String> recipe = itemsConfig.getStringList(itemKey + ".craft");

        if (recipe.size() != 3) {
            return;
        }

        // Parse recipe and set materials
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
                        meta.setDisplayName(translateColors(mat.name().replace("_", " ")));
                        item.setItemMeta(meta);
                    }
                } else {
                    item = createSecureItem(Material.BLACK_STAINED_GLASS_PANE, " ", true);
                }

                recipeInventory.setItem(craftingSlots[slotIndex], item);
                slotIndex++;
            }
        }

        // Set result item in slot 24 without secure lore
        ItemStack resultItem = customItem.a().clone();
        recipeInventory.setItem(24, resultItem);

        openInventorySafely(recipeInventory);
    }

    /**
     * Safely play sound
     */
    private void playSound(Player player, Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            // Ignore sound errors
        }
    }

    /**
     * Close the GUI safely
     */
    private void closeGUI() {
        isClosing = true;
        Bukkit.getScheduler().runTask(SuddenDeath.getInstance(), () -> {
            player.closeInventory();
            cleanup();
        });
    }

    /**
     * Clean up resources and unregister events
     */
    private void cleanup() {
        isClosing = true;

        // Final security check
        cleanupPlayerInventory();

        // Clear tracking
        trackedInventories.clear();
        originalPlayerItems.clear();
        currentOpenInventory = null;

        // Unregister events
        try {
            org.bukkit.event.HandlerList.unregisterAll(this);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
}