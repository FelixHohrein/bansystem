package net.baublase.bansystem;

import org.bukkit.plugin.java.JavaPlugin;

public final class BanSystemPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Baublase Ban-System aktiviert (Minecraft " + getServer().getMinecraftVersion() + ").");
    }

    @Override
    public void onDisable() {
        getLogger().info("Baublase Ban-System deaktiviert.");
    }
}
