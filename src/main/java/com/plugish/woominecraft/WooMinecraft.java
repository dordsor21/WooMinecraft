/*
 * Woo Minecraft Donation plugin
 * Author:	   Jerry Wood
 * Author URI: http://plugish.com
 * License:	   GPLv2
 *
 * Copyright 2014 All rights Reserved
 *
 */
package com.plugish.woominecraft;

import com.plugish.woominecraft.Commands.WooCommand;
import com.plugish.woominecraft.Lang.LangSetup;
import com.plugish.woominecraft.Listeners.PlayerListener;
import com.plugish.woominecraft.Util.BukkitRunner;
import com.plugish.woominecraft.Util.CommandRunner;
import com.plugish.woominecraft.Util.RcHttp;
import org.apache.http.client.utils.URIBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class WooMinecraft extends JavaPlugin {

    public static WooMinecraft instance;

    public HashMap<String, JSONObject> cache;

    private YamlConfiguration l10n;

    @Override
    public void onEnable() {
        instance = this;
        YamlConfiguration config = (YamlConfiguration) getConfig();
        cache = new HashMap<>();

        // Save the default config.yml
        try {
            saveDefaultConfig();
        } catch (IllegalArgumentException e) {
            getLogger().warning(e.getMessage());
        }

        String lang = getConfig().getString("lang");
        if (lang == null) {
            getLogger().warning("No default l10n set, setting to english.");
        }

        initCommands();
        getLogger().info(this.getLang("log.com_init"));

        new PlayerListener();

        // Setup the scheduler
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, new BukkitRunner(), config.getInt("update_interval") * 20, config.getInt("update_interval") * 20);

        getLogger().info(this.getLang("log.enabled"));
    }

    @Override
    public void onDisable() {
        getLogger().info(this.getLang("log.com_init"));
    }

    /**
     * Helper method to get localized strings
     * <p>
     * Much better than typing this.l10n.getString...
     *
     * @param path Path to the config var
     * @return String
     */
    public String getLang(String path) {
        if (null == this.l10n) {

            LangSetup lang = new LangSetup(instance);
            l10n = lang.loadConfig();
        }

        return this.l10n.getString(path);
    }

    /**
     * Validates the basics needed in the config.yml file.
     * <p>
     * Multiple reports of user configs not having keys etc... so this will ensure they know of this
     * and will not allow checks to continue if the required data isn't set in the config.
     *
     * @throws Exception Reason for failing to validate the config.
     */
    private void validateConfig() throws Exception {

        if (1 > this.getConfig().getString("url").length()) {
            throw new Exception("Server URL is empty, check config.");
        } else if (this.getConfig().getString("url").equals("http://playground.dev")) {
            throw new Exception("URL is still the default URL, check config.");
        } else if (1 > this.getConfig().getString("key").length()) {
            throw new Exception("Server Key is empty, this is insecure, check config.");
        }
    }

    /**
     * Checks all online players against the
     * website's database looking for pending donation deliveries
     *
     * @return boolean
     * @throws Exception Why the operation failed.
     */
    public boolean check() throws Exception {

        // Make 100% sure the config has at least a key and url
        this.validateConfig();

        URIBuilder uriBuilder = new URIBuilder(getConfig().getString("url"));
        uriBuilder.addParameter("wmc_key", getConfig().getString("key"));

        String url = uriBuilder.toString();
        if (url.equals("")) {
            throw new Exception("WMC URL is empty for some reason");
        }

        RcHttp rcHttp = new RcHttp(this);
        String httpResponse = rcHttp.request(url);

        // No response, kill out here.
        if (httpResponse.equals("")) {
            return false;
        }

        // Grab the pending commands
        JSONObject pendingCommands = new JSONObject(httpResponse);

        // If the request was not a WordPress success, we may have a message
        if (!pendingCommands.getBoolean("success")) {

            wmc_log("Server response was false, checking for message and bailing.", 2);

            // See if we have a data object.
            Object dataCheck = pendingCommands.get("data");
            if (dataCheck instanceof JSONObject) {
                JSONObject errors = pendingCommands.getJSONObject("data");
                String msg = errors.getString("msg");
                // Throw the message as an exception.
                throw new Exception(msg);
            }

            return false;
        }

        Object dataCheck = pendingCommands.get("data");
        if (!(dataCheck instanceof JSONObject)) {
            wmc_log("No data to process, or data is invalid.");
            return false;
        }

        JSONObject data = pendingCommands.getJSONObject("data");
        Iterator<String> playerNames = data.keys();

        wmc_log("Player names acquired -- walking over them now.");
        while (playerNames.hasNext()) {
            // Walk over players.
            String playerName = playerNames.next();
            wmc_log("Checking for player: " + playerName);

            @SuppressWarnings("deprecation")
            Player player = Bukkit.getServer().getPlayerExact(playerName);
            if (player == null) {
                wmc_log("Player not found. Adding player data to cache to run when they log on.", 2);
                cache.put(playerName.toLowerCase(), data.getJSONObject(playerName));
                continue;
            }

            /*
             * Use white-list worlds check, if it's set.
             */
            if (getConfig().getBoolean("enable-world-whitelist")) {
                List<String> whitelistWorlds = getConfig().getStringList("whitelist-worlds");
                String playerWorld = player.getWorld().getName();
                if (!whitelistWorlds.contains(playerWorld)) {
                    wmc_log("Player " + player.getDisplayName() + " was in world " + playerWorld + " which is not in the white-list, no commands were ran.");
                    continue;
                }
            }

            // Get all orders for the current player.
            JSONObject playerOrders = data.getJSONObject(playerName);

            Bukkit.getScheduler().runTask(instance, new CommandRunner(player, playerOrders));
        }

        wmc_log("All order data processed.");

        return true;
    }

    public void wmc_log(String message) {
        this.wmc_log(message, 1);
    }

    private void wmc_log(String message, Integer level) {

        if (!this.getConfig().getBoolean("debug")) {
            return;
        }

        switch (level) {
            case 1:
                this.getLogger().info(message);
                break;
            case 2:
                this.getLogger().warning(message);
                break;
            case 3:
                this.getLogger().severe(message);
                break;
        }
    }

    /**
     * Initialize Commands
     */
    private void initCommands() {
        getCommand("woo").setExecutor(new WooCommand());
    }
}