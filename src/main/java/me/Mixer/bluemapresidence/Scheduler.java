package me.Mixer.bluemapresidence;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

/**
 * Scheduling abstraction that works on both regular Paper/Spigot and on Folia (and Folia-based forks
 * such as Canvas / Luminol).
 * <p>
 * On Folia there is no single main thread and the legacy {@link org.bukkit.scheduler.BukkitScheduler}
 * main-thread methods are unsupported. This helper routes:
 * <ul>
 *   <li>{@link #run(Runnable)} to the <b>global region thread</b> — the Folia equivalent of the main
 *       thread, where global / per-world state and PlaceholderAPI are safe to touch;</li>
 *   <li>{@link #runAsync(Runnable)} and {@link #runTimerAsync} to the <b>async scheduler</b> — a thread
 *       pool off every region thread, where heavy computation and (thread-safe) BlueMap marker writes
 *       belong.</li>
 * </ul>
 */
public final class Scheduler {

    private static final long MS_PER_TICK = 50L;

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            String version = Bukkit.getVersion();
            return version.contains("Folia") || version.contains("Luminol") || version.contains("Canvas");
        }
    }

    /** Run on the global region thread (Folia) / main thread (Paper). */
    public static void run(Runnable runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute((Plugin) Main.getInstance(), runnable);
        } else {
            Bukkit.getScheduler().runTask((Plugin) Main.getInstance(), runnable);
        }
    }

    /** Run off any region thread, on the async scheduler / an async BukkitTask. */
    public static Task runAsync(Runnable runnable) {
        if (isFolia()) {
            return new Task(Bukkit.getAsyncScheduler().runNow((Plugin) Main.getInstance(), t -> runnable.run()));
        }
        return new Task(Bukkit.getScheduler().runTaskAsynchronously((Plugin) Main.getInstance(), runnable));
    }

    /**
     * Repeating task that runs entirely on the async scheduler. Previously this used the global region
     * scheduler's fixed-rate timer, which meant the periodic marker refresh executed its heavy work on
     * the global region thread and stalled the server. The periodic trigger now runs async; any work that
     * must briefly touch the main/global thread is re-dispatched from inside the callback.
     *
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period in ticks
     */
    public static Task runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia()) {
            long delayMs = Math.max(1L, delayTicks) * MS_PER_TICK;
            long periodMs = Math.max(1L, periodTicks) * MS_PER_TICK;
            return new Task(Bukkit.getAsyncScheduler().runAtFixedRate((Plugin) Main.getInstance(),
                    t -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS));
        }
        return new Task(Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin) Main.getInstance(),
                runnable, Math.max(0L, delayTicks), Math.max(1L, periodTicks)));
    }

    public static class Task {
        private Object foliaTask;
        private BukkitTask bukkitTask;

        Task(Object foliaTask) {
            this.foliaTask = foliaTask;
        }

        Task(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }

        public void cancel() {
            if (this.foliaTask != null) {
                ((ScheduledTask) this.foliaTask).cancel();
            } else if (this.bukkitTask != null) {
                this.bukkitTask.cancel();
            }
        }
    }
}
