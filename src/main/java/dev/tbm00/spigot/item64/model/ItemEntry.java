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
    private String name;
    private String item;
    private Boolean glowing;
    private List<String> lore;

    public ItemEntry(JavaPlugin javaPlugin, String givePerm, String usePerm, String type,
                        String KEY, String name, String item, Boolean glowing, List<String> lore) {
        this.givePerm = givePerm;
        this.usePerm = usePerm;
        this.type = type;
        this.key = new NamespacedKey(javaPlugin, KEY);
        this.keyString = KEY;
        this.name = name;
        this.item = item;
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