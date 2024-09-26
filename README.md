<p align="center">
  <img src="./icon.png" alt="Item64 Icon" width="400"/>
</p>

# Item64
A spigot plugin that adds some custom items.

Created by tbm00 for play.mc64.wtf.

## Features
- **Flamethrower** Shoots flames.
- **Explosive Bow & Crossbow** Shoots explosive arrows.
- **Lightning Gun** Shoots ender pearls that summon lightning.
- **Magic Wand** Shoots random potion effects. Left click for an offensive potion, right click for a positive potion.
- **Survival-Friendly** Configurable ammo, hunger costs, money costs, and cooldown timers.
- **Highly Configurable** Customize many aspects of each new item.
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
- `/itm heal [player]` Heal a player (health, hunger, potion effects)

### Permissions
Each item has configurable permissions (in `config.yml`) that must be fulfilled for a player to use or spawn the item. The only hardcoded permissions are:
- `item64.help` Ability to display the command list *(Default: OP)*
- `item64.heal` Ability to heal a player *(Default: OP)*

## Configuration Notes
- MoneyCost requires the Vault hook.
- HungerCost (0-20) is removed from the player on use.
- Cooldown is seconds until next use.
- If ammoItem is not disabled (`""`), the plugin will require and remove 1 of that item from the player on use.
- ExtraDamage is only applied to players (using `org.bukkit.entity.Damageable.damage(double, Entity)`).
- If enabled, the DeluxeCombat hook will check if all surrounding players have PVP enabled (and no newbie protection) before permitting any damage.
- If enabled, the GriefDefender hook will check if PVP is enabled in the claim where the projectile lands, and in the claim where the shooter is located, before permitting any damage. It will also require the shooter to have builder trust in the affected claim for arrow explosions to do block damage.

### Custom Item Types

**`FLAME_PARTICLE`** 
- Shoots flame particles that make fires.
- Particles don't deal any damage, so extraDamage is enabled by default.
- Cooldown works differently; it's the maximum number of seconds a player can repetitively use the item.
- Applicable to most items.

**`EXPLOSIVE_ARROW`** 
- Summons an explosion on arrow impact.
- Explosion deals a lot of damage + arrow damage if direct hit, so extraDamage is disabled by default.
- Applicable to bows and crossbows.

**`LIGHTNING_PEARL`** 
- Shoots an ender pearl that summons a lightning strike on impact.
- Lightning doesn't deal much damage, so extraDamage is set high by default.
- Applicable to most items.

**`RANDOM_POTION`** 
- Shoots a random splash potion.
- Most potions don't deal direct damage, so extraDamage is applied to negative (left-clicked) potion effects.
- Positive potion effects are shot from right clicks: 
    - `INCREASE_DAMAGE (STRENGTH)`, `HEAL (INSTANT_HEALTH)`, `JUMP (JUMP_BOOST)`, `SPEED`, `REGENERATION`, `FIRE_RESISTANCE`, `NIGHT_VISION`, `INVISIBILITY`, `ABSORPTION`, `SATURATION`, `SLOW_FALLING`
- Negative potion effects are shot from left clicks:
    - `SLOW (SLOWNESS)`, `HARM (INSTANT_DAMAGE)`, `CONFUSION (NAUSEA)`, `BLINDNESS`, `HUNGER`, `WEAKNESS`, `POISON`, `LEVITATION`, `GLOWING`
- Applicable to most items.

**`CONSUME_COMMAND`** 
- Runs commands on item consumption.
- ExtraDamage & shotRandomness are not applicable.
- Has no DeluxeCombat or GriefDefender checks.
- Requires extra configuration options: consoleCommands & removeConsumedItem 
    - ConsoleCommands are run when a player consumes the item (`<player>` is replaced with the consumer's username).
    - RemoveConsumedItem removes the custom item that is consumed (not the ammoItem) if `true`.
- Applicable to most food, potions, milk, etc.

**`CONSUME_EFFECTS`** 
- Applies potion effects & fully heals player on on item consumption.
- ExtraDamage & shotRandomness are not applicable.
- Has no DeluxeCombat or GriefDefender checks.
- Requires extra configuration option: removeConsumedItem 
    - RemoveConsumedItem removes the custom item that is consumed (not the ammoItem) if `true`.
- Applicable to most food, potions, milk, etc.

## Default Config
```
# Item64 v0.2.5-beta by @tbm00

hooks:
  Vault:
    enabled: false
  GriefDefender:
    enabled: false
    ignoredClaims: []
      # - "9ae58371-3857-4ec8-9d19-f6d24c2f57c7"
  DeluxeCombat:
    enabled: false
    anchorExplosionPvpCheck: false # Temporary fix until DC dev fixes anchor explosions

stopBlockPlace:
- "REDSTONE_TORCH"

itemEntries:
  enabled: true
  '1':
    enabled: true
    type: "FLAME_PARTICLE" # Don't change
    key: "FLAMETHROWER"
    givePerm: "item64.give.flamethrower"
    usePerm: "item64.use.flamethrower"
    moneyCost: 0.00
    hungerCost: 1
    cooldown: 0
    shotRandomness: 0.3
    extraDamage: 2.0
    ammoItem: "COAL"
    item: "REDSTONE_TORCH"
    name: "&6Flamethrower"
    lore:
      - "&8&oRequires Coal"
    hideEnchants: true
    enchantments:
      - "MENDING:1"
  '2':
    enabled: true
    type: "EXPLOSIVE_ARROW" # Don't change
    key: "EXPLOSIVE_CROSSBOW"
    givePerm: "item64.give.explosive_crossbow"
    usePerm: "item64.use.explosive_crossbow"
    moneyCost: 0.00
    hungerCost: 6
    cooldown: 1
    shotRandomness: 0.2
    extraDamage: 0.0
    ammoItem: "TNT"
    item: "CROSSBOW"
    name: "&6Explosive Crossbow"
    lore:
      - "&8&oRequires TNT and Arrows"
    hideEnchants: false
    enchantments:
      - "QUICK_CHARGE:4"
      - "MENDING:1"
  '3':
    enabled: true
    type: "LIGHTNING_PEARL" # Don't change
    key: "LIGHTNING_GUN"
    givePerm: "item64.give.lightning_gun"
    usePerm: "item64.use.lightning_gun"
    moneyCost: 0.00
    hungerCost: 4
    cooldown: 4
    shotRandomness: 0.2
    extraDamage: 7.0
    ammoItem: "ENDER_PEARL"
    item: "ECHO_SHARD"
    name: "&6Lightning Gun"
    lore:
      - "&8&oRequires Ender Pearl"
    hideEnchants: true
    enchantments:
      - "MENDING:1"
  '4':
    enabled: true
    type: "RANDOM_POTION" # Don't change
    key: "MAGIC_WAND"
    givePerm: "item64.give.magic_wand"
    usePerm: "item64.use.magic_wand"
    moneyCost: 0.00
    hungerCost: 4
    cooldown: 4
    shotRandomness: 0.1
    extraDamage: 2.0
    ammoItem: "POTION"
    item: "BRUSH"
    name: "&6Magic Wand"
    lore:
      - "&8&oRequires Potion or Water Bottle"
    hideEnchants: true
    enchantments:
      - "MENDING:1"
  '5':
    enabled: true
    type: "CONSUME_COMMANDS" # Don't change
    key: "HEALING_HONEY"
    givePerm: "item64.give.healing_honey"
    usePerm: "item64.use.healing_honey"
    moneyCost: 0.00
    hungerCost: 0
    cooldown: 60
    ammoItem: ""
    item: "HONEY_BOTTLE"
    name: "&6Healing Honey"
    lore:
      - "&eRestores health, hunger,"
      - "&eand removes potions effects"
    hideEnchants: true
    enchantments:
      - "UNBREAKING:1"
    removeConsumedItem: true
    consoleCommands:
      - "itm heal <player>"
  '6':
    enabled: true
    type: "CONSUME_EFFECTS" # Don't change
    key: "SUPERAPPLE"
    givePerm: "item64.give.superapple"
    usePerm: "item64.use.superapple"
    moneyCost: 0.00
    hungerCost: 0
    cooldown: 60
    ammoItem: ""
    item: "ENCHANTED_GOLDEN_APPLE"
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
    removeConsumedItem: true
```
