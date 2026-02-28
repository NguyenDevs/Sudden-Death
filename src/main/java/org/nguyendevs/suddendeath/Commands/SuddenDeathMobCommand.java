package org.nguyendevs.suddendeath.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nguyendevs.suddendeath.Features.CustomMobs;
import org.nguyendevs.suddendeath.GUI.MonsterEdition;
import org.nguyendevs.suddendeath.SuddenDeath;
import org.nguyendevs.suddendeath.Utils.ConfigFile;
import org.nguyendevs.suddendeath.Utils.MobStat;
import org.nguyendevs.suddendeath.Utils.Utils;

import java.util.logging.Level;

public class SuddenDeathMobCommand implements CommandExecutor {

    private static final String PERMISSION_ADMIN = "suddendeath.admin";

    private static final String DIVIDER = "&8&m---------------[&r";
    private static final String HEADER_HELP = DIVIDER + "&d Sudden Death Help Page &8&m]---------------";
    private static final String HEADER_MOB_LIST = DIVIDER + "&d Mob List &8&m]---------------------";
    private static final String HEADER_MOB_TYPES = DIVIDER + "&d Available Mob Types &8&m]----------------";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&6[&cSudden&4Death&6] &cThis command is only available for players."));
            return true;
        }

        if (!player.hasPermission(PERMISSION_ADMIN)) {
            msg(player, Utils.msg("prefix") + " " + Utils.msg("not-enough-perms"));
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return true;
        }

        try {
            if (args.length == 0) {
                sendHelpMessage(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help" -> sendHelpMessage(player);
                case "kill" -> handleKill(player, args);
                case "edit" -> handleEdit(player, args);
                case "create" -> handleCreate(player, args);
                case "delete" -> handleDelete(player, args);
                case "list" -> handleList(player, args);
                case "spawn" -> handleSpawn(player, args);
                default -> {
                    msg(player, Utils.msg("prefix") + " " + "&cUnknown command. Use /sdmob for help.");
                    sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error processing /sdmob for: " + player.getName(), e);
            msg(player, Utils.msg("prefix") + " " + "&cAn error occurred while processing your command.");
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(color(HEADER_HELP));
        player.sendMessage(color("&d<> &7= required"));
        player.sendMessage(color("&d() &7= optional"));
        player.sendMessage(color("&d... &7= multiple args support"));
        player.sendMessage(Component.empty());
        player.sendMessage(color("&d/sdmob create <type> <name> &fcreates a new monster."));
        player.sendMessage(color("&d/sdmob edit <type> <name> &fedits an existing mob."));
        player.sendMessage(color("&d/sdmob delete <type> <name> &fdeletes an existing monster."));
        player.sendMessage(color("&d/sdmob list type &flists all supported mob types."));
        player.sendMessage(color("&d/sdmob list <type> &flists all mobs from one specific type."));
        player.sendMessage(color("&d/sdmob spawn <type> <name> <amount> &fspawns a custom mob at target block."));
        player.sendMessage(color("&d/sdmob kill <radius> &fkills every nearby custom mob."));
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f);
    }

    private void handleKill(Player player, String[] args) {
        if (args.length < 2) {
            msg(player, Utils.msg("prefix") + " " + "&cUsage: /sdmob kill <radius>");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
            if (radius <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            msg(player, Utils.msg("prefix") + " " + "&c" + args[1] + " is not a valid positive number.");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity.hasMetadata("SDCustomMob")) {
                entity.remove();
                count++;
            }
        }

        msg(player, Utils.msg("prefix") + " " + "&eSuccessfully killed " + count + " custom mob"
                + (count != 1 ? "s" : "") + ".");
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 3) {
            msg(player, Utils.msg("prefix") + " " + "&cUsage: /sdmob edit <type> <mob-id>");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null)
            return;

        String id = normalizeId(args[2]);
        ConfigFile mobs = getMobConfig(player, type);
        if (mobs == null)
            return;

        if (!mobs.getConfig().contains(id)) {
            msg(player, Utils.msg("prefix") + " " + "&cCouldn't find the mob called " + id + ".");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        new MonsterEdition(player, type, id).open();
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            msg(player, Utils.msg("prefix") + " " + "&cUsage: /sdmob create <type> <mob-id>");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null)
            return;

        String id = normalizeId(args[2]);
        ConfigFile mobs = getMobConfig(player, type);
        if (mobs == null || !isValidId(player, id, mobs))
            return;

        try {
            for (MobStat stat : MobStat.values()) {
                mobs.getConfig().set(id + "." + stat.getPath(), stat.getDefaultValue());
            }
            mobs.save();
            msg(player, Utils.msg("prefix") + " " + "&eYou successfully created a new mob: &f" + id + "&e!");
            new MonsterEdition(player, type, id).open();
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error creating mob " + id + " for: " + player.getName(), e);
            msg(player, Utils.msg("prefix") + " " + "&cAn error occurred while creating the mob.");
        }
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 3) {
            msg(player, Utils.msg("prefix") + " " + "&cUsage: /sdmob delete <type> <mob-id>");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null)
            return;

        String id = normalizeId(args[2]);
        ConfigFile mobs = getMobConfig(player, type);
        if (mobs == null)
            return;

        if (!mobs.getConfig().contains(id)) {
            msg(player, Utils.msg("prefix") + " " + "&cThere is no mob called " + id + "!");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        try {
            mobs.getConfig().set(id, null);
            mobs.save();
            msg(player, Utils.msg("prefix") + " " + "&eYou successfully removed &f" + id + "&e!");
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error deleting mob " + id + " for: " + player.getName(), e);
            msg(player, Utils.msg("prefix") + " " + "&cAn error occurred while deleting the mob.");
        }
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void handleSpawn(Player player, String[] args) {
        if (args.length < 4) {
            msg(player, Utils.msg("prefix") + " " + "&cUsage: /sdmob spawn <type> <mob-id> <amount>");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null)
            return;

        String id = normalizeId(args[2]);
        ConfigFile mobs = getMobConfig(player, type);
        if (mobs == null)
            return;

        if (!mobs.getConfig().contains(id)) {
            msg(player, Utils.msg("prefix") + " " + "&cCouldn't find the mob called " + id + ".");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
            if (amount <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            msg(player, Utils.msg("prefix") + " " + "&c" + args[3] + " is not a valid positive number.");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        org.bukkit.block.Block target = player.getTargetBlockExact(50);
        if (target == null) {
            msg(player, Utils.msg("prefix") + " " + "&cYou are not looking at a block.");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return;
        }

        org.bukkit.Location spawnLoc = target.getLocation().add(0.5, 1, 0.5);
        try {
            for (int i = 0; i < amount; i++) {
                LivingEntity entity = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, type);
                entity.setMetadata("SDCommandSpawn",
                        new FixedMetadataValue(SuddenDeath.getInstance(), true));
                CustomMobs.applyCustomMobProperties(entity, mobs.getConfig(), id);
            }
            msg(player, Utils.msg("prefix") + " " + "&eSpawned &6" + amount + " &e" + id + "!");
            sound(player, Sound.ENTITY_CHICKEN_EGG, 1.0f);
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error spawning custom mobs via command", e);
            msg(player, Utils.msg("prefix") + " " + "&cAn error occurred while spawning.");
        }
    }

    private void handleList(Player player, String[] args) {
        if (args.length < 2) {
            msg(player, Utils.msg("prefix") + " " + "&cUsage: /sdmob list <mob-type/'type'>");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return;
        }

        if (args[1].equalsIgnoreCase("type")) {
            listMobTypes(player);
            return;
        }

        EntityType type = parseEntityType(player, args[1]);
        if (type == null)
            return;

        ConfigFile configFile = getMobConfig(player, type);
        if (configFile == null)
            return;

        FileConfiguration mobs = configFile.getConfig();

        player.sendMessage(color(HEADER_MOB_LIST));
        player.sendMessage(color("&8&oFrom " + type.name()));
        player.sendMessage(Component.empty());

        try {
            if (mobs.getKeys(false).isEmpty()) {
                msg(player, Utils.msg("prefix") + " " + "&eNo custom mobs found for " + type.name() + ".");
            } else {
                for (String mobId : mobs.getKeys(false)) {
                    String mobName = mobs.getString(mobId + ".name", mobId);
                    Component entry = color(mobName)
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(mobId, NamedTextColor.WHITE)
                                    .clickEvent(ClickEvent.runCommand("/sdmob edit " + type.name() + " " + mobId))
                                    .hoverEvent(HoverEvent.showText(
                                            Component.text("Click to edit " + mobId).color(NamedTextColor.GRAY))))
                            .append(Component.text(")", NamedTextColor.GRAY));
                    player.sendMessage(entry);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error listing mobs for " + type.name() + " for: " + player.getName(), e);
            msg(player, Utils.msg("prefix") + " " + "&cAn error occurred while listing mobs.");
        }
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    private void listMobTypes(Player player) {
        player.sendMessage(color(HEADER_MOB_TYPES));
        player.sendMessage(Component.empty());
        try {
            for (EntityType mobType : EntityType.values()) {
                if (mobType.isAlive()) {
                    Component entry = Component.text(mobType.name(), NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("/sdmob list " + mobType.name()))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Click to see all mobs in " + mobType.name())));
                    player.sendMessage(entry);
                }
            }
        } catch (Exception e) {
            SuddenDeath.getInstance().getLogger().log(Level.WARNING,
                    "Error listing mob types for: " + player.getName(), e);
            msg(player, Utils.msg("prefix") + " " + "&cAn error occurred while listing mob types.");
        }
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
    }

    @Nullable
    private EntityType parseEntityType(Player player, String typeStr) {
        String normalized = typeStr.toUpperCase().replace("-", "_");
        try {
            EntityType type = EntityType.valueOf(normalized);
            if (!type.isAlive()) {
                msg(player, Utils.msg("prefix") + " " + "&c" + normalized + " is not a supported mob type.");
                return null;
            }
            return type;
        } catch (IllegalArgumentException e) {
            msg(player, Utils.msg("prefix") + " " + "&c" + normalized + " is not a supported mob type.");
            msg(player, Utils.msg("prefix") + " " + "&cUse /sdmob list type to see all available mob types.");
            sound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f);
            return null;
        }
    }

    @Nullable
    private ConfigFile getMobConfig(Player player, EntityType type) {
        ConfigFile config = SuddenDeath.getInstance().getConfigManager().getMobConfig(type);
        if (config == null) {
            msg(player, Utils.msg("prefix") + " " + "&cFailed to load configuration for type " + type.name() + ".");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
        }
        return config;
    }

    private boolean isValidId(Player player, String id, ConfigFile mobs) {
        if (Utils.isIDType(id)) {
            msg(player, Utils.msg("prefix") + " " + "&c" + id + " is not a valid ID.");
            msg(player, Utils.msg("prefix") + " " + "&cID Format: USE_THIS_FORMAT");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return false;
        }

        if (id.equalsIgnoreCase("DEFAULT_KEY")) {
            msg(player, Utils.msg("prefix") + " " + "&cThis ID is forbidden.");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return false;
        }

        if (mobs.getConfig().contains(id)) {
            msg(player, Utils.msg("prefix") + " " + "&cThere is already a mob with ID " + id + ".");
            sound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
            return false;
        }

        return true;
    }

    private String normalizeId(String raw) {
        return raw.toUpperCase().replace("-", "_");
    }

    private void msg(Player player, String message) {
        player.sendMessage(color(message));
    }

    private void sound(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 1.0f, pitch);
    }

    private Component color(String message) {
        return Utils.color(message);
    }
}