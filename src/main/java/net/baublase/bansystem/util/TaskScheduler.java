package net.baublase.bansystem.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Dünne Hülle um den Bukkit-Scheduler, damit Services nicht direkt Paper kennen.
 */
public final class TaskScheduler {

    private final JavaPlugin plugin;

    public TaskScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    public void runSync(Runnable runnable) {
        if (plugin.getServer().isPrimaryThread()) {
            runnable.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public void runAsync(Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public void runLater(Runnable runnable, long ticks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, ticks);
    }

    public void runAsyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
    }

    public void runSyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }
}
