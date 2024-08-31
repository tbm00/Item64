# Item64
A spigot plugin that adds some custom items.

Created by tbm00 for play.mc64.wtf.

## Features
- **Explosive Bow** Shoots explosive arrows.
- **Lightning Gun** Shoots ender pearls that summon lightning.
- **Magic Wand** Casts random potion effects. Left click for an offensive effect, right click for a postive effect.
- **Survival-Friendly** Configurable ammo, hunger-costs, and cooldown timers.
- **Respect Claims & PVP** Hooks into GriefDefender and DeluxeCombat.

## Dependencies
- **Java 17+**: REQUIRED
- **Spigot 1.18.1+**: UNTESTED ON OLDER VERSIONS
- **GriefDefender**: OPTIONAL
- **DeluxeCombat**: OPTIONAL

## Commands & Permissions
#### Commands
- `/itm help` Display this command list
- `/itm give <itemKey> [player]` Spawn a custom item
#### Permissions
- `item64.help` Ability to display the command list *(Default: OP)*
- `item64.give.explosive_arrow` Ability to give explosive arrow item *(Default: OP)*
- `item64.give.lightning_pearl` Ability to give lightning pearl item *(Default: OP)*
- `item64.give.random_potion` Ability to give random potion pearl item *(Default: OP)*
- `item64.use.explosive_arrow` Ability to use explosive arrow item *(Default: everyone)*
- `item64.use.lightning_pearl` Ability to use lightning pearl item *(Default: everyone)*
- `item64.use.random_potion` Ability to use random potion item *(Default: everyone)*

## Config
```
hooks:
  GriefDefender:
    enabled: false
    ignoredClaims: []
      # - "9ae58371-3857-4ec8-9d19-f6d24c2f57c7"
  DeluxeCombat:
    enabled: false
itemEntries:
  enabled: true
  '1':
    enabled: true
    type: "EXPLOSIVE_ARROW" # Don't change
    key: "EXPLOSIVEBOW"
    cooldown: 4
    hunger: 6
    shotRandomness: 0.2
    extraDamage: 0.0
    ammoItem: "TNT"
    item: "BOW"
    name: "&6Explosive Bow"
    lore:
      - "&8&oRequires TNT and Arrows"
    hideEnchants: false
    enchantments:
      - "MENDING:1"
  '2':
    enabled: true
    type: "LIGHTNING_PEARL" # Don't change
    key: "LIGHTNINGGUN"
    cooldown: 4
    hunger: 4
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
  '3':
    enabled: true
    type: "RANDOM_POTION" # Don't change
    key: "MAGICWAND"
    cooldown: 4
    hunger: 4
    shotRandomness: 0.1
    extraDamage: 3.0
    ammoItem: "POTION"
    item: "BRUSH"
    name: "&6Magic Wand"
    lore:
      - "&8&oRequires Potion or Water Bottle"
    hideEnchants: true
    enchantments:
      - "MENDING:1"
  '4':
    enabled: true
    type: "BROKEN_ARROW" # Don't change
    key: "BROKENBOW"
    cooldown: 0
    hunger: 0
    shotRandomness: 5.0
    extraDamage: 0.0
    ammoItem: ""
    item: "BOW"
    name: "&cBroken Bow"
    lore: []
    hideEnchants: false
    enchantments:
      - "POWER:8"
```
