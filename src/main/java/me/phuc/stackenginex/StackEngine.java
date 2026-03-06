package me.phuc.stackenginex;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
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

        Material type = e.getItem().getItemStack().getType();

        Bukkit.getScheduler().runTaskLater(this,()->{

            mergeInventory(player,type);

        },2L);
    }

    // ================= INVENTORY CLICK =================

    @EventHandler
    public void onClick(InventoryClickEvent e){

        if(!(e.getWhoClicked() instanceof Player p)) return;

        Bukkit.getScheduler().runTaskLater(this,()->{

            for(ItemStack item : p.getInventory().getContents()){

                if(item==null) continue;

                mergeInventory(p,item.getType());
            }

        },1L);
    }

    // ================= OPEN CONTAINER =================

    @EventHandler
    public void onOpen(InventoryOpenEvent e){

        if(!(e.getPlayer() instanceof Player p)) return;

        Bukkit.getScheduler().runTaskLater(this,()->{

            for(ItemStack item : p.getInventory().getContents()){

                if(item==null) continue;

                mergeInventory(p,item.getType());
            }

        },2L);
    }

    // ================= MERGE =================

    private void mergeInventory(Player player,Material type){

        Inventory inv = player.getInventory();

        int total = 0;

        List<Integer> slots = new ArrayList<>();

        for(int i=0;i<inv.getSize();i++){

            ItemStack item = inv.getItem(i);

            if(item==null) continue;
            if(item.getType()!=type) continue;

            slots.add(i);

            total += item.getAmount();

            Integer stored = getStored(item);

            if(stored!=null) total+=stored;
        }

        if(total<=64) return;

        for(int slot : slots){

            inv.setItem(slot,null);
        }

        ItemStack stack = new ItemStack(type,64);

        int stored = total-64;

        applyStored(stack,stored);

        inv.addItem(stack);
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

    // ================= DROP =================

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){

        Player p = e.getPlayer();

        Item item = e.getItemDrop();

        createHologram(item);

        Bukkit.getScheduler().runTaskLater(this,()->{

            ItemStack hand = p.getInventory().getItemInMainHand();

            if(hand==null) return;

            Integer stored = getStored(hand);

            if(stored==null || stored<=0) return;

            stored--;

            if(stored>0){

                applyStored(hand,stored);

            }else{

                clearStored(hand);
            }

            if(hand.getAmount()<64){
                hand.setAmount(64);
            }

        },1L);
    }

    // ================= HOLOGRAM =================

    private void createHologram(Item item){

        ItemStack stack = item.getItemStack();

        Integer stored = getStored(stack);

        if(stored==null) return;

        String name = stack.getType().name().toLowerCase().replace("_"," ");

        Location loc = item.getLocation().add(0,0.6,0);

        ArmorStand holo = loc.getWorld().spawn(loc,ArmorStand.class);

        holo.setInvisible(true);
        holo.setGravity(false);
        holo.setMarker(true);
        holo.setCustomNameVisible(true);

        holo.setCustomName(color("&e"+name+" &7| &fStored: &a"+stored));

        new BukkitRunnable(){

            @Override
            public void run(){

                if(item.isDead() || !item.isValid()){

                    holo.remove();
                    cancel();
                    return;
                }

                holo.teleport(item.getLocation().add(0,0.6,0));
            }

        }.runTaskTimer(this,0,5);
    }

    // ================= ACTIONBAR =================

    private void startActionbar(){

        new BukkitRunnable(){

            @Override
            public void run(){

                for(Player p:Bukkit.getOnlinePlayers()){

                    ItemStack item = p.getInventory().getItemInMainHand();

                    Integer stored = getStored(item);

                    if(stored==null || stored<=0){

                        p.sendActionBar("");

                        continue;
                    }

                    int total = stored + item.getAmount();

                    String msg="&eStored: &f"+stored+" &7| &aTotal: &f"+total;

                    p.sendActionBar(color(msg));
                }

            }

        }.runTaskTimer(this,5,5);
    }

    // ================= STORAGE =================

    private Integer getStored(ItemStack item){

        if(item==null) return null;

        if(!item.hasItemMeta()) return null;

        return item.getItemMeta().getPersistentDataContainer()
                .get(storedKey, PersistentDataType.INTEGER);
    }

    private void applyStored(ItemStack item,int amount){

        ItemMeta meta=item.getItemMeta();

        meta.getPersistentDataContainer().set(storedKey,
                PersistentDataType.INTEGER,amount);

        meta.addEnchant(Enchantment.UNBREAKING,1,true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore=new ArrayList<>();

        lore.add(color("&7Stored Blocks"));
        lore.add(color("&e"+amount));

        meta.setLore(lore);

        item.setItemMeta(meta);

        item.setAmount(64);
    }

    private void clearStored(ItemStack item){

        if(!item.hasItemMeta()) return;

        ItemMeta meta=item.getItemMeta();

        meta.getPersistentDataContainer().remove(storedKey);

        meta.removeEnchant(Enchantment.UNBREAKING);

        meta.setLore(null);

        item.setItemMeta(meta);
    }

    private String color(String s){

        return ChatColor.translateAlternateColorCodes('&',s);
    }
}
