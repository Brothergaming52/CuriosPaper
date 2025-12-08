package org.bg52.curiospaper.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * ChatInputManager
 *
 * Supports:
 *  - single-line sessions (complete after first non-cancel/done message)
 *  - multi-line sessions (accumulate lines until player types "done")
 *
 * Backwards compatible: existing startSession(...) still works as multi-line.
 */
public class ChatInputManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, ChatInputSession> activeSessions;

    public ChatInputManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeSessions = new HashMap<>();
    }

    /**
     * Old-style multi-line session (backwards-compatible).
     * Player must type "done" to finish, or "cancel" to abort.
     */
    public void startSession(Player player, String promptMessage, Consumer<List<String>> onComplete, Runnable onCancel) {
        startSession(player, promptMessage, onComplete, onCancel, /* maxLines= */ -1);
    }

    /**
     * New: start a session with a maxLines parameter.
     * - If maxLines == 1: complete the session as soon as the player enters one non-control message.
     * - If maxLines <= 0: unlimited; player must type "done".
     */
    public void startSession(Player player, String promptMessage, Consumer<List<String>> onComplete, Runnable onCancel, int maxLines) {
        ChatInputSession session = new ChatInputSession(promptMessage, onComplete, onCancel, maxLines);
        activeSessions.put(player.getUniqueId(), session);

        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§6§l» §eCHAT INPUT MODE");
        player.sendMessage("");
        player.sendMessage("§7" + promptMessage);
        player.sendMessage("");

        if (maxLines == 1) {
            player.sendMessage("§eType §ayour answer §eto submit (or §c'cancel' §eto abort)");
        } else {
            player.sendMessage("§eType §a'done' §eto finish");
            player.sendMessage("§c'cancel' §eto abort");
        }

        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    /**
     * Convenience for single-line sessions that accept a Consumer<String>.
     */
    public void startSingleLineSession(Player player, String promptMessage, Consumer<String> onComplete, Runnable onCancel) {
        Consumer<List<String>> wrapper = lines -> {
            String value = lines.isEmpty() ? "" : lines.get(0);
            onComplete.accept(value);
        };
        startSession(player, promptMessage, wrapper, onCancel, 1);
    }

    /**
     * Checks if a player has an active chat input session
     */
    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    /**
     * Cancels a player's chat input session
     */
    public void cancelSession(UUID playerId) {
        ChatInputSession s = activeSessions.remove(playerId);
        if (s != null) {
            // run cancel on main thread
            Bukkit.getScheduler().runTask(plugin, s.getOnCancel());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!activeSessions.containsKey(playerId)) {
            return;
        }

        // Cancel the event so it doesn't broadcast
        event.setCancelled(true);

        ChatInputSession session = activeSessions.get(playerId);
        String message = event.getMessage().trim();

        // cancel command
        if (message.equalsIgnoreCase("cancel")) {
            activeSessions.remove(playerId);
            player.sendMessage("§c✘ Input cancelled.");

            Bukkit.getScheduler().runTask(plugin, () -> session.getOnCancel().run());
            return;
        }

        // If this is a single-line session, accept any non-control message and finish immediately.
        if (session.getMaxLines() == 1) {
            session.addInputLine(message);
            activeSessions.remove(playerId);
            player.sendMessage("§a✔ Input received. Processing...");

            Bukkit.getScheduler().runTask(plugin, () -> session.getOnComplete().accept(session.getInputLines()));
            return;
        }

        // Multi-line session handling
        if (message.equalsIgnoreCase("done")) {
            List<String> lines = session.getInputLines();
            activeSessions.remove(playerId);
            player.sendMessage("§a✔ Input complete! Processing...");

            Bukkit.getScheduler().runTask(plugin, () -> session.getOnComplete().accept(lines));
            return;
        }

        // Add a line to the session
        session.addInputLine(message);
        player.sendMessage("§8[" + session.getInputLines().size() + "] §7" + message);

        // If there is a positive maxLines (>1), and we've reached it, auto-complete
        if (session.getMaxLines() > 1 && session.getInputLines().size() >= session.getMaxLines()) {
            List<String> lines = session.getInputLines();
            activeSessions.remove(playerId);
            player.sendMessage("§a✔ Input complete (max lines reached). Processing...");

            Bukkit.getScheduler().runTask(plugin, () -> session.getOnComplete().accept(lines));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up sessions when players quit
        activeSessions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Represents a chat input session for a player
     */
    private static class ChatInputSession {
        private final String promptMessage;
        private final List<String> inputLines;
        private final Consumer<List<String>> onComplete;
        private final Runnable onCancel;
        private final int maxLines; // -1 = unlimited until 'done', 1 = single-line, >1 = fixed count

        public ChatInputSession(String promptMessage, Consumer<List<String>> onComplete, Runnable onCancel, int maxLines) {
            this.promptMessage = promptMessage;
            this.inputLines = new ArrayList<>();
            this.onComplete = onComplete;
            this.onCancel = onCancel;
            this.maxLines = maxLines;
        }

        public String getPromptMessage() {
            return promptMessage;
        }

        public List<String> getInputLines() {
            return new ArrayList<>(inputLines);
        }

        public void addInputLine(String line) {
            inputLines.add(line);
        }

        public Consumer<List<String>> getOnComplete() {
            return onComplete;
        }

        public Runnable getOnCancel() {
            return onCancel;
        }

        public int getMaxLines() {
            return maxLines;
        }
    }
}
