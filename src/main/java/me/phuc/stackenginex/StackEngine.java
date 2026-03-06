package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StackEngine extends JavaPlugin implements Listener {

    private NamespacedKey storedKey;

    private int defaultMax;
    private boolean only64;
    private boolean glow;
    private boolean actionbarEnabled;
    private boolean loreEnabled;

    private List<String> loreFormat;

    private final Map<String,Integer> permissionLimits = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        storedKey = new NamespacedKey(this,"stored");

        loadConfigValues();

        Bukkit.getPluginManager().registerEvents(this,this);

        startActionbar();
    }

    private void loadConfigValues(){

        defaultMax = Math.max(1,getConfig().getInt("stack.default-max",128));

        only64 = getConfig().getBoolean("stack.only-64-stackable",true);

        glow = getConfig().getBoolean("stored.glowing-when-stored",true);

        loreEnabled = getConfig().getBoolean("stored.lore.enabled",true);

        loreFormat = getConfig().getStringList("stored.lore.format");

        actionbarEnabled = getConfig().getBoolean("actionbar.enabled",true);

        permissionLimits.clear();

        ConfigurationSection sec = getConfig().getConfigurationSection("stack.permission-limits");

        if(sec!=null){

            for(String key:sec.getKeys(false)){

                permissionLimits.put(key,sec.getInt(key));

            }

        }

    }

    private int getMax(Player p){

        int highest = defaultMax;

        for(Map.Entry<String,Integer> e:permissionLimits.entrySet()){

            if(p.hasPermission(e.getKey())){

                int value = e.getValue();

                if(value==-1) return -1;

                if(value>highest) highest=value;

            }

        }

        return highest;

    }

    // ================= PICKUP =================

    @EventHandler
    public void onPickup(EntityPickupItemEvent e){

        if(!(e.getEntity() instanceof Player player)) return;

        ItemStack picked = e.getItem().getItemStack();

        if(picked==null) return;

        Material type = picked.getType();

        if(only64 && type.getMaxStackSize()!=64) return;

        Bukkit.getScheduler().runTaskLater(this,()->{

            Inventory inv = player.getInventory();

            int max = getMax(player);

            int total = 0;

            ItemStack storedStack = null;

            for(ItemStack item:inv.getContents()){

                if(item==null) continue;

                if(item.getType()!=type) continue;

                total += item.getAmount();

                Integer stored = getStored(item);

                if(stored!=null){

                    total += stored;

                    storedStack = item;

                }

            }

            if(max!=-1 && total>=max) return;

            if(total<=64) return;

            if(max!=-1 && total>max) total=max;

            int storedAmount = total-64;

            inv.remove(type);

            ItemStack result = new ItemStack(type,64);

            if(storedAmount>0){

                applyStored(result,storedAmount,player);

            }

            inv.addItem(result);

        },1L);

    }

    // ================= AUTO REFILL =================

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack hand = p.getInventory().getItemInMainHand();

            if(hand==null) return;

            Integer stored = getStored(hand);

            if(stored==null || stored<=0) return;

            int amount = hand.getAmount();

            if(amount<64){

                hand.setAmount(amount+1);

                int newStored = stored-1;

                if(newStored>0){

                    applyStored(hand,newStored,p);

                }else{

                    clearStored(hand);

                }

            }

        },1L);

    }

    // ================= ACTIONBAR =================

    private void startActionbar(){

        if(!actionbarEnabled) return;

        new BukkitRunnable(){

            public void run(){

                for(Player p:Bukkit.getOnlinePlayers()){

                    ItemStack item = p.getInventory().getItemInMainHand();

                    Integer stored = getStored(item);

                    if(stored==null){

                        p.sendActionBar("");

                        continue;

                    }

                    int total = item.getAmount()+stored;

                    int max = getMax(p);

                    String text = "§eStored: "+stored+" §7| §fTotal: "+total+"/"+(max==-1?"∞":max);

                    p.sendActionBar(text);

                }

            }

        }.runTaskTimer(this,2L,2L);

    }

    // ================= STORAGE =================

    private Integer getStored(ItemStack item){

        if(item==null) return null;

        if(!item.hasItemMeta()) return null;

        return item.getItemMeta().getPersistentDataContainer()

                .get(storedKey, PersistentDataType.INTEGER);

    }

    private void applyStored(ItemStack item,int amount,Player player){

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(storedKey,PersistentDataType.INTEGER,amount);

        if(glow){

            meta.addEnchant(Enchantment.UNBREAKING,1,true);

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        }

        if(loreEnabled && loreFormat!=null){

            List<String> lore = new ArrayList<>();

            int total = item.getAmount()+amount;

            int max = getMax(player);

            for(String line:loreFormat){

                line=line

                        .replace("{stored}",String.valueOf(amount))

                        .replace("{total}",String.valueOf(total))

                        .replace("{max}",max==-1?"∞":String.valueOf(max));

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

        meta.setLore(null);

        meta.removeEnchant(Enchantment.UNBREAKING);

        item.setItemMeta(meta);

    }

}
