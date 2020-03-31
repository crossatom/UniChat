package ru.unicorn.storm.unichat;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class UniChat extends JavaPlugin implements Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Plugin startup logic
        if(!(new File(getDataFolder(), "config.yml").exists()))
            saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));

        if(Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Не найден Vault. Отключение.");
            getPluginLoader().disablePlugin(this);
            return;
        }
        if(Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            getLogger().warning("Не найден LuckPerms. Отключение.");
            getPluginLoader().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().fine("Плагин загружен");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent evt) {
        evt.setCancelled(true);

        String format = config.getString("chat.format");
        String message = evt.getMessage();
        boolean global = false;
        boolean question = false;
        boolean advert = false;

        if(evt.getMessage().charAt(0) == '!') {
            format = format.replace("{TYPE}", config.getString("chat.type.global") + " ");
            message = message.substring(1);
            global = true;
        } else if(evt.getMessage().charAt(0) == '?') {
            format = format.replace("{TYPE}", config.getString("chat.type.question") + " ");
            message = message.substring(1);
            question = true;
        } else if(evt.getMessage().charAt(0) == '>') {
            format = format.replace("{TYPE}", config.getString("chat.type.advert") + " ");
            message = message.substring(1);
            advert = true;

            getServer().dispatchCommand(getServer().getConsoleSender(), "eco " + evt.getPlayer().getDisplayName() + " take " + (evt.getMessage().toCharArray().length) + " За рекламу");
        } else {
            format = format.replace("{TYPE}", "");
        }

        LuckPerms lapi = LuckPermsProvider.get();
        User user = lapi.getUserManager().getUser(evt.getPlayer().getDisplayName());

        if(user == null) return;

        ImmutableContextSet contextSet = lapi.getContextManager().getContext(user).orElseGet(lapi.getContextManager()::getStaticContext);
        CachedMetaData metaData = user.getCachedData().getMetaData(QueryOptions.contextual(contextSet));
        String prefix = metaData.getPrefix();

        if(prefix == null) prefix = "NULL";
        format = format.replace("{PREFIX}", prefix);

        if(evt.getPlayer().isOp() && config.getString("chat.op-color") != null) {
            format = format.replace("{NICKNAME}", ChatColor.translateAlternateColorCodes('&', "&" + config.getString("chat.op-color")) + evt.getPlayer().getDisplayName());
        } else {
            format = format.replace("{NICKNAME}", evt.getPlayer().getDisplayName());
        }

        if(message.charAt(0) == ' ') message = message.substring(1);
        String fullmes = ChatColor.translateAlternateColorCodes('&', format + message);
        if(global || question || advert) {
            getServer().broadcastMessage(fullmes);
        } else {
            double radius = config.getInt("chat.radius");
            for(Entity e : evt.getPlayer().getNearbyEntities(radius, radius, radius)) {
                if(e instanceof Player) {
                    e.sendMessage(fullmes);
                }
            }
            evt.getPlayer().sendMessage(fullmes);
            System.out.println(fullmes);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        config = null;
        getLogger().fine("Плагин отключён");
    }
}
