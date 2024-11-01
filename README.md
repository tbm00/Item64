<p align="center">
  <img src="./icon.png" alt="Item64 Icon" width="400"/>
</p>

# Item64
A spigot plugin that adds custom items.

Created by tbm00 for play.mc64.wtf.


## Features
- **Flamethrower** Shoots flames.
- **Explosive Bow & Crossbow** Shoots explosive arrows.
- **Lightning Gun** Shoots ender pearls that summon lightning.
- **Magic Wand** Shoots random potion effects. Left click for an offensive potion, right click for a positive potion.
- **Survival-Friendly** Configurable ammo, hunger costs, money costs, and cooldown timers.
- **Very Configurable, Very Simple** Customize many aspects of each new item.
- **Respect Claims & PVP** Hooks into GriefDefender and DeluxeCombat.


## Dependencies
- **Java 17+**: REQUIRED
- **Spigot 1.18.1+**: UNTESTED ON OLDER VERSIONS
- **Vault**: OPTIONAL
- **DeluxeCombat**: OPTIONAL
- **GriefDefender**: OPTIONAL


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
If you're using the default config and haven't changed any usePerms or givePerms, then the following nodes will work for you:
- `item64.give.<key>` Ability to spawn a particular item *(Default: OP)*
- `item64.give.*` Ability to spawn all items *(Default: OP)*
- `item64.use.<key>` Ability to use a particular item *(Default: OP)*
- `item64.use.*` Ability to use all items *(Default: OP)*

## Item Entries
### Config Specification
<details><summary>`ID` (click to expand)</summary>
<details><summary>-- key: (STRING)</summary>

Required on *all ItemEntry types*. Listeners only apply to items with current key; pre-existing items will break if you change their key.
</details>
<details><summary>-- type: (STRING)</summary>

Required on *all ItemEntry types*. Must be `EXPLOSIVE_ARROW`, `LIGHTNING_PEARL`, `RANDOM_POTION`, `FLAME_PARTICLE`, or `CONSUMABLE`.
</details>
<details><summary>-- enabled: (BOOLEAN)</summary>

Required on *all ItemEntry types*.
</details>
<details><summary>-- givePerm: (STRING)</summary>

Required on *all ItemEntry types*.
</details>
<details><summary>-- usePerm: (STRING)</summary>

Required on *all ItemEntry types*.
</details>
<details><summary>-- usage:</summary>
<details><summary>-- -- moneyCost: (DOUBLE)</summary>

Applicable on *all ItemEntry types*. Requires Vault dependency.
</details>
<details><summary>-- -- hungerCost: (INTEGER)</summary>

Applicable on *all ItemEntry types*. In range 0-20.
</details>
<details><summary>-- -- cooldown: (INTEGER)</summary>

Applicable on *all ItemEntry types*. Time in seconds.
</details>
<details><summary>-- -- ammoItem:</summary>
<details><summary>-- -- -- mat: (STRING)</summary>

Applicable on *all ItemEntry types*.
</details>
<details><summary>-- -- -- removeAmmoItemOnUse: (BOOLEAN)</summary>

Applicable on *all ItemEntry types*.
</details>
</details>
<details><summary>-- -- projectile:</summary>
<details><summary>-- -- -- shotRandomness: (DOUBLE)</summary>

Applicable on `EXPLOSIVE_ARROW`, `LIGHTNING_PEARL`, `RANDOM_POTION`, & `FLAME_PARTICLE`. 
</details>
<details><summary>-- -- -- extraPlayerDamage: (DOUBLE)</summary>

Applicable on `EXPLOSIVE_ARROW`, `LIGHTNING_PEARL`, `RANDOM_POTION`, & `FLAME_PARTICLE`. 
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
<details><summary>-- -- consumable:</summary>
<details><summary>-- -- -- removeConsumableOnUse: (BOOLEAN)</summary>

Only applicable on `CONSUMABLE`.
</details>
<details><summary>-- -- -- consoleCommands: (LIST-STRING)</summary>

Only pplicable on `CONSUMABLE`. Use `<player>` for sender's username.
</details>
<details><summary>-- -- -- effects: (LIST-STRING)</summary>

Only pplicable on `CONSUMABLE`. Format: `effect-name:amplifier:time-in-seconds`.
</details>
</details>
</details>
<details><summary>-- item:</summary>
<details><summary>-- -- mat: (STRING)</summary>

Required on *all ItemEntry types*.
</details>
<details><summary>-- -- name: (STRING)</summary>

Applicable on *all ItemEntry types*.
</details>
<details><summary>-- -- lore: (LIST-STRING)</summary>

Applicable on *all ItemEntry types*.
</details>
<details><summary>-- -- hideEnchants: (BOOLEAN)</summary>

Applicable on *all ItemEntry types*.
</details>
<details><summary>-- -- enchantments: (LIST-STRING)</summary>

Applicable on *all ItemEntry types*. Format: `enchant-name:level`
</details>
</details>
</details>

### Types
**`EXPLOSIVE_ARROW`** 
- Summons an explosion on arrow impact.
- Applicable to bows and crossbows.
- Explosion deals a lot of damage + arrow damage if direct hit, so extraPlayerDamage is disabled by default.
- Has GriefDefender check that prevents block damage if user doesn't have builder trust in affected claims.
- Has GriefDefender check that prevents explosions entirely if pvp is toggled off in any affected claims.
- Has DeluxeCombat check that prevents explosions entirely if pvp is toggled off on any affected players.

**`LIGHTNING_PEARL`** 
- Shoots an ender pearl that summons a lightning strike on impact.
- Applicable to most items.
- Lightning doesn't deal much damage, so extraPlayerDamage is set high by default.
- Has GriefDefender check that prevents lightning entirely if pvp is toggled off in any affected claims.
- Has DeluxeCombat check that prevents lightning entirely if pvp is toggled off on any affected players.

**`RANDOM_POTION`** 
- Shoots a random splash potion.
- Applicable to most items.
- Most potions don't deal direct damage, so extraPlayerDamage is applied to left-clicked potion effects.
- Has GriefDefender check that prevents potion shots entirely if pvp is toggled off in any affected claims.
- Has DeluxeCombat check that prevents potion shots entirely if pvp is toggled off on any affected players.

**`FLAME_PARTICLE`** 
- Shoots flame particles that make fires.
- Applicable to most items.
- Particles don't deal any damage, so extraPlayerDamage is enabled by default.
- Cooldown works differently; it's the maximum number of seconds a player can repetitively use the item.
- Has GriefDefender check that prevents flames entirely if pvp is toggled off in any affected claims.
- Has DeluxeCombat check that prevents flames entirely if pvp is toggled off on any affected players.

**`CONSUMABLE`** 
- Runs commands and/or gives potion effects on item consumption.
- Applicable to most food, potions, milk, etc.
- Doesn't have any GriefDefender or DeluxeCombat checks.


## Default Config
```
# Item64 v0.2.8-beta by @tbm00
# https://github.com/tbm00/Item64

hooks:
  Vault:
    enabled: false
  GriefDefender:
    enabled: false
    ignoredClaims: []
  DeluxeCombat:
    enabled: false
    anchorExplosionPvpCheck: false

stopBlockPlace:
  - "REDSTONE_TORCH"

itemEntries:
  enabled: true
  '1':
    key: "FLAMETHROWER"
    type: "FLAME_PARTICLE" 
    enabled: true
    givePerm: "item64.give.flamethrower"
    usePerm: "item64.use.flamethrower"
    usage:
      moneyCost: 0.00
      hungerCost: 1
      cooldown: 0
      ammoItem:
        mat: "COAL"
        removeAmmoItem: true
      projectile:
        shotRandomness: 0.42
        extraPlayerDamage: 3.0
    item:
      mat: "REDSTONE_TORCH"
      name: "&6Flamethrower"
      lore:
        - "&8&oRequires Coal"
      hideEnchants: true
      enchantments:
        - "MENDING:1"
  '2':
    key: "EXPLOSIVE_BOW"
    type: "EXPLOSIVE_ARROW"
    enabled: true
    givePerm: "item64.give.explosive_bow"
    usePerm: "item64.use.explosive_bow"
    usage:
      moneyCost: 0.00
      hungerCost: 0
      cooldown: 0
      ammoItem:
        mat: "BEDROCK"
        removeAmmoItem: true
      projectile:
        shotRandomness: 0.32
        extraPlayerDamage: 0.0
        explosiveArrow:
          power: 96
    item:
      mat:  "BOW"
      name: "&6Explosive Bow"
      lore:
        - "&8&oRequires Bedrock and Arrows"
      hideEnchants: false
      enchantments:
        - "UNBREAKING:5"
        - "MENDING:1"
  '3':
    key: "EXPLOSIVE_CROSSBOW"
    type: "EXPLOSIVE_ARROW"
    enabled: true
    givePerm: "item64.give.explosive_crossbow"
    usePerm: "item64.use.explosive_crossbow"
    usage:
      moneyCost: 0.00
      hungerCost: 5
      cooldown: 2
      ammoItem:
        mat: "TNT"
        removeAmmoItem: true
      projectile:
        shotRandomness: 0.2
        extraPlayerDamage: 0.0
        explosiveArrow:
          power: 4
    item:
      mat:  "CROSSBOW"
      name: "&6Explosive Crossbow"
      lore:
        - "&8&oRequires TNT and Arrows"
      hideEnchants: false
      enchantments:
        - "QUICK_CHARGE:5"
        - "MENDING:1"
  '4':
    key: "LIGHTNING_GUN"
    type: "LIGHTNING_PEARL"
    enabled: true
    givePerm: "item64.give.lightning_gun"
    usePerm: "item64.use.lightning_gun"
    usage:
      moneyCost: 0.00
      hungerCost: 2
      cooldown: 2
      ammoItem:
        mat: "ENDER_PEARL"
        removeAmmoItem: true
      projectile:
        shotRandomness: 0.22
        extraPlayerDamage: 5.0
    item:
      mat: "ECHO_SHARD"
      name: "&6Lightning Gun"
      lore:
        - "&8&oRequires Ender Pearls"
      hideEnchants: true
      enchantments:
        - "MENDING:1"
  '5':
    key: "MAGIC_WAND"
    type: "RANDOM_POTION" 
    enabled: true
    givePerm: "item64.give.magic_wand"
    usePerm: "item64.use.magic_wand"
    usage:
      moneyCost: 0.00
      hungerCost: 2
      cooldown: 2
      ammoItem:
        mat: "POTION"
        removeAmmoItem: true
      projectile:
        shotRandomness: 0.22
        extraPlayerDamage: 6.0
        randomPotion:
          rightClickEffects:
            - "INCREASE_DAMAGE:1:30"
            - "HEAL:1:30"
            - "JUMP:1:30"
            - "SPEED:1:30"
            - "REGENERATION:1:30"
            - "FIRE_RESISTANCE:1:30"
            - "NIGHT_VISION:1:30"
            - "INVISIBILITY:1:30"
            - "ABSORPTION:1:30"
            - "SATURATION:1:30"
            - "SLOW_FALLING:1:30"
          leftClickEffects:
            - "SLOW:1:30"
            - "HARM:1:30"
            - "CONFUSION:1:30"
            - "BLINDNESS:1:30"
            - "HUNGER:1:30"
            - "WEAKNESS:1:30"
            - "POISON:1:30"
            - "LEVITATION:1:30"
            - "GLOWING:1:30"
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
  '6':
    key: "HEALING_HONEY"
    type: "CONSUMABLE" 
    enabled: true
    givePerm: "item64.give.healing_honey"
    usePerm: "item64.use.healing_honey"
    usage:
      moneyCost: 0.00
      hungerCost: 0
      cooldown: 0
      ammoItem:
        mat: ""
        removeAmmoItem: true
      consumable:
        removeConsumableOnUse: true
        consoleCommands:
          - "itm heal <player> -fx"
        effects: []
    item:
      mat: "HONEY_BOTTLE"
      name: "&6Healing Honey"
      lore:
        - "&eRestores health, hunger,"
        - "&eand removes potions effects"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
  '7':
    key: "SUPERAPPLE"
    type: "CONSUMABLE" 
    enabled: true
    givePerm: "item64.give.superapple"
    usePerm: "item64.use.superapple"
    usage:
      moneyCost: 0.00
      hungerCost: 0
      cooldown: 0
      ammoItem:
        mat: ""
        removeAmmoItem: true
      consumable:
        removeConsumableOnUse: true
        consoleCommands:
          - "itm heal <player>"
        effects:
          - "REGENERATION:1:1800"
          - "ABSORPTION:3:1800"
          - "DAMAGE_RESISTANCE:3:1800"
          - "FIRE_RESISTANCE:0:1800"
          - "INCREASE_DAMAGE:1:1800"
          - "FAST_DIGGING:1:60"
          - "JUMP:1:1800"
          - "SPEED:0:1800"
    item:
      mat: "ENCHANTED_GOLDEN_APPLE"
      name: "&6Superapple"
      lore:
        - "&e30 mins of"
        - "&e  Regeneration II"
        - "&e  Absorption IV"
        - "&e  Resistance IV"
        - "&e  Fire Resistance I"
        - "&e  Strength II"
        - "&e  Haste II"
        - "&e  Jump Boost II"
        - "&e  Speed I"
      hideEnchants: true
      enchantments:
        - "UNBREAKING:1"
```
