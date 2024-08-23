package dev.tbm00.spigot.item64.model;

import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemEntry {
    private String givePerm;
    private String usePerm;
    private String type;
    private NamespacedKey key;
    private String keyString;
    private int cooldown;
    private int hunger;
    private String name;
    private String item;
    private String ammoItem;
    private Boolean glowing;
    private List<String> lore;

    public ItemEntry(JavaPlugin javaPlugin, String givePerm, String usePerm, String type, String KEY,
                        int cooldown, int hunger, String name, String item, String ammoItem, Boolean glowing, List<String> lore) {
        this.givePerm = givePerm;
        this.usePerm = usePerm;
        this.type = type;
        this.key = new NamespacedKey(javaPlugin, KEY);
        this.keyString = KEY;
        this.cooldown = cooldown;
        this.hunger = hunger;
        this.name = name;
        this.item = item;
        this.ammoItem = ammoItem;
        this.glowing = glowing;
        this.lore = lore;
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

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public int getHunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getAmmoItem() {
        return ammoItem;
    }

    public void setAmmoItem(String ammoItem) {
        this.ammoItem = ammoItem;
    }

    public Boolean getGlowing() {
        return glowing;
    }

    public void setGlowing(Boolean glowing) {
        this.glowing = glowing;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }
}