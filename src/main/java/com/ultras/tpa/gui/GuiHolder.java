package com.ultras.tpa.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Marks an inventory as an ULTRAS menu and stores the click action of each slot. */
public final class GuiHolder implements InventoryHolder {

    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;
    private UUID requestId;

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void reset() {
        actions.clear();
        inventory.clear();
    }

    public void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    /** @return true if the slot had an action */
    public boolean handle(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action == null) {
            return false;
        }
        action.accept(event);
        return true;
    }

    public UUID requestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
}
