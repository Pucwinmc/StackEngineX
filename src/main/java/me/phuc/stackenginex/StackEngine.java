package me.phuc.stackenginex;

import io.papermc.paper.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StackEngine extends JavaPlugin implements Listener {

    private final int MAX_STACK = 128;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngine chạy chuẩn Paper 1.21.4!");
    }

    // ================= PICKUP =================
    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent e) {

        Player player = e.getPlayer();
        Item entity = e.getItem();
        ItemStack ground = entity.getItemStack();

        if (ground.getType().isAir()) return;

        int amountLeft = ground.getAmount();

        for (ItemStack invItem : player.getInventory().getContents()) {

            if (invItem == null) continue;
            if (!invItem.isSimilar(ground)) continue;

            int stored = getStored(invItem);
            int total = invItem.getAmount() + stored;

            if (total >= MAX_STACK) continue;

            int space = MAX_STACK - total;
            int move = Math.min(space, amountLeft);

            total += move;
            amountLeft -= move;

            int vanilla = Math.min(64, total);
            int newStored = total - vanilla;

            invItem.setAmount(vanilla);
            setStored(invItem, newStored);
            addGlow(invItem);

            if (amountLeft <= 0) break;
        }

        if (amountLeft <= 0) {
            entity.remove();
            e.setCancelled(true);
        } else {
            ground.setAmount(amountLeft);
        }
    }

    // ================= REFILL =================
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {

        ItemStack item = e.getItemInHand();

        Bukkit.getScheduler().runTaskLater(this, () -> {

            if (item.getAmount() == 0) {

                int stored = getStored(item);
                if (stored <= 0) return;

                int refill = Math.min(64, stored);
                int newStored = stored - refill;

                item.setAmount(refill);
                setStored(item, newStored);
                addGlow(item);
            }

        }, 1L);
    }

    // ================= UNSTACK TO CHEST =================
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getCurrentItem() == null) return;
        if (e.getClickedInventory() == null) return;

        Inventory inv = e.getClickedInventory();

        if (inv.getHolder() instanceof Player) return;

        ItemStack item = e.getCurrentItem();
        int stored = getStored(item);

        if (stored <= 0) return;

        int move = Math.min(64, stored);
        int newStored = stored - move;

        ItemStack extra = new ItemStack(item.getType(), move);
        inv.addItem(extra);

        setStored(item, newStored);
    }

    // ================= STORAGE =================
    private int getStored(ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;

        for (String line : meta.getLore()) {
            if (line.startsWith("Stored: ")) {
                return Integer.parseInt(line.replace("Stored: ", ""));
            }
        }
        return 0;
    }

    private void setStored(ItemStack item, int amount) {

        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (amount > 0) {
            lore.add("Stored: " + amount);
        }

        meta.setLore(lore.isEmpty() ? null : lore);
        item.setItemMeta(meta);
    }

    private void addGlow(ItemStack item) {

        ItemMeta meta = item.getItemMeta();

        if (!meta.hasEnchant(Enchantment.UNBREAKING)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            item.setItemMeta(meta);
        }
    }
}
