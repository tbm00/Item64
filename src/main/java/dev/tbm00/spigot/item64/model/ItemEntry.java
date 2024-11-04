package dev.tbm00.spigot.item64.model;

import java.util.List;

import org.bukkit.NamespacedKey;

import dev.tbm00.spigot.item64.Item64;

public class ItemEntry {
    private int id;
    private String givePerm;
    private String usePerm;
    private String type;
    private NamespacedKey key;
    private String keyString;
    private double money;
    private int cooldown;
    private int hunger;
    private boolean removeItem;
    private double random;
    private double damage;
    private String ammoItem;
    private boolean removeAmmo;
    private String material;
    private String name;
    private List<String> lore;
    private boolean hideEnchants;
    private List<String> enchants;
    private List<String> commands;
    private String message;
    private List<String> effects;
    private List<String> rEffects;
    private List<String> lEffects;
    private double power;
    private double rewardChance;
    private String rewardMessage;
    private boolean giveItem;
    private List<String> rewardCommands;
    
    public ItemEntry(Item64 item64, int id, String givePerm, String usePerm, String type, String KEY, double money, int hunger, 
                        int cooldown, double random, double damage, String ammoItem, boolean removeAmmo, String material, String name, List<String> lore, 
                        boolean hideEnchants, List<String> enchants, boolean removeItem, List<String> commands, String message, List<String> effects, List<String> rEffects, 
                        List<String> lEffects, double power, double rewardChance, String rewardMessage, boolean giveItem, List<String> rewardCommands) {
        this.id = id;
        this.givePerm = givePerm;
        this.usePerm = usePerm;
        this.type = type;
        this.key = new NamespacedKey(item64, KEY);
        this.keyString = KEY;
        this.money = money;
        this.hunger = hunger;
        this.cooldown = cooldown;
        this.random = random;
        this.damage = damage;
        this.ammoItem = ammoItem;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.hideEnchants = hideEnchants;
        this.enchants = enchants;
        this.removeItem = removeItem;
        this.commands = commands;
        this.message = message;
        this.effects = effects;
        this.rEffects = rEffects;
        this.lEffects = lEffects;
        this.power = power;
        this.rewardChance = rewardChance;
        this.rewardMessage = rewardMessage;
        this.giveItem = giveItem;
        this.rewardCommands = rewardCommands;
    }
    
    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getGivePerm() {
        return givePerm;
    }

    public void setGivePerm(String givePerm) {
        this.givePerm = givePerm;
    }

    public String getUsePerm() {
        return usePerm;
    }

    public void setUsePerm(String usePerm) {
        this.usePerm = usePerm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public String getKeyString() {
        return keyString;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getHunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public double getRandom() {
        return random;
    }

    public void setRandom(int random) {
        this.random = random;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public String getAmmoItem() {
        return ammoItem;
    }

    public void setAmmoItem(String ammoItem) {
        this.ammoItem = ammoItem;
    }

    public boolean getRemoveAmmo() {
        return removeAmmo;
    }

    public void setRemoveAmmo(boolean removeAmmo) {
        this.removeAmmo = removeAmmo;
    }


    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public boolean getHideEnchants() {
        return hideEnchants;
    }

    public void setHideEnchants(boolean hideEnchants) {
        this.hideEnchants = hideEnchants;
    }

    public List<String> getEnchants() {
        return enchants;
    }

    public void setEnchants(List<String> enchants) {
        this.enchants = enchants;
    }

    public boolean getRemoveItem() {
        return removeItem;
    }

    public void setRemoveItem(boolean removeItem) {
        this.removeItem = removeItem;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getEffects() {
        return effects;
    }

    public void setEffects(List<String> effects) {
        this.effects = effects;
    }

    public List<String> getREffects() {
        return rEffects;
    }

    public void setREffects(List<String> rEffects) {
        this.rEffects = rEffects;
    }

    public List<String> getLEffects() {
        return lEffects;
    }

    public void setLEffects(List<String> lEffects) {
        this.lEffects = lEffects;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public double getRewardChance() {
        return rewardChance;
    }

    public void setRewardChance(double rewardChance) {
        this.rewardChance = rewardChance;
    }

    public String getRewardMessage() {
        return rewardMessage;
    }

    public void setRewardMessage(String rewardMessage) {
        this.rewardMessage = rewardMessage;
    }

    public boolean getGiveItem() {
        return giveItem;
    }

    public void setGiveItem(boolean giveItem) {
        this.giveItem = giveItem;
    }

    public List<String> getRewardCommands() {
        return rewardCommands;
    }

    public void setRewardCommands(List<String> rewardCommands) {
        this.rewardCommands = rewardCommands;
    }
}