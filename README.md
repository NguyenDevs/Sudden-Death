![image](https://github.com/user-attachments/assets/ee708002-7190-4818-ba89-347770d689ad)

Done with predictable, easy survival worlds? Brace yourself for Sudden Death! This plugin cranks up the challenge, pushing your survival skills to the limit with intense hardcore features and thrilling new mechanics. Watch out—monsters now wield unique, powerful abilities that’ll keep you on edge. Transform your Minecraft server with a fresh, heart-pounding experience!

![image](https://github.com/user-attachments/assets/9b628bd9-f92e-4d21-bf58-3f8039309d89)

**Survival features**
- **Advanced Player Drops:** Players drop a skull, bones, and human flesh on death. Flesh can be cooked.
- **Arrow Slow:** Arrows slow players for a short duration.
- **Bleeding:** Damage has a chance to cause bleeding, disabling health regen and dealing periodic damage. Stopped with a bandage.
- **Blood Screen:** Player screen turns red when damaged, based on health or interval.
- **Dangerous Coal:** Mining coal may trigger a gas pocket explosion.
- **Electricity Shock:** Powered redstone components deal damage to players periodically.
- **Fall Stun:** High falls slow players, with duration based on fall height.
- **Freddy:** Waking up has a chance to summon Freddy.
- **Hunger Nausea:** Hunger causes permanent nausea.
- **Infection:** Zombies may infect players, causing nausea and periodic damage; stopped with Strange Brew.
- **Realistic Pickup:** Players must crouch and look down to pick up items, causing brief slowdown.
- **Snow Slow:** Players without specific boots are slowed on snow.
- **Stone Stiffness:** Punching stone deals damage to players.
- **Thunderstorm:** Storms may become thunderstorms with stronger, more frequent lightning.

**Monster abilities**
- **Angry Spiders:** Spiders throw cobwebs, dealing damage and slowing players.
- **Blood Moon:** Nights may turn red, increasing player damage taken and slowing them; monsters gain speed, strength, and resistance.
- **Bone Grenades:** Skeletons may throw explosive bone grenades, causing damage and knockback.
- **Bone Wizards:** Skeletons named 'Bone Wizards' cast fireball or frost curse spells, dealing damage and slowing players.
- **Breeze Dash:** Breeze may accelerate and fire multiple WindCharges, but with reduced jump ability.
- **Creeper Revenge:** Creepers may explode on death.
- **Ender Power:** Hitting certain End mobs may blind players temporarily.
- **Everburning Blazes:** Blazes summon fire beams that ignite players.
- **Force of the Undead:** Monsters deal increased attack damage, configurable per mob.
- **Leaping Spiders:** Spiders can leap powerfully at players.
- **Mob Critical Strikes:** Monsters can deal critical strikes with extra damage, configurable per mob.
- **Nether Shield:** Magma cubes, pigmen, and blazes may block attacks, reflecting damage and igniting players.
- **Poisoned Slimes:** Slimes may poison players on hit.
- **Quick Mobs:** Monsters move faster, configurable per mob.
- **Shocking Skeleton Arrows:** Skeleton arrows shock players, slowing them.
- **Silverfishes Summon:** Zombies may spawn silverfish on death.
- **Stray Frost:** Strays may shoot frost arrows, freezing players.
- **Spider Web:** Cave spiders may shoot webs to trap players.
- **Tanky Monsters:** Monsters take reduced damage, configurable per mob.
- **Thief Slimes:** Slimes may steal EXP from players on hit.
- **Undead Gunners:** Zombies named 'Undead Gunner' cast explosive rockets.
- **Undead Rage:** Zombies and pigmen gain strength and speed when damaged.
- **Witch Scrolls:** Witches may block damage with a shield and cast runes that damage and slow players.
- **Wither Machinegun:** Wither Skeletons throw damaging coal projectiles.
- **Wither Rush:** Wither Skeletons teleport to players, dealing damage.

![image](https://github.com/user-attachments/assets/1fcdbad5-5c56-4335-96bd-d958248dc608)

**Custom Mob System** 
Unleash your creativity with Sudden Death’s custom mob system! Craft unique monsters with fully adjustable stats and spawn rates to spice up your Minecraft server. While it’s not as intricate as comprehensive plugins like MythicMobs (for example, it doesn’t support custom drops), it’s perfect for those who want a quick, hassle-free way to design mobs without spending hours on setup. Here’s what you can customize for your mobs:

- **Attack Strength**
- **Max Health**
- **Movement Speed**
- **Main & Off-Hand Items**
- **Gear & Equipment**
- **Custom Name Display** 
- **Enchantments & Leather Armor Colors** for equipped items 
- **Spawn Probability**
- **Potion Effects**
Elevate your server’s challenge and excitement with these tailored monster-making tools!

![image](https://github.com/user-attachments/assets/31b13b0c-d8e2-4634-813e-280ae6d081d7)

## Commands
- `/sds status` shows your current status (bleeding, infected...).
- `/sds admin` opens up the Admin GUI, where operators can freely enable and disable features.
- `/sds get <item-name>` gives you a specific plugin item.
- `/sds itemlist` shows every custom item, and how to get them.
- `/sds reload` reloads the configuration file (pretty useless).
- `/sds recipe` opens the crafting recipe GUI.
- `/sds clean` removes bleeding, infected... effects.
- `/sds help` shows the available commands.

- `/sdmob create <type> <mob-id>` creates a new custom monster.
- `/sdmob remove <type> <mob-id>` deletes an existing monster.
- `/sdmob edit <type> <mob-id>` opens the Mob Editor GUI.
- `/sdmob list type` shows supported monster types.
- `/sdmob list <type>` shows every custom mob of a certain type.

## Permissions
- `suddendeath.status` allow you to open the status GUI. (default:true)
- `suddendeath.admin` allow to use admin commands.
- `suddendeath.update-notify` prompts a notification on login when an update is available.
- `suddendeath.recipe` allow to open recipe GUI (default:true)
