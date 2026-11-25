package org.bg52.curiospaper.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Universal multi-platform scheduler abstraction providing seamless, zero-overhead execution
 * across Folia (multi-threaded regional ticking), Paper, Purpur, and Spigot.
 *
 * Guarantees that thread-independent logic runs asynchronously outside the main thread,
 * while region/entity-specific mutations are authoritatively dispatched to their owning threads.
 *
 * Lifecycle-safe: Gracefully handles plugin shutdown and disabled states without throwing
 * IllegalPluginAccessException.
 */
public class PlatformScheduler {

    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
        default boolean isCancelled() { return false; }
    }

    private static final boolean IS_FOLIA;
    private static final boolean HAS_REGION_SCHEDULER;
    private static PlatformScheduler instance;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (Throwable ignored) {}
        IS_FOLIA = folia;

        boolean region = false;
        try {
            Bukkit.class.getMethod("getAsyncScheduler");
            region = true;
        } catch (Throwable ignored) {}
        HAS_REGION_SCHEDULER = region;
    }

    private final Plugin plugin;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static PlatformScheduler get() {
        return instance;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static boolean hasRegionScheduler() {
        return HAS_REGION_SCHEDULER;
    }

    private static boolean isEntityValid(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Player player) {
            return player.isOnline();
        }
        return entity.isValid();
    }

    // ─────────────────────────────────────────────────────────────
    // ASYNC (Runs outside the main/region thread on all platforms)
    // ─────────────────────────────────────────────────────────────

    public TaskHandle runAsync(Runnable task) {
        if (!plugin.isEnabled()) {
            try {
                CompletableFuture.runAsync(task);
            } catch (Throwable ignored) {}
            return () -> {};
        }
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                try {
                    CompletableFuture.runAsync(task);
                } catch (Throwable ignored) {}
                return () -> {};
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                try {
                    CompletableFuture.runAsync(task);
                } catch (Throwable ignored) {}
                return () -> {};
            }
        }
    }

    public TaskHandle runAsyncLater(Runnable task, long delayTicks) {
        if (!plugin.isEnabled()) return () -> {};
        long millis = Math.max(50L, delayTicks * 50L);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), millis, TimeUnit.MILLISECONDS);
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(1L, delayTicks));
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    public TaskHandle runAsyncTimer(Runnable task, long initialDelayTicks, long periodTicks) {
        if (!plugin.isEnabled()) return () -> {};
        long initialMillis = Math.max(50L, initialDelayTicks * 50L);
        long periodMillis = Math.max(50L, periodTicks * 50L);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), initialMillis, periodMillis, TimeUnit.MILLISECONDS);
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, Math.max(1L, initialDelayTicks), Math.max(1L, periodTicks));
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GLOBAL TICK / SERVER THREAD
    // ─────────────────────────────────────────────────────────────

    public TaskHandle runGlobal(Runnable task) {
        if (!plugin.isEnabled()) {
            try { task.run(); } catch (Throwable ignored) {}
            return () -> {};
        }
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                try { task.run(); } catch (Throwable ignored) {}
                return () -> {};
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTask(plugin, task);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                try { task.run(); } catch (Throwable ignored) {}
                return () -> {};
            }
        }
    }

    public TaskHandle runGlobalLater(Runnable task, long delayTicks) {
        if (!plugin.isEnabled()) return () -> {};
        long safeDelay = Math.max(1L, delayTicks);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), safeDelay);
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskLater(plugin, task, safeDelay);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    public TaskHandle runGlobalTimer(Runnable task, long initialDelayTicks, long periodTicks) {
        if (!plugin.isEnabled()) return () -> {};
        long safeInitial = Math.max(1L, initialDelayTicks);
        long safePeriod = Math.max(1L, periodTicks);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), safeInitial, safePeriod);
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskTimer(plugin, task, safeInitial, safePeriod);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ENTITY SCHEDULER (Runs on entity's owning thread / region)
    // ─────────────────────────────────────────────────────────────

    public TaskHandle runEntity(Entity entity, Runnable task) {
        if (!isEntityValid(entity)) return () -> {};
        if (!plugin.isEnabled()) {
            try { task.run(); } catch (Throwable ignored) {}
            return () -> {};
        }
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = entity.getScheduler().run(plugin, t -> task.run(), null);
                if (st == null) {
                    return runGlobal(task);
                }
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return runGlobal(task);
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTask(plugin, task);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                try { task.run(); } catch (Throwable ignored) {}
                return () -> {};
            }
        }
    }

    public TaskHandle runEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (!isEntityValid(entity) || !plugin.isEnabled()) return () -> {};
        long safeDelay = Math.max(1L, delayTicks);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = entity.getScheduler().runDelayed(plugin, t -> task.run(), null, safeDelay);
                if (st == null) {
                    return runGlobalLater(task, safeDelay);
                }
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return runGlobalLater(task, safeDelay);
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskLater(plugin, task, safeDelay);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    public TaskHandle runEntityTimer(Entity entity, Runnable task, long initialDelayTicks, long periodTicks) {
        if (!isEntityValid(entity) || !plugin.isEnabled()) return () -> {};
        long safeInitial = Math.max(1L, initialDelayTicks);
        long safePeriod = Math.max(1L, periodTicks);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, safeInitial, safePeriod);
                if (st == null) {
                    return runGlobalTimer(task, safeInitial, safePeriod);
                }
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return runGlobalTimer(task, safeInitial, safePeriod);
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskTimer(plugin, task, safeInitial, safePeriod);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // REGION / LOCATION SCHEDULER
    // ─────────────────────────────────────────────────────────────

    public TaskHandle runLocation(Location loc, Runnable task) {
        if (loc == null || loc.getWorld() == null) return runGlobal(task);
        if (!plugin.isEnabled()) {
            try { task.run(); } catch (Throwable ignored) {}
            return () -> {};
        }
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getRegionScheduler().run(plugin, loc, t -> task.run());
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return runGlobal(task);
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTask(plugin, task);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                try { task.run(); } catch (Throwable ignored) {}
                return () -> {};
            }
        }
    }

    public TaskHandle runLocationLater(Location loc, Runnable task, long delayTicks) {
        if (loc == null || loc.getWorld() == null) return runGlobalLater(task, delayTicks);
        if (!plugin.isEnabled()) return () -> {};
        long safeDelay = Math.max(1L, delayTicks);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getRegionScheduler().runDelayed(plugin, loc, t -> task.run(), safeDelay);
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return runGlobalLater(task, delayTicks);
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskLater(plugin, task, safeDelay);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    public TaskHandle runLocationTimer(Location loc, Runnable task, long initialDelayTicks, long periodTicks) {
        if (loc == null || loc.getWorld() == null) return runGlobalTimer(task, initialDelayTicks, periodTicks);
        if (!plugin.isEnabled()) return () -> {};
        long safeInitial = Math.max(1L, initialDelayTicks);
        long safePeriod = Math.max(1L, periodTicks);
        if (HAS_REGION_SCHEDULER) {
            try {
                var st = Bukkit.getRegionScheduler().runAtFixedRate(plugin, loc, t -> task.run(), safeInitial, safePeriod);
                return new TaskHandle() {
                    @Override public void cancel() { st.cancel(); }
                    @Override public boolean isCancelled() { return st.isCancelled(); }
                };
            } catch (Throwable t) {
                return runGlobalTimer(task, initialDelayTicks, periodTicks);
            }
        } else {
            try {
                var bt = Bukkit.getScheduler().runTaskTimer(plugin, task, safeInitial, safePeriod);
                return new TaskHandle() {
                    @Override public void cancel() { bt.cancel(); }
                    @Override public boolean isCancelled() { return bt.isCancelled(); }
                };
            } catch (Throwable t) {
                return () -> {};
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SHUTDOWN / CANCEL ALL
    // ─────────────────────────────────────────────────────────────

    public void cancelTasks() {
        if (HAS_REGION_SCHEDULER) {
            try {
                Bukkit.getAsyncScheduler().cancelTasks(plugin);
                Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {}
        }
        if (!IS_FOLIA) {
            try {
                Bukkit.getScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {}
        }
    }
}
