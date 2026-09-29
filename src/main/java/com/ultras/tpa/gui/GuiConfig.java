package com.ultras.tpa.gui;

import com.ultras.tpa.UltrasTpa;
import com.ultras.tpa.utils.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One guis/*.yml file: layout, filler and item construction. */
public final class GuiConfig {

    private final UltrasTpa plugin;
    private final String id;
    private final YamlConfiguration yaml;

    public GuiConfig(UltrasTpa plugin, String id, YamlConfiguration yaml) {
        this.plugin = plugin;
        this.id = id;
        this.yaml = yaml;
    }

    public int rows() {
        return Math.max(1, Math.min(6, yaml.getInt("rows", 3)));
    }

    public Component title(Player viewer, TagResolver... resolvers) {
        String custom = yaml.getString("title");
        return Text.parse(custom != null ? custom : plugin.lang().raw(viewer, "gui." + id + ".title"), resolvers);
    }

    public boolean has(String key) {
        return yaml.isConfigurationSection("items." + key);
    }

    public int slot(String key) {
        return yaml.getInt("items." + key + ".slot", -1);
    }

    /** Reads a list such as ["0-44"] or [10, 11, 12] from the "layout" section. */
    public List<Integer> slots(String key, int fallbackFrom, int fallbackTo) {
        List<Integer> slots = new ArrayList<>();
        for (String part : yaml.getStringList("layout." + key)) {
            String[] range = part.trim().split("-");
            try {
                int from = Integer.parseInt(range[0].trim());
                int to = range.length > 1 ? Integer.parseInt(range[1].trim()) : from;
                for (int slot = from; slot <= to; slot++) {
                    slots.add(slot);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid slot '" + part + "' in guis/" + id + ".yml");
            }
        }
        if (slots.isEmpty()) {
            for (int slot = fallbackFrom; slot <= fallbackTo; slot++) {
                slots.add(slot);
            }
        }
        return slots;
    }

    public void applyFiller(Inventory inventory) {
        ConfigurationSection filler = yaml.getConfigurationSection("filler");
        if (filler == null || !filler.getBoolean("enabled", true)) {
            return;
        }
        ItemStack item = new ItemStack(material(filler.getString("material"), Material.BLACK_STAINED_GLASS_PANE));
        ItemMeta meta = item.getItemMeta();
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    /** Places the item {@code key} at the slot defined in its yml section. */
    public void put(GuiHolder holder, String key, Player viewer, OfflinePlayer skull, Boolean state,
                    List<Component> extraLore, Consumer<InventoryClickEvent> action, TagResolver... resolvers) {
        if (has(key)) {
            putAt(slot(key), holder, key, viewer, skull, state, extraLore, action, resolvers);
        }
    }

    /** Places the item {@code key} at an explicit slot (used for paged content). */
    public void putAt(int slot, GuiHolder holder, String key, Player viewer, OfflinePlayer skull, Boolean state,
                      List<Component> extraLore, Consumer<InventoryClickEvent> action, TagResolver... resolvers) {
        ConfigurationSection section = yaml.getConfigurationSection("items." + key);
        if (section != null) {
            holder.set(slot, build(section, key, viewer, skull, state, extraLore, resolvers), action);
        }
    }

    private ItemStack build(ConfigurationSection section, String key, Player viewer, OfflinePlayer skull,
                            Boolean state, List<Component> extraLore, TagResolver... resolvers) {
        ConfigurationSection variant = state == null ? null : section.getConfigurationSection(state ? "on" : "off");
        String materialName = variant != null && variant.contains("material")
                ? variant.getString("material") : section.getString("material");
        boolean glow = variant != null && variant.contains("glow")
                ? variant.getBoolean("glow") : section.getBoolean("glow", false);

        ItemStack item = new ItemStack(material(materialName, Material.STONE), Math.max(1, section.getInt("amount", 1)));
        ItemMeta meta = item.getItemMeta();

        String base = "gui." + id + "." + key;
        String name = section.contains("name") ? section.getString("name") : plugin.lang().raw(viewer, base + ".name");
        List<String> loreLines = section.isList("lore") ? section.getStringList("lore") : plugin.lang().rawList(viewer, base + ".lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Text.parse(line, resolvers));
        }
        if (extraLore != null) {
            lore.addAll(extraLore);
        }
        meta.displayName(Text.parse(name, resolvers));
        meta.lore(lore.isEmpty() ? null : lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        int modelData = section.getInt("custom-model-data", 0);
        if (modelData > 0) {
            meta.setCustomModelData(modelData);
        }
        ConfigurationSection enchants = section.getConfigurationSection("enchants");
        if (enchants != null) {
            for (String enchantKey : enchants.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantKey.toLowerCase(java.util.Locale.ROOT)));
                if (enchantment != null) {
                    meta.addEnchant(enchantment, enchants.getInt(enchantKey, 1), true);
                }
            }
        }
        if (skull != null && meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(skull);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Material material(String name, Material fallback) {
        Material material = name == null ? null : Material.matchMaterial(name);
        return material != null ? material : fallback;
    }
}
