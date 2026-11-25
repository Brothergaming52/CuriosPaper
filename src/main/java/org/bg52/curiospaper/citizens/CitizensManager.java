package org.bg52.curiospaper.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Manages Citizens integration, CuriosTrait registration, and NPC GUI invocation.
 */
public class CitizensManager {

    private final CuriosPaper plugin;
    private NpcCuriosGUI npcCuriosGUI;
    private boolean available = false;

    public CitizensManager(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            return;
        }

        try {
            CitizensAPI.getTraitFactory().registerTrait(
                    TraitInfo.create(CuriosTrait.class).withName("curios")
            );
            this.npcCuriosGUI = new NpcCuriosGUI(plugin);
            Bukkit.getPluginManager().registerEvents(npcCuriosGUI, plugin);
            this.available = true;
            plugin.getLogger().info("[Citizens] Registered CuriosTrait and NPC integration successfully!");
        } catch (Throwable t) {
            plugin.getLogger().warning("[Citizens] Failed to initialize Citizens integration: " + t.getMessage());
            this.available = false;
        }
    }

    public void shutdown() {
        if (!available) return;
        try {
            if (CitizensAPI.hasImplementation()) {
                for (NPC npc : CitizensAPI.getNPCRegistry()) {
                    if (npc.hasTrait(CuriosTrait.class)) {
                        CuriosTrait trait = npc.getTraitNullable(CuriosTrait.class);
                        if (trait != null && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                            plugin.getPacketModelManager().removeAllMobModels((org.bukkit.entity.LivingEntity) npc.getEntity());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public boolean isAvailable() {
        return available;
    }

    public void openGUI(Player player, NPC npc) {
        if (npcCuriosGUI != null) {
            npcCuriosGUI.open(player, npc);
        }
    }

    public NPC resolveNPC(CommandSender sender, String idOrName) {
        if (!available || !CitizensAPI.hasImplementation()) {
            return null;
        }

        if (idOrName == null || idOrName.trim().isEmpty()) {
            return CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        }

        try {
            int id = Integer.parseInt(idOrName);
            NPC npc = CitizensAPI.getNPCRegistry().getById(id);
            if (npc != null) return npc;
        } catch (NumberFormatException ignored) {}

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.getName().equalsIgnoreCase(idOrName)) {
                return npc;
            }
        }

        return null;
    }
}
