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
    private int maxStack;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        storedKey = new NamespacedKey(this,"stored");

        maxStack = getConfig().getInt("stack.max",128);

        Bukkit.getPluginManager().registerEvents(this,this);
    }

    // ================= PICKUP =================

    @EventHandler
    public void onPickup(EntityPickupItemEvent e){

        if(!(e.getEntity() instanceof Player player)) return;

        Material type = e.getItem().getItemStack().getType();

        if(type.getMaxStackSize() != 64) return;

        Bukkit.getScheduler().runTaskLater(this,()->{

            compress(player,type);

        },2L);
    }

    // ================= STACK ENGINE =================

    private void compress(Player player,Material type){

        Inventory inv = player.getInventory();

        int total = 0;

        List<ItemStack> items = new ArrayList<>();

        for(ItemStack item : inv.getContents()){

            if(item == null) continue;
            if(item.getType() != type) continue;

            items.add(item);

            total += item.getAmount();

            Integer stored = getStored(item);

            if(stored != null){
                total += stored;
            }
        }

        // chưa đủ để nén
        if(total <= 64) return;

        // đã đạt max → để vanilla xử lý
        if(total >= maxStack) return;

        int stored = total - 64;

        // remove từng stack
        for(ItemStack i : items){
            inv.remove(i);
        }

        ItemStack result = new ItemStack(type,64);

        applyStored(result,stored);

        inv.addItem(result);
    }

    // ================= PLACE =================

    @EventHandler
    public void onPlace(BlockPlaceEvent e){

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack hand = p.getInventory().getItemInMainHand();

            if(hand == null) return;

            Integer stored = getStored(hand);

            if(stored == null) return;

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
                .set(storedKey,PersistentDataType.INTEGER,amount);

        meta.addEnchant(Enchantment.UNBREAKING,1,true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY+"Stored: "+ChatColor.YELLOW+amount);
        lore.add(ChatColor.GRAY+"Total: "+ChatColor.WHITE+(item.getAmount()+amount));

        meta.setLore(lore);

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
