package moe.kira.globalshopping;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class GlobalShopping extends JavaPlugin implements Listener {
    // ------------------------------
    // Storage
    // ------------------------------
    private Collection<Material> allowedItems = Sets.newHashSet();
    private List<BusinessData> business = Lists.newArrayList();
    private List<BusinessData> newestBusiness = Lists.newArrayListWithExpectedSize(4);
    private List<BusinessMoneyData> economy = Lists.newArrayList();
    
    public static class BusinessData implements Serializable {
        private static final long serialVersionUID = 6255715797864393504L;
        
        public Material material;
        public int count;
        public int price;
        public String owner;
        @Nullable
        public String buyer;
        public short data;
        public byte data2;
        
        public BusinessData(Material material, int count, int price, String owner, String buyer, short data, byte data2) {
            this.material = material;
            this.count = count;
            this.price = price;
            this.owner = owner;
            this.buyer = buyer;
            this.data = data;
            this.data2 = data2;
        }
        
        public BusinessData() {}
        
        public String toString() {
            return material.name() + ";" + count + ";" + price + ";" + owner + ";" + buyer;
        }
        
        @SuppressWarnings("deprecation")
        public BusinessData fromString(String string) {
            String[] data = string.split(";");
            try {
                material = Material.getMaterial(data[0]);
            } catch (Throwable t) {
                try {
                    material = Material.getMaterial(Integer.valueOf(data[0]));
                } catch (Throwable e) {
                    material = Material.matchMaterial(data[0]);
                }
            }
            count = Integer.valueOf(data[1]);
            price = Integer.valueOf(data[2]);
            owner = data[3];
            buyer = data[4];
            try {
                this.data = Short.valueOf(data[5]);
            } catch (Throwable t) {
                this.data = 0;
            }
            try {
                this.data2 = Byte.valueOf(data[6]);
            } catch (Throwable t) {
                this.data2 = -111;
            }
            return this;
        }
    }
    
    public static class BusinessMoneyData implements Serializable {
        private static final long serialVersionUID = 6255715797864393505L;
        
        public String owner;
        public int money;
        
        public BusinessMoneyData(String owner, int money) {
            this.owner = owner;
            this.money = money;
        }
        
        public BusinessMoneyData() {}

        public String toString() {
            return owner + ";" + money;
        }
        
        public BusinessMoneyData fromString(String string) {
            owner = StringUtils.substringBefore(string, ";");
            money = Integer.valueOf(StringUtils.substringAfter(string, ";"));
            return this;
        }
    }
    
    // ------------------------------
    // Command
    // ------------------------------
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof InventoryHolder)) {
            sender.sendMessage("此操作只能由玩家进行。");
            return true;
        }
        
        if (args != null && args.length == 1) {
            if (args[0].equalsIgnoreCase("query")) {
                for (BusinessMoneyData moneyData : economy) {
                    if (moneyData.owner.equals(sender.getName())) {
                        sender.sendMessage(" §5> §6你现在拥有 §a" + moneyData.money + " §6贸易值。");
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                        return true;
                    }
                }
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                sender.sendMessage(" §5> §6你现在拥有 §a0 §6贸易值。");
                return true;
            } else if (args[0].equalsIgnoreCase("reload") && sender.isOp()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(new File("./plugins/".concat(this.getName()).concat("/config.yml")));
                allowedItems = Sets.newHashSet(Collections2.transform(Sets.newHashSet(config.getStringList("settings.allowed-item-materials")), name -> {
                    try {
                        return Material.valueOf(name.toUpperCase(Locale.ROOT));
                    } catch (Throwable e) {
                        try {
                            return Material.getMaterial(Integer.parseInt(name));
                        } catch (Throwable t) {
                            return Material.matchMaterial(name);
                        }
                    }
                }));
                return true;
            } else if (args[0].equalsIgnoreCase("add") && sender.isOp()) {
                allowedItems.add(((Player) sender).getInventory().getItemInMainHand().getType());
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                sender.sendMessage(" §5> §6已添加允许的物品类型： " + ((Player) sender).getInventory().getItemInMainHand().getType());
                
                FileConfiguration config = this.getConfig();
                List<String> list = Lists.newArrayList(Collections2.transform(allowedItems, name -> name.name().toUpperCase(Locale.ROOT)));
                config.set("settings.allowed-item-materials", list);
                this.saveConfig();
                
                return true;
            }
        }
        
        if (args != null && args.length == 2) {
            if (args[0].equalsIgnoreCase("import")) {
                try {
                    int price = Integer.valueOf(args[1]);
                    
                    if (!playerPoints.getAPI().take(sender.getName(), price)) {
                        sender.sendMessage(" §5> §6由于点卷不足， 贸易值转换已取消。");
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                        return true;
                    }
                    
                    for (BusinessMoneyData moneyData : economy) {
                        if (moneyData.owner.equals(sender.getName())) {
                            moneyData.money = moneyData.money + price * 100;
                            sender.sendMessage(" §5> §6你现在拥有 §a" + moneyData.money + " §6贸易值和 §a" + playerPoints.getAPI().look(sender.getName()) + " §6点卷。");
                            ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                            return true;
                        }
                    }
                    
                    BusinessMoneyData moneyData = new BusinessMoneyData();
                    moneyData.owner = sender.getName();
                    moneyData.money = price * 100;
                    economy.add(moneyData);
                    sender.sendMessage(" §5> §6你现在拥有 §a" + moneyData.money + " §6贸易值和 §a" + playerPoints.getAPI().look(sender.getName()) + " §6点卷。");
                    return true;
                } catch (Throwable t) {
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    sender.sendMessage(" §5> §6由于格式不符， 贸易值转换已取消。");
                    return true;
                }
            }
            
            if (args[0].equalsIgnoreCase("export")) {
                try {
                    int price = Integer.valueOf(args[1]);
                    
                    for (BusinessMoneyData moneyData : economy) {
                        if (moneyData.owner.equals(sender.getName())) {
                            if (moneyData.money < price * 100) {
                                sender.sendMessage(" §5> §6由于贸易值不足， 点卷转换已取消。");
                                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                                return true;
                            }
                            
                            playerPoints.getAPI().give(sender.getName(), price);
                            moneyData.money = moneyData.money - price * 100;
                            sender.sendMessage(" §5> §6你现在拥有 §a" + moneyData.money + " §6贸易值和  §a" + playerPoints.getAPI().look(sender.getName()) + " §6点卷。");
                            ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                            return true;
                        }
                    }
                    
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    sender.sendMessage(" §5> §6由于资金不足， 贸易值转换已取消。");
                    return true;
                } catch (Throwable t) {
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    sender.sendMessage(" §5> §6由于格式不符， 贸易值转换已取消。");
                    return true;
                }
            }
        }
        
        // GUI
        createAndOpenInventoryFor((Player) sender, 1);
        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
        
        return true;
    }
    
    private void createAndOpenInventoryFor(Player player, int status) {
        InventoryView now = player.getOpenInventory();
        int nowIndex = 1;
        int pages = (int) Math.floor(business.size() / 45) + 1;
        pages = pages < 1 ? 1 : pages;
        
        if (status != 1) {
            nowIndex = 1;
            if (now.getTitle().startsWith("§l大宗货物交易所")) {
                nowIndex = Integer.parseInt(now.getTitle().substring(12, now.getTitle().length() - 2));
            }
        }
        
        int slots = 0;
        Inventory inventory = Bukkit.createInventory(InventoryHolder.class.cast(player), 54, "§l大宗货物交易所 §8" + (status == 1 ? nowIndex : status == 2 ? nowIndex + 1 : nowIndex - 1 < 1 ? 1 : nowIndex - 1) + "/" + pages);
        int expectedIndexMax = status == 1 || (status == 3 && nowIndex == 1) ? 45 : status == 2 ? (nowIndex + 1) * 45 : nowIndex * 45;
        
        BusinessData[] datas = business.toArray(new BusinessData[business.size()]);
        // Sort
        Arrays.sort(datas, (business1, business2) -> business1.price > business2.price ? 1 : 0);
        business = Lists.newArrayList(datas);
        
        int expectedBusiness = business.size() > expectedIndexMax ? expectedIndexMax : business.size();
        for (int i = status == 1 || (status == 3 && nowIndex == 1) ? 0 : status == 2 ? nowIndex * 45 - 1 : (nowIndex - 1) * 45 - 1; i < expectedBusiness; i++) {
            BusinessData data = business.get(i);
            @SuppressWarnings("deprecation")
            ItemStack sample = data.data2 == -111 ? new ItemStack(data.material, data.count, data.data) : new ItemStack(data.material, data.count, data.data, data.data2);
            
            // Process item
            ItemMeta meta = sample.getItemMeta();
            meta.setLore(Lists.newArrayList("§6价格： §a" + data.price, "§6所有者： " + data.owner, player.getName().equals(data.owner) ? "点按以下架" : "点按以采购"));
            sample.setItemMeta(meta);
            
            inventory.setItem(slots++, sample);
        }
        
        // Newest business
        // Process item
        ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        itemStack.setItemMeta(meta);
        
        inventory.setItem(45, nowIndex > 1 ? bottonPrev : itemStack);
        inventory.setItem(46, itemStack);
        slots = 47;
        
        int i = 0;
        for (; i < newestBusiness.size(); i++) {
            BusinessData data = newestBusiness.get(i);
            @SuppressWarnings("deprecation")
            ItemStack sample = data.data2 == -111 ? new ItemStack(data.material, data.count, data.data) : new ItemStack(data.material, data.count, data.data, data.data2);
            
            // Process item
            meta = sample.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            meta.setLore(Lists.newArrayList("§6价格： §a" + data.price, "§6所有者： " + data.owner, "§6购买者： " + data.buyer, "最近成交的货物"));
            sample.setItemMeta(meta);
            
            inventory.setItem(slots++, sample);
        }
        
        int emptySlots = 4 - i;
        if (emptySlots > 0) {
            for (int x = 0; x < emptySlots; x++)
                inventory.setItem(slots++, itemStack);
        }
        
        inventory.setItem(51, itemStack);
        inventory.setItem(52, bottonNewBusiness);
        inventory.setItem(53, nowIndex >= pages - 1 ? itemStack : bottonNext);
        
        player.openInventory(inventory);
    }
    
    // ------------------------------
    // GUI
    // ------------------------------
    private ItemStack bottonNext;
    private ItemStack bottonPrev;
    private ItemStack bottonNewBusiness;
    
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null || !event.getView().getTitle().startsWith("§l大宗货物交易所") || !event.getCurrentItem().hasItemMeta())
            return;
        
        if (event.getCurrentItem().getItemMeta().hasItemFlag(ItemFlag.HIDE_POTION_EFFECTS))
            event.setCancelled(true);
        
        if (!event.getCurrentItem().getItemMeta().hasLore())
            return;
        
        // Buy
        if (event.getCurrentItem().getItemMeta().getLore().contains("点按以采购")) {
            for (BusinessData data : business) {
                // Search data
                if (data.material == event.getCurrentItem().getType() && data.price == Integer.parseInt(event.getCurrentItem().getItemMeta().getLore().get(0).substring(8)) && data.owner.equalsIgnoreCase(event.getCurrentItem().getItemMeta().getLore().get(1).substring(7))) {
                    
                    // PlayerPoints
                    boolean take = false;
                    boolean pay = false;
                    for (BusinessMoneyData moneyData : economy) {
                        if (moneyData.owner.equals(event.getWhoClicked().getName())) {
                            if (moneyData.money - data.price < 0) {
                                // Notify
                                event.getWhoClicked().closeInventory();
                                ((Player) event.getWhoClicked()).sendTitle("§6未交易", "你的贸易值不足以完成此次交易");
                                ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                                
                                event.setCancelled(true);
                                return;
                            } else {
                                moneyData.money = (int) (moneyData.money - data.price);
                                take = true;
                                break;
                            }
                        }
                    }
                    
                    if (take) {
                        // Only pay when success take
                        for (BusinessMoneyData moneyData : economy) {
                            if (moneyData.owner.equals(event.getCurrentItem().getItemMeta().getLore().get(1).substring(7))) {
                                moneyData.money = (int) (moneyData.money + data.price);
                                pay = true;
                                
                                Player player = Bukkit.getPlayerExact(data.owner);
                                if (player != null && player.isOnline()) {
                                    ((Player) player).sendTitle("§6交易成功", "你的 §a" + data.count + " §r件 §6" + getItemName(event.getCurrentItem()) + " §r已被采购");
                                    Bukkit.dispatchCommand(player, "gs query");
                                    ((Player) player).playSound(((Player) player).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                                }
                                
                                break;
                            }
                        }
                        
                        // No money data
                        if (!pay) {
                            BusinessMoneyData moneyData = new BusinessMoneyData();
                            moneyData.owner = data.owner;
                            moneyData.money = data.price;
                            economy.add(moneyData);
                            
                            Player player = Bukkit.getPlayerExact(data.owner);
                            if (player != null && player.isOnline()) {
                                ((Player) player).sendTitle("§6交易成功", "你的 §a" + event.getCurrentItem().getAmount() + " §r件 §6" + getItemName(event.getCurrentItem()) + " §r已被采购");
                                Bukkit.dispatchCommand(player, "gs query");
                                ((Player) player).playSound(((Player) player).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                            }
                        }
                    } else {
                        // Notify for no money data at all
                        event.getWhoClicked().closeInventory();
                        ((Player) event.getWhoClicked()).sendTitle("§6未交易", "你的贸易值不足以完成此次交易");
                        ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                        event.setCancelled(true);
                        return;
                    }
                    
                    // Add buyer
                    data.buyer = event.getWhoClicked().getName();
                    
                    // Add to newest
                    newestBusiness.add(0, data);
                    if (newestBusiness.size() > 4)
                        for (int i = 4; i < newestBusiness.size(); i++)
                            newestBusiness.remove(i);
                    
                    // Trans item
                    business.remove(data);
                    
                    // Notify
                    event.getWhoClicked().closeInventory();
                    event.getWhoClicked().getInventory().addItem(data.data2 == -111 ? new ItemStack(data.material, data.count, data.data) : new ItemStack(data.material, data.count, data.data, data.data2));
                    ((Player) event.getWhoClicked()).sendTitle("§6交易成功", "你已成功入手 §a" + event.getCurrentItem().getAmount() + " §r件 §6" + getItemName(event.getCurrentItem()));
                    ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                    Bukkit.dispatchCommand(((Player) event.getWhoClicked()), "gs query");
                    
                    event.setCancelled(true);
                    return;
                }
            }
            
            ((Player) event.getWhoClicked()).sendTitle("§c操作失败", "§6你所选择的货物已不存在");
            ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
            
            event.setCancelled(true);
            return;
        } else if (event.getCurrentItem().getItemMeta().getLore().contains("点按以下架")) {
            for (BusinessData data : business) {
                // Search data
                if (data.material == event.getCurrentItem().getType() && data.price == Integer.parseInt(event.getCurrentItem().getItemMeta().getLore().get(0).substring(8)) && data.owner.equalsIgnoreCase(event.getCurrentItem().getItemMeta().getLore().get(1).substring(7))) {
                    
                    // Trans item
                    business.remove(data);
                    
                    // Notify
                    event.getWhoClicked().closeInventory();
                    event.getWhoClicked().getInventory().addItem(data.data2 == -111 ? new ItemStack(data.material, data.count, data.data) : new ItemStack(data.material, data.count, data.data, data.data2));
                    ((Player) event.getWhoClicked()).sendTitle("§6已下架", "你从市场下架了 §a" + event.getCurrentItem().getAmount() + " §r件 §6" + getItemName(event.getCurrentItem()));
                    ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    
                    event.setCancelled(true);
                    return;
                }
            }
            
            ((Player) event.getWhoClicked()).sendTitle("§c操作失败", "§6你所选择的货物已不存在");
            ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
            
            event.setCancelled(true);
            return;
        } else if (event.getCurrentItem().getItemMeta().getLore().contains("按下以开始上架商品")) {
            // Notify
            event.getWhoClicked().closeInventory();
            ((Player) event.getWhoClicked()).sendTitle("§6上架商品", "请在手持货物时提交商品价格");
            ((Player) event.getWhoClicked()).sendMessage(" §5> §6请在聊天栏提交你想要设定的商品贸易值价格。 §7(1 : 100)");
            
            ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
            submitter.add((Player) event.getWhoClicked());
            
            event.setCancelled(true);
            return;
        } else if (event.getCurrentItem().getItemMeta().getLore().contains("按下以浏览下一页商品")) {
            ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
            createAndOpenInventoryFor((Player) event.getWhoClicked(), 2);
            event.setCancelled(true);
        } else if (event.getCurrentItem().getItemMeta().getLore().contains("按下以浏览上一页商品")) {
            createAndOpenInventoryFor((Player) event.getWhoClicked(), 3);
            ((Player) event.getWhoClicked()).playSound(((Player) event.getWhoClicked()).getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
            event.setCancelled(true);
        }
    }
    
    public String getItemName(ItemStack item) {
        net.minecraft.server.v1_12_R1.ItemStack nmsStack = org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack.asNMSCopy(item);
        return nmsStack.getItem().b(nmsStack);
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith("§l大宗货物交易所")) {
            event.setCancelled(true);
        }
    }
    
    // ------------------------------
    // Submit
    // ------------------------------
    private Set<Player> submitter = Sets.newConcurrentHashSet();
    
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (submitter.contains(event.getPlayer())) {
            submitter.remove(event.getPlayer());
            try {
                // Notify
                int price = Integer.valueOf(event.getMessage());
                
                if (price < 0 || price > Integer.MAX_VALUE) {
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    event.getPlayer().sendMessage(" §5> §6由于格式不符， 商品上架已取消。");
                    return;
                }
                
                // Trans item
                ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
                
                if (itemStack == null || itemStack.getType() == Material.AIR || !allowedItems.contains(itemStack.getType())) {
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    ((Player) event.getPlayer()).sendTitle("§6未上架", "§7你持有的货物不支持交易");
                    return;
                }
                
                if (itemStack.getAmount() < itemStack.getType().getMaxStackSize()) {
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                    ((Player) event.getPlayer()).sendTitle("§6未上架", "§7只有完整的货物可以上架 §2(" + itemStack.getAmount() + "/" + itemStack.getType().getMaxStackSize() + ")");
                    return;
                }
                
                event.getPlayer().getInventory().setItemInMainHand(null);
                event.getPlayer().sendMessage(" §5> §6你已经成功以 §a" + price + " §6贸易值的价格上架商品。");
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 100, 1);
                
                BusinessData data = new BusinessData(itemStack.getType(), itemStack.getAmount(), price, event.getPlayer().getName(), null, itemStack.getDurability(), itemStack.getData().getData());
                business.add(data);
                event.setCancelled(true);
                return;
            } catch (Throwable t) {
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 100, 1);
                event.getPlayer().sendMessage(" §5> §6由于格式不符， 商品上架已取消。");
                return;
            }
        }
    }
    
    // ------------------------------
    // Plugin
    // ------------------------------
    private PlayerPoints playerPoints;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        // Config
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        allowedItems = Sets.newHashSet(Collections2.transform(Sets.newHashSet(config.getStringList("settings.allowed-item-materials")), name -> {
            try {
                return Material.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (Throwable e) {
                try {
                    return Material.getMaterial(Integer.parseInt(name));
                } catch (Throwable t) {
                    return Material.matchMaterial(name);
                }
            }
        }));
        List<String> list = Lists.newArrayList(Collections2.transform(allowedItems, name -> name.name().toUpperCase(Locale.ROOT)));
        config.set("settings.allowed-item-materials", list);
        this.saveConfig();
        
        // Business data
        File file = new File(".".concat("/plugins/").concat(this.getName()).concat("/business_data.yml"));
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        business = Lists.newArrayList(Lists.transform(data.getList("global-business") == null ? Lists.newArrayList() : data.getStringList("global-business"), object -> new BusinessData().fromString(object)));
        
        file = new File(".".concat("/plugins/").concat(this.getName()).concat("/economy_data.yml"));
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        data = YamlConfiguration.loadConfiguration(file);
        economy = Lists.newArrayList(Lists.transform(data.getList("global-economy") == null ? Lists.newArrayList() : data.getStringList("global-economy"), object -> new BusinessMoneyData().fromString(object)));
        
        // Command
        Bukkit.getPluginCommand("gs").setExecutor(this);
        
        // Misc
        this.hookPlayerPoints();
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Items
        bottonNext = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = bottonNext.getItemMeta();
        meta.setDisplayName("§6下一页");
        meta.setLore(Collections.singletonList("按下以浏览下一页商品"));
        bottonNext.setItemMeta(meta);
        
        bottonPrev = new ItemStack(Material.TIPPED_ARROW);
        meta = bottonPrev.getItemMeta();
        meta.setDisplayName("§6上一页");
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.setLore(Collections.singletonList("按下以浏览上一页商品"));
        bottonPrev.setItemMeta(meta);
        
        bottonNewBusiness = new ItemStack(Material.EMERALD);
        meta = bottonNewBusiness.getItemMeta();
        meta.setDisplayName("§6上架商品");
        meta.setLore(Collections.singletonList("按下以开始上架商品"));
        bottonNewBusiness.setItemMeta(meta);
        
        // Save timer
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            File ffile = new File(".".concat(File.separator).concat("plugins").concat(File.separator).concat(this.getName()).concat(File.separator).concat("data").concat(File.separator).concat("business.yml"));
            FileConfiguration fdata = YamlConfiguration.loadConfiguration(ffile);
            List<String> list2 = Lists.newArrayList(Collections2.transform(business, BusinessData::toString));
            fdata.set("global-business", list2);
            try {
                fdata.save(ffile);
            } catch (IOException e) {
                
                e.printStackTrace();
            }
            
            ffile = new File(".".concat(File.separator).concat("plugins").concat(File.separator).concat(this.getName()).concat(File.separator).concat("data").concat(File.separator).concat("economy.yml"));
            fdata = YamlConfiguration.loadConfiguration(ffile);
            List<String> list3 = Lists.newArrayList(Collections2.transform(economy, BusinessMoneyData::toString));
            fdata.set("global-economy", list3);
            try {
                fdata.save(ffile);
            } catch (IOException e) {
                
                e.printStackTrace();
            }
        }, 6000, 6000);
    }
    
    private boolean hookPlayerPoints() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("PlayerPoints");
        playerPoints = PlayerPoints.class.cast(plugin);
        return playerPoints != null;
    }
    
    @Override
    public void onDisable() {
        // Save config
        File file = new File(".".concat("/plugins/").concat(this.getName()).concat("/business_data.yml"));
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> list2 = Lists.newArrayList(Collections2.transform(business, BusinessData::toString));
        data.set("global-business", list2);
        try {
            data.save(file);
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
        file = new File(".".concat("/plugins/").concat(this.getName()).concat("/economy_data.yml"));
        data = YamlConfiguration.loadConfiguration(file);
        List<String> list3 = Lists.newArrayList(Collections2.transform(economy, BusinessMoneyData::toString));
        data.set("global-economy", list3);
        try {
            data.save(file);
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
}
