package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        loadConfig();

        storedKey = new NamespacedKey(this,"stored");

        Bukkit.getPluginManager().registerEvents(this,this);

        startActionbar();
    }

    private void loadConfig(){

        defaultMax = getConfig().getInt("stack.default-max",128);

        only64 = getConfig().getBoolean("stack.only-64-stackable",true);

        glow = getConfig().getBoolean("stored.glow",true);

        loreEnabled = getConfig().getBoolean("stored.lore.enabled",true);

        loreFormat = getConfig().getStringList("stored.lore.format");

        actionbarEnabled = getConfig().getBoolean("actionbar.enabled",true);

        permissionLimits.clear();

        ConfigurationSection sec = getConfig().getConfigurationSection("stack.permission-limits");

        if(sec != null){

            for(String key : sec.getKeys(false)){

                permissionLimits.put(key,sec.getInt(key));

            }
        }
    }

    private int getMax(Player p){

        int max = defaultMax;

        for(Map.Entry<String,Integer> e : permissionLimits.entrySet()){

            if(p.hasPermission(e.getKey())){

                int v = e.getValue();

                if(v == -1) return -1;

                if(v > max) max = v;

            }
        }

        return max;
    }

    // ================= PICKUP =================

    @EventHandler
    public void onPickup(EntityPickupItemEvent e){

        if(!(e.getEntity() instanceof Player player)) return;

        ItemStack picked = e.getItem().getItemStack();

        if(picked == null) return;

        Material type = picked.getType();

        if(only64 && type.getMaxStackSize() != 64) return;

        Bukkit.getScheduler().runTaskLater(this,()->{

            Inventory inv = player.getInventory();

            int max = getMax(player);

            ItemStack storedStack = null;

            for(ItemStack item : inv.getContents()){

                if(item == null) continue;

                if(item.getType() != type) continue;

                Integer stored = getStored(item);

                if(stored != null){

                    storedStack = item;

                    break;
                }
            }

            if(storedStack == null) return;

            Integer stored = getStored(storedStack);

            if(stored == null) stored = 0;

            int currentTotal = storedStack.getAmount() + stored;

            if(max != -1 && currentTotal >= max) return;

            int add = picked.getAmount();

            int newTotal = currentTotal + add;

            if(max != -1 && newTotal > max){

                add = max - currentTotal;

                newTotal = max;

            }

            int newStored = newTotal - storedStack.getAmount();

            applyStored(storedStack,newStored,player);

        },1L);
    }

    // ================= AUTO REFILL =================

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){

        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack item = p.getInventory().getItemInMainHand();

            if(item == null) return;

            Integer stored = getStored(item);

            if(stored == null || stored <= 0) return;

            int amount = item.getAmount();

            if(amount < 64){

                item.setAmount(amount + 1);

                stored--;

                if(stored > 0){

                    applyStored(item,stored,p);

                }else{

                    clearStored(item);

                }
            }

        },1L);
    }

    // ================= ACTIONBAR =================

    private void startActionbar(){

        if(!actionbarEnabled) return;

        new BukkitRunnable(){

            @Override
            public void run(){

                for(Player p : Bukkit.getOnlinePlayers()){

                    ItemStack item = p.getInventory().getItemInMainHand();

                    Integer stored = getStored(item);

                    if(stored == null){

                        p.sendActionBar("");

                        continue;
                    }

                    int total = item.getAmount() + stored;

                    int max = getMax(p);

                    String text = "&eStored: "+stored+" &7| &fTotal: "+total+"/"+(max==-1?"∞":max);

                    p.sendActionBar(color(text));

                }

            }

        }.runTaskTimer(this,2L,2L);
    }

    // ================= STORAGE =================

    private Integer getStored(ItemStack item){

        if(item == null || !item.hasItemMeta()) return null;

        return item.getItemMeta().getPersistentDataContainer().get(storedKey,PersistentDataType.INTEGER);
    }

    private void applyStored(ItemStack item,int amount,Player player){

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(storedKey,PersistentDataType.INTEGER,amount);

        if(glow){

            meta.addEnchant(Enchantment.UNBREAKING,1,true);

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        }

        if(loreEnabled && loreFormat != null){

            int total = item.getAmount() + amount;

            int max = getMax(player);

            List<String> lore = new ArrayList<>();

            for(String line : loreFormat){

                line = line
                        .replace("{stored}",String.valueOf(amount))
                        .replace("{total}",String.valueOf(total))
                        .replace("{max}",String.valueOf(max));

                lore.add(color(line));

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

    private String color(String s){

        return ChatColor.translateAlternateColorCodes('&',s);
    }
}
