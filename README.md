<p align="center">
  <img src="./logo.png" alt="Item64 Icon" width="400"/>
</p>

# Item64
A spigot plugin that adds unlimited custom items and a block breaking/placing event.

Created by tbm00 for play.mc64.wtf.


## Features

### **Custom PVP & non-PVP Items**
*Explosive bows, crossbows, flamethrowers, magic wands, lightning guns, custom pickaxes, candy, and an endless possibilities of items designed by you!*

Pre-Defined Items:
  - **Flamethrower** Shoots flames and causes fires.
  - **Explosive Crossbow** Shoots survival-friendly explosive arrows.
  - **Explosive Bow** Shoots extremely powerful explosive arrows.
  - **Lightning Gun** Shoots ender pearls that summon lightning.
  - **Magic Wand** Shoots random potion/beacon effects. Left-click for an offensive effect, right-click for a positive effect.
  - **3x3 Pickaxe** Breaks all blocks in a three by three radius.
  - **Smelting Pickaxe** Breaks ore and smelts it.
  - **Various Candies** Applies enhanced effects to players when they consume/use the item.

### **Block Breaking/Placing Event**
*Give items and/or run commands when players break or place specific blocks.*

The default config uses this feature for a Halloween event that gives custom candy items when players break pumpkins. If you choose to use this feature, you can re-theme it to be entirely different!

### **Survival-Friendly**
*The default config is safe to use on SMP servers.*

  - Configure ammo, cooldowns, hunger costs, money costs, randomness, and chance to find a better balance on your server.
  - Respects claims and PVP; hooks into **WorldGuard**, **GriefDefender** and **DeluxeCombat** to make sure the player doesn't bypass PVP-toggled-off or destroy claimed land.
  - Listeners to prevent players from exploiting the block breaking event.

### **Very Simple but Very Configurable**
*Drag-&-Drop install, yet powerful & highly tinkerable!*

Simply use the default config, which is survival-friendly, or customize it to your liking:
  - Delete/modify pre-defined item entries.
  - Create unlimited item entries that shoot custom projectiles, run commands, and apply effects.
  - Configure damage, ammo, hunger costs, money costs, cooldown timers, projectile randomness, lore, enchantments, effects, & more.
  - Redesign the Halloween BlockEvent to fit your server or a different theme.


## Dependencies

- **Java 17+**: REQUIRED
- **Spigot 1.18.1+**: UNTESTED ON OLDER VERSIONS
- **Vault**: OPTIONAL
- **WorldGuard**: OPTIONAL
- **GriefDefender**: OPTIONAL
- **DeluxeCombat**: OPTIONAL


## Commands & Permissions

### Commands
- `/itm help` Display this command list
- `/itm give <itemKey> [player] (#)` Spawn custom item(s)
- `/itm heal [player]` Heal yourself or [player]
- `/itm heal [player] -fx` Heal & remove effects on yourself or [player]

### Permissions
Each item has configurable permissions (in `config.yml`) that must be fulfilled for a player to use or spawn the item. The only hardcoded permissions are:
- `item64.help` Ability to display the command list *(Default: OP)*
- `item64.heal` Ability to heal a player *(Default: OP)*
- `item64.allowplace` Ability to place blacklisted blocks during BlockEvent *(Default: OP)*
- `item64.allowbreak` Ability to break blacklisted blocks during BlockEvent *(Default: OP)*

  *If you're using the default config and haven't changed any usePerms or givePerms, then the following nodes will work for you:*
  - `item64.give.<key>` Ability to spawn a particular item *(Default: OP)*
  - `item64.give.*` Ability to spawn all items *(Default: OP)*
  - `item64.use.<key>` Ability to use a particular item *(Default: OP)*
  - `item64.use.*` Ability to use all items *(Default: OP)*


## Custom Items

### ItemEntry Types

**`CONSUMABLE`** 
- Runs commands and/or gives potion effects on item consumption.
- Applicable to food, potions, milk, etc.

**`USABLE`** 
- Runs commands and/or gives potion effects on item use.
- Applicable to all items.

**`AREA_BREAK`** 
- Breaks blocks in a admin-defined radius.
- Applicable to pickaxes, axes, and shovels.
- Type `3D` will break a cuboid, `2D` will break a plane.
- Has WorldGuard & GriefDefender checks that prevents block damage if user doesn't have build perms in affected area.

**`SMELT_BREAK`** 
- Breaks block and cooks it if its stone, iron ore, copper ore, gold ore, or ancient debris.
- Applicable to pickaxes.
- Has WorldGuard & GriefDefender checks that prevents block damage if user doesn't have build perms on affected block.

**`EXPLOSIVE_ARROW`**
- Summons an explosion on arrow impact.
- Applicable to bows and crossbows.
- Has WorldGuard & GriefDefender checks that prevents block damage if user doesn't have build perms in affected area.
- Has WorldGuard & GriefDefender checks that prevents player damage if user doesn't have PVP ability in affected area.
- Has DeluxeCombat check that prevents explosions entirely if PVP is toggled off for any affected players.

**`LIGHTNING_PEARL`** 
- Shoots an ender pearl that summons a lightning strike on impact.
- Applicable to most items.
- Has WorldGuard & GriefDefender checks that prevents block damage if user doesn't have build perms in affected area.
- Has WorldGuard & GriefDefender checks that prevents player damage if user doesn't have PVP ability in affected area.
- Has DeluxeCombat check that prevents explosions entirely if PVP is toggled off for any affected players.

**`RANDOM_POTION`** 
- Shoots a random splash potion.
- Applicable to most items.
- extraDamage is only applied to potions shot via left-clicking.
- Has WorldGuard & GriefDefender checks that prevents block damage if user doesn't have build perms in affected area.
- Has WorldGuard & GriefDefender checks that prevents player damage if user doesn't have PVP ability in affected area.
- Has DeluxeCombat check that prevents explosions entirely if PVP is toggled off for any affected players.

**`FLAME_PARTICLE`** 
- Shoots flame particles that make fires.
- Applicable to most items.
- Particles don't deal any damage, so be sure to use extraDamage.
- Cooldown works differently; it's the maximum number of seconds a player can repetitively use the item.
- Has WorldGuard & GriefDefender checks that prevents block damage if user doesn't have build perms in affected area.
- Has WorldGuard & GriefDefender checks that prevents player damage if user doesn't have PVP ability in affected area.
- Has DeluxeCombat check that prevents explosions entirely if PVP is toggled off for any affected players.

**`NO_ITEM`** 
- NOT AN ITEM -- the only intended use case is to simply run commands for a BreakEvent reward.
- Not applicable on any items.

### ItemEntry Specification

<details><summary>`ID` (click to expand)</summary>
<details><summary>-- key: (STRING)</summary>

Required for *all ItemEntries*. Since listeners only apply to items with enabled entries, pre-existing items will break if you remove/change their key.
</details>
<details><summary>-- type: (STRING)</summary>

Required for *all ItemEntries*. Must be `AREA_BREAK`, `SMELT_BREAK`, `EXPLOSIVE_ARROW`, `LIGHTNING_PEARL`, `RANDOM_POTION`, `FLAME_PARTICLE`, `CONSUMABLE`, `USABLE`, or `NO_ITEM`.
</details>
<details><summary>-- enabled: (BOOLEAN)</summary>

Required for *all ItemEntries*.
</details>
<details><summary>-- givePerm: (STRING)</summary>

Required for *all types except `NO_ITEM`*.
</details>
<details><summary>-- usePerm: (STRING)</summary>

Required for *all types except `NO_ITEM`*.
</details>
<details><summary>-- item:</summary>
<details><summary>-- -- mat: (STRING)</summary>

Required for *all types except `NO_ITEM`*.
</details>
<details><summary>-- -- name: (STRING)</summary>

Applicable on *all types except `NO_ITEM`*.
</details>
<details><summary>-- -- lore: (LIST-STRING)</summary>

Applicable on *all types except `NO_ITEM`*.
</details>
<details><summary>-- -- hideEnchants: (BOOLEAN)</summary>

Applicable on *all types except `NO_ITEM`*.
</details>
<details><summary>-- -- enchantments: (LIST-STRING)</summary>

Applicable on *all types except `NO_ITEM`*. Format: `enchant-name:level`
</details>
</details>
<details><summary>-- usage:</summary>
<details><summary>-- -- moneyCost: (DOUBLE)</summary>

Applicable on *all types except `NO_ITEM`*. Requires Vault dependency to use this; leave empty/null if not using Vault.
</details>
<details><summary>-- -- hungerChance: (DOUBLE)</summary>

Applicable on *all types except `NO_ITEM`*. In range [0.0,100.0]. Defaults to 100.0, but never used if hungerCost==0.
</details>
<details><summary>-- -- hungerCost: (INTEGER)</summary>

Applicable on *all types except `NO_ITEM`*. In range [0,20].
</details>
<details><summary>-- -- cooldown: (INTEGER)</summary>

Applicable on *all types except `NO_ITEM`*. Time in seconds until next use.
</details>
<details><summary>-- -- ammoItem:</summary>
<details><summary>-- -- -- mat: (STRING)</summary>

Applicable on *all types except `NO_ITEM`*.
</details>
<details><summary>-- -- -- removeAmmoItemOnUse: (BOOLEAN)</summary>

Applicable on *all types except `NO_ITEM`*.
</details>
</details>
<details><summary>-- -- projectile:</summary>
<details><summary>-- -- -- shotRandomness: (DOUBLE)</summary>

Only applicable on `EXPLOSIVE_ARROW`, `LIGHTNING_PEARL`, `RANDOM_POTION`, & `FLAME_PARTICLE`. 
</details>
<details><summary>-- -- -- extraDamage: (DOUBLE)</summary>

Only applicable on `EXPLOSIVE_ARROW`, `LIGHTNING_PEARL`, `RANDOM_POTION`, & `FLAME_PARTICLE`. 
</details>
<details><summary>-- -- -- randomPotion:</summary>
<details><summary>-- -- -- -- rightClickEffects: (LIST-STRING)</summary>

Only applicable on `RANDOM_POTION`. Format: `effect-name:amplifier:time-in-seconds`.
</details>
<details><summary>-- -- -- -- leftClickEffects: (LIST-STRING)</summary>

Only applicable on `RANDOM_POTION`. Format: `effect-name:amplifier:time-in-seconds`.
</details>
</details>
<details><summary>-- -- -- explosiveArrow:</summary>
<details><summary>-- -- -- -- power: (FLOAT)</summary>

Only applicable on `EXPLOSIVE_ARROW`.
</details>
</details>
</details>
<details><summary>-- -- triggers:</summary>
<details><summary>-- -- -- removeItemOnUse: (BOOLEAN)</summary>

Only applicable on `CONSUMABLE` & `USABLE`.
</details>
<details><summary>-- -- -- consoleCommands: (LIST-STRING)</summary>

Only applicable on `CONSUMABLE`& `USABLE`. Use `<player>` for sender's username.
</details>
<details><summary>-- -- -- effects: (LIST-STRING)</summary>

Only applicable on `CONSUMABLE` & `USABLE`. Format: `effect-name:amplifier:time-in-seconds`.
</details>
<details><summary>-- -- -- actionBarMessage: (STRING)</summary>

Only applicable on `CONSUMABLE` & `USABLE`.
</details>
</details>
<details><summary>-- -- breakage:</summary>
<details><summary>-- -- -- radius: (INTEGER)</summary>

Only applicable on `AREA_BREAK`.
</details>
<details><summary>-- -- -- type: (STRING)</summary>

Only applicable on `AREA_BREAK`. Must be `3D` or `2D`.
</details>
</details>
</details>
<details><summary>-- breakEvent:</summary>
<details><summary>-- -- rewardChance: (DOUBLE)</summary>

Applicable on *all types* but only applicable if breakEvent.rewardBlockPlacing.enabled==true. 0 to exclude from reward calculation.
</details>
<details><summary>-- -- rewardMessage: (STRING)</summary>

Applicable on *all types* but only applicable if breakEvent.rewardBlockPlacing.enabled==true & rewardChance>0.
</details>
<details><summary>-- -- giveRewardItem: (BOOLEAN)</summary>

Applicable on *all types except `NO_ITEM`* but only applicable if breakEvent.rewardBlockPlacing.enabled==true & rewardChance>0.
</details>
<details><summary>-- -- rewardCommands: (LIST-STRING)</summary>

Applicable on *all types* but only applicable if breakEvent.rewardBlockPlacing.enabled==true & rewardChance>0.
</details>
</details>
</details>


## Default Config

```
# Item64 v0.2.26-beta by @tbm00
# https://github.com/tbm00/Item64

enabled: true

# Optional hooks for server integration.
hooks:
  Vault:
    enabled: false
  GriefDefender:
    enabled: false
    ignoredClaims: []
  DeluxeCombat:
    enabled: false
  WorldGuard:
    enabled: false

# Create your own block breaking/placing event.
# Rewards are selected from the itemEntries with a defined `blockEvent.rewardChance` > 0.
blockEvent:
  joinMessage: "&eWelcome! &6We're currently having our Halloween event -- break pumpkins to find candy!"
  joinMessageDelay: 5
  inactiveWorlds:
    - "world"
    - "world_nether"
    - "world_the_end"
  rewardBlockPlacing:
    enabled: false
    chance: 33.3
    blocks: []
  rewardBlockBreaking:
    enabled: false
    chance: 33.3
    blocks:
      - "PUMPKIN"
      - "CARVED_PUMPKIN"
      - "JACK_O_LANTERN"
  preventBlockPlacing:
    enabled: false
    message: "&cSorry, you cannot place that block during our event!"
    blocks:
      - "PUMPKIN"
      - "CARVED_PUMPKIN"
      - "JACK_O_LANTERN"
  preventBlockBreaking:
    enabled: false
    message: "&cSorry, you cannot break that block during our event!"
    blocks: []
  preventBlockGrowth:
    enabled: false
    logInConsole: false
    blocks:
      - "PUMPKIN"

# Disable/modify my pre-defined items and create your own entries as needed!
# Use https://github.com/tbm00/Item64?tab=readme-ov-file#itementry-specification for help.
itemEntries:
  "1":
    key: "FLAMETHROWER"
    type: "FLAME_PARTICLE"
    enabled: true
    givePerm: "item64.give.pvp_item.flamethrower"
    usePerm: "item64.use.pvp_item.flamethrower"
    item:
      mat: "REDSTONE_TORCH"
      name: "&6Flamethrower"
      lore:
        - "&8&oRequires Coal"
      hideEnchants: true
      enchantments:
        - "MENDING:1"
    usage:
      moneyCost: 0.00
      hungerCost: 1
      cooldown: 0
      ammoItem:
        mat: "COAL"
        removeAmmoItemOnUse: true
      projectile:
        shotRandomness: 0.42
        extraDamage: 3.0
  "2":
    key: "EXPLOSIVE_BOW"
    type: "EXPLOSIVE_ARROW"
    enabled: true
    givePerm: "item64.give.pvp_item.explosive_bow"
    usePerm: "item64.use.pvp_item.explosive_bow"
    item:
      mat: "BOW"
      name: "&6Explosive Bow"
      lore:
        - "&8&oRequires Bedrock and Arrows"
      hideEnchants: false
      enchantments:
        - "UNBREAKING:5"
        - "MENDING:1"
    usage:
      moneyCost: 0.00
      hungerCost: 0
      cooldown: 0
      ammoItem:
        mat: "BEDROCK"
        removeAmmoItemOnUse: true
      projectile:
        shotRandomness: 0.32
        extraDamage: 0.0
        explosiveArrow:
          power: 96
  "3":
    key: "EXPLOSIVE_CROSSBOW"
    type: "EXPLOSIVE_ARROW"
    enabled: true
    givePerm: "item64.give.pvp_item.explosive_crossbow"
    usePerm: "item64.use.pvp_item.explosive_crossbow"
    item:
      mat: "CROSSBOW"
      name: "&6Explosive Crossbow"
      lore:
        - "&8&oRequires TNT and Arrows"
      hideEnchants: false
      enchantments:
        - "QUICK_CHARGE:5"
        - "MENDING:1"
    usage:
      moneyCost: 0.00
      hungerCost: 5
      cooldown: 2
      ammoItem:
        mat: "TNT"
        removeAmmoItemOnUse: true
      projectile:
        shotRandomness: 0.2
        extraDamage: 0.0
        explosiveArrow:
          power: 4
  "4":
    key: "LIGHTNING_GUN"
    type: "LIGHTNING_PEARL"
    enabled: true
    givePerm: "item64.give.pvp_item.lightning_gun"
    usePerm: "item64.use.pvp_item.lightning_gun"
    item:
      mat: "ECHO_SHARD"
      name: "&6Lightning Gun"
      lore:
        - "&8&oRequires Ender Pearls"
      hideEnchants: true
      enchantments:
        - "MENDING:1"
    usage:
      moneyCost: 0.00
      hungerCost: 2
      cooldown: 2
      ammoItem:
        mat: "ENDER_PEARL"
        removeAmmoItemOnUse: true
      projectile:
        shotRandomness: 0.17
        extraDamage: 5.0
  "5":
    key: "MAGIC_WAND"
    type: "RANDOM_POTION"
    enabled: true
    givePerm: "item64.give.pvp_item.magic_wand"
    usePerm: "item64.use.pvp_item.magic_wand"
    item:
      mat: "BRUSH"
      name: "&6Magic Wand"
      lore:
        - "&9Left Click for Aggressive Effects"
        - "&9Right Click for Positive Effects"
        - "&8&oRequires Potions or Water Bottles"
      hideEnchants: true
      enchantments:
        - "MENDING:1"
    usage:
      moneyCost: 0.00
      hungerCost: 2
      cooldown: 2
      ammoItem:
        mat: "POTION"
        removeAmmoItemOnUse: true
      projectile:
        shotRandomness: 0.17
        extraDamage: 6.0
        randomPotion:
          rightClickEffects:
            - "INCREASE_DAMAGE:1:30"
            - "HEAL:1:30"
            - "JUMP:1:30"
            - "SPEED:1:30"
            - "REGENERATION:1:30"
            - "FIRE_RESISTANCE:0:30"
            - "NIGHT_VISION:1:30"
            - "INVISIBILITY:0:30"
            - "ABSORPTION:1:30"
            - "SATURATION:1:30"
            - "SLOW_FALLING:1:30"
          leftClickEffects:
            - "SLOW:1:30"
            - "HARM:1:30"
            - "CONFUSION:0:30"
            - "BLINDNESS:0:30"
            - "HUNGER:1:30"
            - "WEAKNESS:1:30"
            - "POISON:1:30"
            - "LEVITATION:2:45"
            - "GLOWING:0:30"
  "6":
    key: "AREA_PICK_3"
    type: "AREA_BREAK"
    enabled: true
    givePerm: "item64.give.area_pick_3"
    usePerm: "item64.use.area_pick_3"
    item:
      mat: "DIAMOND_PICKAXE"
      name: "&63x3 Pickaxe"
      lore:
        - "&8&oRequires TNT"
      hideEnchants: false
      enchantments:
        - "EFFICIENCY:3"
    usage:
      moneyCost: 0.00
      hungerChance: 33.3
      hungerCost: 1
      cooldown: 0
      ammoItem:
        mat: "TNT"
        removeAmmoItemOnUse: true
      breakage:
        radius: 1
        type: "2D"
  "7":
    key: "SMELT_PICK"
    type: "SMELT_BREAK"
    enabled: true
    givePerm: "item64.give.smelt_pick"
    usePerm: "item64.use.smelt_pick"
    item:
      mat: "DIAMOND_PICKAXE"
      name: "&6Smelting Pickaxe"
      lore: []
      hideEnchants: false
      enchantments:
        - "EFFICIENCY:3"
    usage:
      moneyCost: 0.00
      hungerChance: 33.3
      hungerCost: 1
      cooldown: 0
      ammoItem: []
  "8":
    key: "HEALING_HONEY"
    type: "CONSUMABLE"
    enabled: true
    givePerm: "item64.give.candy.healing_honey"
    usePerm: "item64.use.candy.healing_honey"
    item:
      mat: "HONEY_BOTTLE"
      name: "&6Healing Honey"
      lore:
        - "&eRestores health, hunger,"
        - "&eand removes potions effects"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands:
          - "itm heal <player> -fx"
        effects: []
        actionBarMessage: "&aYou gobbled the honey!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found some healing honey!"
      giveRewardItem: true
      rewardCommands: []
  "9":
    key: "SUPERAPPLE"
    type: "CONSUMABLE"
    enabled: true
    givePerm: "item64.give.candy.superapple"
    usePerm: "item64.use.candy.superapple"
    item:
      mat: "ENCHANTED_GOLDEN_APPLE"
      name: "&6Superapple"
      lore:
        - "&e30 mins of"
        - "&e  Regeneration II"
        - "&e  Absorption IV"
        - "&e  Resistance IV"
        - "&e  Fire Resistance"
        - "&e  Strength II"
        - "&e  Haste II"
        - "&e  Jump Boost II"
        - "&e  Speed I"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands:
          - "itm heal <player>"
        effects:
          - "REGENERATION:1:1800"
          - "ABSORPTION:3:1800"
          - "DAMAGE_RESISTANCE:3:1800"
          - "FIRE_RESISTANCE:0:1800"
          - "INCREASE_DAMAGE:1:1800"
          - "FAST_DIGGING:1:1800"
          - "JUMP:1:1800"
          - "SPEED:0:1800"
        actionBarMessage: "&aAn apple a day, yk what they say!"
    blockEvent:
      rewardChance: 1.0
      rewardMessage: "&6You found a superapple!"
      giveRewardItem: true
      rewardCommands: []
  "10":
    key: "KIT_KAT"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.kit_kat"
    usePerm: "item64.use.candy.kit_kat"
    item:
      mat: "DARK_OAK_SLAB"
      name: "&4Kit-Kat"
      lore:
        - "&f6 mins of"
        - "&f  Haste III"
        - "&f  Speed III"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "FAST_DIGGING:2:360"
          - "SPEED:2:360"
        actionBarMessage: "&0oOOOoOoOOoOooo &6You feel a sugar rush!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found some kit-kit!"
      giveRewardItem: true
      rewardCommands: []
  "11":
    key: "JOLLY_RANCHER"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.jolly_rancher"
    usePerm: "item64.use.candy.jolly_rancher"
    item:
      mat: "AMETHYST_SHARD"
      name: "&5Jolly Rancher"
      lore:
        - "&d7 mins of"
        - "&d  Resistance III"
        - "&d  Fire Resistance"
        - "&d  Slowness IV"
        - "&d  Mining Fatigue V"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "DAMAGE_RESISTANCE:2:420"
          - "FIRE_RESISTANCE:0:420"
          - "SLOW:3:420"
          - "SLOW_DIGGING:4:420"
        actionBarMessage: "&0oOoOoooOOoOOo &6You feel tough but sluggish!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a jolly rancher!"
      giveRewardItem: true
      rewardCommands: []
  "12":
    key: "CANDY_CARROT"
    type: "CONSUMABLE"
    enabled: true
    givePerm: "item64.give.candy.candy_carrot"
    usePerm: "item64.use.candy.candy_carrot"
    item:
      mat: "GOLDEN_CARROT"
      name: "&6Candy Carrot"
      lore:
        - "&e8 mins of"
        - "&e  Night Vision III"
        - "&e  Resistance II"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "NIGHT_VISION:2:480"
          - "DAMAGE_RESISTANCE:1:480"
        actionBarMessage: "&0OoOooOOOOOoO &6Your eyes adjust to the darkness!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a candy carrot!"
      giveRewardItem: true
      rewardCommands: []
  "13":
    key: "MOONLIGHT_MUNCH"
    type: "CONSUMABLE"
    enabled: true
    givePerm: "item64.give.candy.moonlight_munch"
    usePerm: "item64.use.candy.moonlight_munch"
    item:
      mat: "MUSHROOM_STEW"
      name: "&8Moonlight Munch"
      lore:
        - "&78 mins of"
        - "&7  Night Vision"
        - "&7  Regeneration II"
        - "&7  Glowing II"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "NIGHT_VISION:0:480"
          - "REGENERATION:1:480"
          - "GLOWING:1:480"
        actionBarMessage: "&0oOOOooOoOooo &6You glow under the moonlight!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a moonlight munch!"
      giveRewardItem: true
      rewardCommands: []
  "14":
    key: "PHANTOM_FRUIT"
    type: "CONSUMABLE"
    enabled: true
    givePerm: "item64.give.candy.phantom_fruit"
    usePerm: "item64.use.candy.phantom_fruit"
    item:
      mat: "CHORUS_FRUIT"
      name: "&dPhantom Fruit"
      lore:
        - "&54 mins of"
        - "&5  Invisibility II"
        - "&5  Glowing"
        - "&5  Nausea"
        - "&5  Slow Falling VI"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "INVISIBILITY:1:240"
          - "GLOWING:0:240"
          - "CONFUSION:0:240"
          - "SLOW_FALLING:5:240"
        actionBarMessage: "&0oOOOooOoOooo &6You fade in and out of sight..."
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a phantom fruit!"
      giveRewardItem: true
      rewardCommands: []
  "15":
    key: "SPIDER_WEB_TAFFY"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.spider_web_taffy"
    usePerm: "item64.use.candy.spider_web_taffy"
    item:
      mat: "COBWEB"
      name: "&7Spider Web Taffy"
      lore:
        - "&f4 mins of"
        - "&f  Slowness VII"
        - "&f  Strength V"
        - "&f  Nausea"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "SLOW:6:240"
          - "INCREASE_DAMAGE:4:240"
          - "CONFUSION:0:240"
        actionBarMessage: "&0oOOOooOoOooo &6Sticky and strong!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found some spider web taffy!"
      giveRewardItem: true
      rewardCommands: []
  "16":
    key: "BROOMSTICK"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.BROOMSTICK"
    usePerm: "item64.use.candy.BROOMSTICK"
    item:
      mat: "BRUSH"
      name: "&bBroomstick"
      lore:
        - "&33 mins of"
        - "&3  Speed III"
        - "&3  Night Vision"
        - "&3  Slow Falling IV"
        - "&3  Jump Boost VII"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "SPEED:2:180"
          - "NIGHT_VISION:0:180"
          - "SLOW_FALLING:3:180"
          - "JUMP:6:180"
        actionBarMessage: "&0oOOOooOoOooo &6You feel as light as a feather!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a broomstick!"
      giveRewardItem: true
      rewardCommands: []
  "17":
    key: "GHOSTLY_GUMMY"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.ghostly_gummy"
    usePerm: "item64.use.candy.ghostly_gummy"
    item:
      mat: "SLIME_BALL"
      name: "&2Ghostly Gummy"
      lore:
        - "&a6 mins of"
        - "&a  Levitation III"
        - "&a  Invisibility"
        - "&a  Glowing"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "LEVITATION:2:360"
          - "INVISIBILITY:0:360"
          - "GLOWING:0:360"
        actionBarMessage: "&0oOOOooOoOaahh &6You get me so highhh!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a ghostly gummy!"
      giveRewardItem: true
      rewardCommands: []
  "18":
    key: "ZOMBIE_KUSH"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.zombie_kush"
    usePerm: "item64.use.candy.zombie_kush"
    item:
      mat: "GREEN_DYE"
      name: "&aZombie Kush"
      lore:
        - "&26 mins of"
        - "&2  Haste II"
        - "&2  Slowness"
        - "&2  Slow Falling"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "FAST_DIGGING:1:360"
          - "SLOW:0:360"
          - "SLOW_FALLING:0:360"
        actionBarMessage: "&0oOooOOooOoop &2The grass whispers as you move..."
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found some zombie kush!"
      giveRewardItem: true
      rewardCommands: []
  "19":
    key: "BEER"
    type: "CONSUMABLE"
    enabled: true
    givePerm: "item64.give.candy.beer"
    usePerm: "item64.use.candy.beer"
    item:
      mat: "HONEY_BOTTLE"
      name: "&fBeer"
      lore:
        - "&e8 mins of"
        - "&e  Slow Falling IV"
        - "&e  Slowness IV"
        - "&e  Nausea"
        - "&e  Regeneration IV"
        - "&e  Absorption II"
        - "&e  Mining Fatigue III"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "REGENERATION:3:480"
          - "ABSORPTION:1:480"
          - "SLOW_FALLING:3:480"
          - "SLOW:3:480"
          - "SLOW_DIGGING:2:480"
          - "CONFUSION:0:480"
        actionBarMessage: "&0oOOOOoooaaughhh &6You feel a bit tipsy!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found a bottle of beer!"
      giveRewardItem: true
      rewardCommands: []
  "20":
    key: "COKE"
    type: "USABLE"
    enabled: true
    givePerm: "item64.give.candy.coke"
    usePerm: "item64.use.candy.coke"
    item:
      mat: "SUGAR"
      name: "&9Coke"
      lore:
        - "&b6 mins of"
        - "&b  Speed V"
        - "&b  Jump Boost III"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
    usage:
      triggers:
        removeItemOnUse: true
        consoleCommands: []
        effects:
          - "SPEED:4:360"
          - "JUMP:2:360"
        actionBarMessage: "&0OOOOOOOOOOOOOO &6You're bursting with energy!!"
    blockEvent:
      rewardChance: 8.25
      rewardMessage: "&6You found some sugar!"
      giveRewardItem: true
      rewardCommands: []
  "21":
    key: "CRATE_KEY_REWARD"
    type: "NO_ITEM"
    enabled: false
    blockEvent:
      rewardChance: 0.0
      rewardMessage: "&6You found a crate key!"
      rewardCommands:
        - "crates givekey crate <player> 1"
```
