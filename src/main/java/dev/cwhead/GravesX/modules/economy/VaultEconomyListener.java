package dev.cwhead.GravesX.modules.economy;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.event.GraveAutoLootEvent;
import dev.cwhead.GravesX.event.GraveBreakEvent;
import dev.cwhead.GravesX.event.GraveOpenEvent;
import dev.cwhead.GravesX.event.GravePreTeleportEvent;
import dev.cwhead.GravesX.modules.economy.util.I18n;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Listener to charge players for GravesX actions using Vault and I18n messages.
 */
public final class VaultEconomyListener implements Listener {

    private final Graves plugin;
    private final Economy economy;
    private final EconomyRuntime runtime;
    private final I18n i18n;

    public VaultEconomyListener(Graves plugin, Economy economy, EconomyRuntime runtime, I18n i18n) {
        this.plugin = plugin;
        this.economy = economy;
        this.runtime = runtime;
        this.i18n = i18n;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGravePreTeleport(GravePreTeleportEvent e) {
        if (!e.isPlayer()) {
            plugin.debugMessage("Player not found on teleport event. Skipping check.", 2);
            return;
        }
        Player p = e.getPlayer();

        if (plugin.hasGrantedPermission("graves.economy.teleport", p)) {
            plugin.debugMessage(p.getName() + " has the \"graves.economy.teleport\" bypass permission.", 2);
            return;
        }

        if (chargeOrCancel(p, ChargeConfig.Type.TELEPORT, "teleport")) {
            plugin.debugMessage(p.getName() + " had insufficient funds. Cancelling teleportation.", 2);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveOpen(GraveOpenEvent e) {
        Player p = e.getPlayer();
        if (plugin.hasGrantedPermission("graves.economy.open", p)) {
            plugin.debugMessage(p.getName() + " has the \"graves.economy.open\" bypass permission.", 2);
            return;
        }
        if (chargeOrCancel(p, ChargeConfig.Type.OPEN, "open a grave")) {
            plugin.debugMessage(p.getName() + " had insufficient funds. Cancelling grave open event.", 2);
            e.setCancelled(true);
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLoot(GraveAutoLootEvent e) {
        if (!e.isEntityActuallyPlayer()) return;
        Player p = e.getPlayer();
        if (p == null) return;

        if (plugin.hasGrantedPermission("graves.economy.autoloot", p)) {
            plugin.debugMessage(p.getName() + " has the \"graves.economy.autoloot\" bypass permission.", 2);
            return;
        }

        if (chargeOrCancel(p, ChargeConfig.Type.AUTOLOOT, "auto-loot")) {
            plugin.debugMessage(p.getName() + " had insufficient funds. Cancelling grave auto loot event.", 2);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveBlockBreak(GraveBreakEvent e) {
        Player p = e.getPlayer();

        if (plugin.hasGrantedPermission("graves.economy.block_break", p)) {
            plugin.debugMessage(p.getName() + " has the \"graves.economy.block_break\" bypass permission.", 2);
            return;
        }

        if (chargeOrCancel(p, ChargeConfig.Type.BLOCK_BREAK, "break a grave")) {
            plugin.debugMessage(p.getName() + " had insufficient funds. Cancelling grave block break event.", 2);
            e.setCancelled(true);
        }
    }

    private boolean chargeOrCancel(Player p, ChargeConfig.Type type, String actionWord) {
        ChargeConfig cfg = runtime.get();

        if (!cfg.isTypeEnabled(type)) return false;

        double balance = economy.getBalance(p);
        double baseCost = cfg.computeCost(type, p, balance);

        if (baseCost <= 0.0) return false;

        OptionalDouble overrideOpt = getChargeOverride(p, type);
        double cost = overrideOpt.orElse(baseCost);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("currency", cfg.currency());
        placeholders.put("amount", cfg.fmt(cost));
        placeholders.put("type", actionWord);

        if (balance < cost) {
            sendMsg(p, "graves.economy." + type.name().toLowerCase() + ".insufficient", placeholders);
            return true;
        }

        EconomyResponse r = economy.withdrawPlayer(p, cost);
        if (r == null || !r.transactionSuccess()) {
            sendMsg(p, "graves.economy." + type.name().toLowerCase() + ".insufficient", placeholders);
            return true;
        }

        sendMsg(p, "graves.economy." + type.name().toLowerCase() + ".charged", placeholders);
        return false;
    }

    private void sendMsg(Player p, String key, Map<String,String> placeholders) {
        String locale = p.getLocale().toLowerCase();
        String msg = i18n.translate(key, placeholders, locale);
        if (msg != null && !msg.isEmpty()) {
            p.sendMessage(msg);
        }
    }

    private OptionalDouble getChargeOverride(Player p, ChargeConfig.Type type) {
        final String PREFIX = "graves.economy.chargebypass.";
        final String typePrefix = PREFIX + type.name().toLowerCase() + ".";
        final double MIN_COST = 0.0;
        final double MAX_COST = 1_000_000.0;

        double best = Double.POSITIVE_INFINITY;

        for (PermissionAttachmentInfo permInfo : p.getEffectivePermissions()) {
            String perm = permInfo.getPermission();
            if (!perm.toLowerCase(Locale.ROOT).startsWith(typePrefix)) continue;

            String suffix = perm.substring(typePrefix.length());
            if (suffix.isEmpty()) continue;

            try {
                double parsed = Double.parseDouble(suffix);
                if (parsed < MIN_COST) parsed = MIN_COST;
                if (parsed > MAX_COST) parsed = MAX_COST;
                if (parsed < best) best = parsed;
            } catch (NumberFormatException ignored) {
                // ignore malformed permissions
            }
        }

        return best == Double.POSITIVE_INFINITY ? OptionalDouble.empty() : OptionalDouble.of(best);
    }

}