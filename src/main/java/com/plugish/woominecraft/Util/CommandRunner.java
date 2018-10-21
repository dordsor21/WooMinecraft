package com.plugish.woominecraft.Util;

import com.plugish.woominecraft.WooMinecraft;
import org.apache.http.client.utils.URIBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CommandRunner implements Runnable {

    private final WooMinecraft plugin;
    private final Player player;
    private final JSONObject playerOrders;

    public CommandRunner(Player player, JSONObject playerOrders) {
        this.plugin = WooMinecraft.instance;
        this.player = player;
        this.playerOrders = playerOrders;
    }

    public void run() {
        try {
            if (plugin.getConfig().getBoolean("enable-world-whitelist")) {
                List<String> whitelistWorlds = plugin.getConfig().getStringList("whitelist-worlds");
                String playerWorld = player.getWorld().getName();
                if (!whitelistWorlds.contains(playerWorld)) {
                    plugin.wmc_log("Player " + player.getDisplayName() + " was in world " + playerWorld + " which is not in the white-list, no commands were ran.");
                    return;
                }
            }
            URIBuilder uriBuilder = new URIBuilder(plugin.getConfig().getString("url"));
            uriBuilder.addParameter("wmc_key", plugin.getConfig().getString("key"));

            RcHttp rcHttp = new RcHttp(plugin);
            Iterator<String> orderIDs = playerOrders.keys();
            String url = uriBuilder.toString();
            if (url.equals(""))
                throw new Exception("WMC URL is empty for some reason");
            JSONArray processedData = new JSONArray();
            plugin.wmc_log("Walking over orders for player.");
            while (orderIDs.hasNext()) {
                String orderID = orderIDs.next();
                plugin.wmc_log("===========================");

                // Get all commands per order
                JSONArray commands = playerOrders.getJSONArray(orderID);

                plugin.wmc_log("Processing command for order: " + orderID);
                plugin.wmc_log("===========================");
                plugin.wmc_log("Command Set: " + commands.toString());

                // Walk over commands, executing them one by one.
                for (Integer x = 0; x < commands.length(); x++) {
                    String baseCommand = commands.getString(x);

                    plugin.wmc_log("Dirty Command: " + baseCommand);

                    final String command = baseCommand.replace("%s", player.getName()).replace("&quot;", "\"").replace("&#039;", "'");

                    plugin.wmc_log("Clean Command: " + command);

                    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

                    // TODO: Make this better... nesting a 'new' class while not a bad idea is bad practice.
                    scheduler.scheduleSyncDelayedTask(plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command), 20L);
                }
                processedData.put(Integer.parseInt(orderID));
            }

            HashMap<String, String> postData = new HashMap<>();
            postData.put("processedOrders", processedData.toString());

            String updatedCommandSet = rcHttp.send(url, postData);
            JSONObject updatedResponse = new JSONObject(updatedCommandSet);
            boolean status = updatedResponse.getBoolean("success");

            if (!status) {
                Object dataSet = updatedResponse.get("data");
                if (dataSet instanceof JSONObject) {
                    String message = ((JSONObject) dataSet).getString("msg");
                    throw new Exception(message);
                }
                throw new Exception("Failed sending updated orders to the server, got this instead:" + updatedCommandSet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
