package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    private int defaultMax;
    private boolean only64;
    private boolean glow;
    private boolean loreEnabled;

    private List<String> loreFormat;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        storedKey = new NamespacedKey(this, "stored");

        defaultMax = getConfig().getInt("stack.default-max",128);
        only64 = getConfig().getBoolean("stack.only-64-stackable",true);

        glow = getConfig().getBoolean("stored.glowing",true);

        loreEnabled = getConfig().getBoolean("stored.lore.enabled",true);
        loreFormat = getConfig().getStringList("stored.lore.format");

        Bukkit.getPluginManager().registerEvents(this,this);
    }

    // ================= PICKUP =================

    @EventHandler
    public void onPickup(EntityPickupItemEvent e){

        if(!(e.getEntity() instanceof Player player)) return;

        ItemStack picked = e.getItem().getItemStack();
        if(picked == null) return;

        Material type = picked.getType();

        if(only64 && type.getMaxStackSize() != 64) return;

        Bukkit.getScheduler().runTaskLater(this, ()->{

            Inventory inv = player.getInventory();

            int total = 0;

            ItemStack storedItem = null;

            for(ItemStack item : inv.getContents()){

                if(item == null) continue;
                if(item.getType() != type) continue;

                total += item.getAmount();

                Integer stored = getStored(item);

                if(stored != null){

                    total += stored;
                    storedItem = item;
                }
            }

            if(total <= 64) return;

            if(total > defaultMax) return;

            int storedAmount = total - 64;

            if(storedItem == null){

                for(ItemStack item : inv.getContents()){

                    if(item == null) continue;
                    if(item.getType() != type) continue;

                    storedItem = item;
                    break;
                }
            }

            if(storedItem == null) return;

            storedItem.setAmount(64);

            applyStored(storedItem,storedAmount);

        },1L);
    }


    // ================= AUTO REFILL =================

    @EventHandler
    public void onPlace(BlockPlaceEvent e){

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack hand = p.getInventory().getItemInMainHand();

            if(hand == null) return;

            Integer stored = getStored(hand);

            if(stored == null) return;
            if(stored <= 0) return;

            if(hand.getAmount() < 64){

                hand.setAmount(hand.getAmount()+1);

                stored--;

                if(stored <= 0){

                    clearStored(hand);

                }else{

                    applyStored(hand,stored);

                }

            }

        },1L);

    }


    // ================= STORAGE =================

    private Integer getStored(ItemStack item){

        if(item == null) return null;
        if(!item.hasItemMeta()) return null;

        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void applyStored(ItemStack item,int amount){

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer()
                .set(storedKey, PersistentDataType.INTEGER,amount);

        if(glow){

            meta.addEnchant(Enchantment.UNBREAKING,1,true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        }

        if(loreEnabled){

            List<String> lore = new ArrayList<>();

            for(String line : loreFormat){

                line = line
                        .replace("{stored}",String.valueOf(amount))
                        .replace("{total}",String.valueOf(item.getAmount()+amount));

                lore.add(ChatColor.translateAlternateColorCodes('&',line));
            }

            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }

    private void clearStored(ItemStack item){

        if(!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().remove(storedKey);

        meta.removeEnchant(Enchantment.UNBREAKING);

        meta.setLore(null);

        item.setItemMeta(meta);
    }

}
