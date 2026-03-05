package me.phuc.stackenginex;

import com.destroystokyo.paper.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("StackEngine chạy chuẩn Paper 1.21.4!");
    }

    // ===== PICKUP EVENT (PAPER 1.21.4) =====
    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent e) {

        Player player = e.getPlayer();
        Item entity = e.getItem();
        ItemStack groundItem = entity.getItemStack();

        if (groundItem.getType().isAir()) return;

        Inventory inv = player.getInventory();

        int amountToPickup = groundItem.getAmount();
        int maxVanilla = groundItem.getMaxStackSize(); // 64

        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            if (!item.isSimilar(groundItem)) continue;

            int stored = getStored(item);
            int total = item.getAmount() + stored;

            if (total >= 128) continue;

            int free = 128 - total;
            int move = Math.min(free, amountToPickup);

            total += move;
            amountToPickup -= move;

            int newAmount = Math.min(total, maxVanilla);
            int newStored = total - newAmount;

            item.setAmount(newAmount);
            setStored(item, newStored);

            addGlow(item);

            if (amountToPickup <= 0) break;
        }

        if (amountToPickup <= 0) {
            entity.remove();
            e.setCancelled(true);
        } else {
            groundItem.setAmount(amountToPickup);
        }
    }

    // ===== REFILL WHEN PLACE =====
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item == null) return;

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (item.getAmount() == 0) {
                int stored = getStored(item);
                if (stored > 0) {

                    int refill = Math.min(64, stored);
                    int newStored = stored - refill;

                    item.setAmount(refill);
                    setStored(item, newStored);
                    addGlow(item);
                }
            }
        }, 1L);
    }

    // ===== UNSTACK INTO CHEST =====
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getCurrentItem() == null) return;

        Inventory clicked = e.getClickedInventory();
        if (clicked == null) return;

        if (clicked.getHolder() instanceof Player) return; // chỉ xử lý chest

        ItemStack item = e.getCurrentItem();
        int stored = getStored(item);

        if (stored <= 0) return;

        int move = Math.min(64, stored);
        int newStored = stored - move;

        ItemStack extra = new ItemStack(item.getType(), move);
        clicked.addItem(extra);

        setStored(item, newStored);
    }

    // ===== LORE STORAGE =====
    private int getStored(ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;

        for (String line : meta.getLore()) {
            if (line.contains("Stored:")) {
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
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
    }
}
