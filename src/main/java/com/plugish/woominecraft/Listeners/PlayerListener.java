package com.plugish.woominecraft.Listeners;

import com.plugish.woominecraft.Util.CommandRunner;
import com.plugish.woominecraft.WooMinecraft;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.JSONObject;

public class PlayerListener implements Listener {


    public PlayerListener() {
        Bukkit.getPluginManager().registerEvents(this, WooMinecraft.instance);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        JSONObject playerOrders = WooMinecraft.instance.cache.get(e.getPlayer().getName().toLowerCase());
        if (playerOrders != null)
            Bukkit.getScheduler().runTask(WooMinecraft.instance, new CommandRunner(e.getPlayer(), playerOrders));
    }

}
