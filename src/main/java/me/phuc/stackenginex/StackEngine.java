package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    @Override
    public void onEnable() {

        storedKey = new NamespacedKey(this,"stored");

        Bukkit.getPluginManager().registerEvents(this,this);

        startActionbar();
    }

    // ================= PICKUP =================

    @EventHandler
    public void onPickup(EntityPickupItemEvent e){

        if(!(e.getEntity() instanceof Player player)) return;

        ItemStack picked = e.getItem().getItemStack();
        if(picked==null) return;

        Material type = picked.getType();

        Bukkit.getScheduler().runTaskLater(this,()->{

            Inventory inv = player.getInventory();

            ItemStack stackItem = null;

            for(ItemStack item:inv.getContents()){

                if(item==null) continue;
                if(item.getType()!=type) continue;

                stackItem=item;
                break;
            }

            if(stackItem==null) return;

            int amount = picked.getAmount();

            Integer stored = getStored(stackItem);
            if(stored==null) stored=0;

            stored+=amount;

            applyStored(stackItem,stored);

        },1L);
    }

    // ================= DROP =================

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){

        ItemStack item = e.getItemDrop().getItemStack();

        Integer stored = getStored(item);

        if(stored==null || stored<=0) return;

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack hand = p.getInventory().getItemInMainHand();

            if(hand==null) return;

            Integer s = getStored(hand);

            if(s==null || s<=0) return;

            s--;

            if(s>0){

                applyStored(hand,s);

            }else{

                clearStored(hand);

            }

            if(hand.getAmount()<64){

                hand.setAmount(64);
            }

        },1L);
    }

    // ================= PLACE =================

    @EventHandler
    public void onPlace(BlockPlaceEvent e){

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack hand = p.getInventory().getItemInMainHand();

            if(hand==null) return;

            Integer stored = getStored(hand);

            if(stored==null || stored<=0) return;

            int amount = hand.getAmount();

            if(amount<64){

                hand.setAmount(amount+1);

                stored--;

                if(stored>0){

                    applyStored(hand,stored);

                }else{

                    clearStored(hand);

                }

            }

        },1L);
    }

    // ================= ACTIONBAR =================

    private void startActionbar(){

        new BukkitRunnable(){

            @Override
            public void run(){

                for(Player p : Bukkit.getOnlinePlayers()){

                    ItemStack item = p.getInventory().getItemInMainHand();

                    Integer stored = getStored(item);

                    if(stored==null || stored<=0){

                        p.sendActionBar("");

                        continue;
                    }

                    int total = stored + item.getAmount();

                    String msg = "&eStored: &f"+stored+" &7| &aTotal: &f"+total;

                    p.sendActionBar(color(msg));
                }

            }

        }.runTaskTimer(this,20,20);
    }

    // ================= STORAGE =================

    private Integer getStored(ItemStack item){

        if(item==null) return null;

        if(!item.hasItemMeta()) return null;

        return item.getItemMeta().getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void applyStored(ItemStack item,int amount){

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(storedKey,
                PersistentDataType.INTEGER,amount);

        meta.addEnchant(Enchantment.UNBREAKING,1,true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();

        lore.add(color("&7Stored Blocks"));
        lore.add(color("&e"+amount));

        meta.setLore(lore);

        item.setItemMeta(meta);

        item.setAmount(64);
    }

    private void clearStored(ItemStack item){

        if(!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().remove(storedKey);

        meta.removeEnchant(Enchantment.UNBREAKING);

        meta.setLore(null);

        item.setItemMeta(meta);
    }

    private String color(String s){

        return ChatColor.translateAlternateColorCodes('&',s);
    }
}
