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
- `/itm give <itemKey>` Spawn in a custom \<item\> in your inventory
- `/itm give <itemKey> <player>` Spawn in a custom \<item\> in player's inventory
#### Permissions
Each item has configurable permissions (in `config.yml`) that must be fulfiled for a player to use or spawn the item. The only hardcoded permission node is item64.help.
- `item64.help` Ability to display the command list *(Default: OP)*


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
    givePerm: "item64.give"
    usePerm: "item64.use"
    cooldown: 8
    hunger: 4
    name: "&6Explosive Bow"
    item: "BOW"
    ammoItem: "TNT"
    glowing: true
    lore:
      - "&8&oRequires TNT and Arrows"
  '2':
    enabled: true
    type: "LIGHTNING_PEARL" # Don't change
    key: "LIGHTNINGGUN"
    givePerm: "item64.give"
    usePerm: "item64.use"
    cooldown: 8
    hunger: 4
    name: "&6Lightning Gun"
    item: "ECHO_SHARD"
    ammoItem: "ENDER_PEARL"
    glowing: true
    lore:
      - "&8&oRequires Ender Pearl"
  '3':
    enabled: true
    type: "RANDOM_POTION" # Don't change
    key: "MAGICWAND"
    givePerm: "item64.give"
    usePerm: "item64.use"
    cooldown: 8
    hunger: 4
    name: "&6Magic Wand"
    item: "BRUSH"
    ammoItem: "POTION"
    glowing: true
    lore:
      - "&8&oRequires Potion or Water Bottle"
```
