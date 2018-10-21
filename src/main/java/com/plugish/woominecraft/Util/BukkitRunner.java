package com.plugish.woominecraft.Util;

import com.plugish.woominecraft.WooMinecraft;

public class BukkitRunner implements Runnable {

    public final WooMinecraft plugin;

    public BukkitRunner() {
        this.plugin = WooMinecraft.instance;
    }

    public void run() {
        try {
            plugin.check();
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
            e.printStackTrace();
        }
    }

}
