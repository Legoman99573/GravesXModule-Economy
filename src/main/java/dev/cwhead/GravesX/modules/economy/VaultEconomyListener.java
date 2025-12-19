package dev.cwhead.GravesX.modules.economy;

import com.ranull.graves.Graves;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.event.*;
import dev.cwhead.GravesX.modules.economy.util.I18n;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.lang.reflect.Method;
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

    /**
     * Pre-check ONLY: if TELEPORT is FIXED mode, treat the fixed amount as "per block" and
     * cancel if the player's BALANCE is too low. No withdrawal here.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGravePreTeleport(GravePreTeleportEvent e) {
        if (!e.isPlayer()) {
            plugin.debugMessage("Player not found on teleport pre-event. Skipping check.", 2);
            return;
        }

        Player p = e.getPlayer();

        if (plugin.hasGrantedPermission("graves.economy.teleport", p)) {
            plugin.debugMessage(p.getName() + " has the \"graves.economy.teleport\" bypass permission.", 2);
            return;
        }

        int blocks = getTeleportBlocks(p, e.getGrave());

        if (!hasEnoughBalanceFor(p, ChargeConfig.Type.TELEPORT, blocks)) {
            plugin.debugMessage(p.getName() + " has insufficient funds (pre-check). Cancelling teleportation.", 2);
            e.setCancelled(true);
        }
    }

    /**
     * Actual charge (withdraw) happens on the real teleport event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveTeleport(GraveTeleportEvent e) {
        Player p = e.getPlayer();
        if (p == null) return;

        if (plugin.hasGrantedPermission("graves.economy.teleport", p)) {
            plugin.debugMessage(p.getName() + " has the \"graves.economy.teleport\" bypass permission.", 2);
            return;
        }

        int blocks = getTeleportBlocks(p, e.getGrave());

        if (chargeOrCancel(p, ChargeConfig.Type.TELEPORT, "teleport", blocks)) {
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

        if (chargeOrCancel(p, ChargeConfig.Type.OPEN, "open a grave", 1)) {
            plugin.debugMessage(p.getName() + " had insufficient funds. Cancelling grave open event.", 2);
            e.setCancelled(true);
        }
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

        if (chargeOrCancel(p, ChargeConfig.Type.AUTOLOOT, "auto-loot", 1)) {
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

        if (chargeOrCancel(p, ChargeConfig.Type.BLOCK_BREAK, "break a grave", 1)) {
            plugin.debugMessage(p.getName() + " had insufficient funds. Cancelling grave block break event.", 2);
            e.setCancelled(true);
        }
    }

    /**
     * Charge the player for the given action. Returns true when the event should be cancelled
     * (e.g. insufficient funds or failed charge), false on success / no charge required.
     *
     * @param blocks For TELEPORT only: when config mode is FIXED, this multiplies the FIXED amount per block.
     *               For other actions, pass 1.
     */
    private boolean chargeOrCancel(Player p, ChargeConfig.Type type, String actionWord, int blocks) {
        ChargeConfig cfg = runtime.get();

        if (economy == null) {
            plugin.debugMessage("Charge cancelled: economy provider is null (Vault not hooked?)", 1);
            return true;
        }

        if (!cfg.isTypeEnabled(type)) {
            plugin.debugMessage("Charge skipped: type " + type + " disabled", 2);
            return false;
        }

        double balance = economy.getBalance(p);
        double baseCost = cfg.computeCost(type, p, balance);

        if (!(baseCost > 0.0)) {
            plugin.debugMessage("Charge skipped: computed cost=" + baseCost + " for " + p.getName()
                    + " balance=" + balance + " type=" + type, 2);
            return false;
        }

        OptionalDouble overrideOpt = getChargeOverride(p, type);
        double cost = overrideOpt.orElse(baseCost);

        cost = applyTeleportPerBlockIfNeeded(cfg, type, cost, blocks);

        if (overrideOpt.isPresent()) {
            plugin.debugMessage("Charge override for " + p.getName() + " type=" + type + " base=" + baseCost + " override=" + cost, 2);
        }
        if (!(cost > 0.0)) {
            plugin.debugMessage("Charge skipped: final cost=" + cost + " for " + p.getName(), 2);
            return false;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("currency", cfg.currency());
        placeholders.put("amount", cfg.fmt(cost));
        placeholders.put("type", actionWord);

        boolean hasEnough;
        try {
            hasEnough = economy.has(p, cost);
        } catch (Throwable t) {
            plugin.debugMessage("economy.has(...) threw: " + t.getMessage(), 2);
            hasEnough = (balance >= cost);
        }

        if (!hasEnough) {
            sendMsg(p, "graves.economy." + type.name().toLowerCase() + ".insufficient", placeholders);
            plugin.debugMessage("Insufficient funds: " + p.getName() + " balance=" + balance + " cost=" + cost, 2);
            return true;
        }

        EconomyResponse r;

        try {
            r = economy.withdrawPlayer(p, cost);
        } catch (Throwable t) {
            plugin.debugMessage("withdrawPlayer(OfflinePlayer,double) threw: " + t.getMessage(), 2);
            r = null;
        }

        if (r == null || !r.transactionSuccess()) {
            try {
                r = economy.withdrawPlayer(p, p.getWorld().getName(), cost);
            } catch (Throwable ignored) {
            }
        }

        if (r == null || !r.transactionSuccess()) {
            String err = (r == null) ? "null response" : (r.errorMessage + " (" + r.type + ")");
            plugin.debugMessage("Charging failed: " + p.getName() + " cost=" + cost + " err=" + err, 2);
            placeholders.put("error", err);
            sendMsg(p, "graves.economy." + type.name().toLowerCase() + ".failed", placeholders);
            return true;
        }

        double after = economy.getBalance(p);
        plugin.debugMessage("Charged " + p.getName() + " " + cost + " " + cfg.currency()
                + " for " + type + " balance " + balance + " -> " + after, 2);

        sendMsg(p, "graves.economy." + type.name().toLowerCase() + ".charged", placeholders);
        return false;
    }

    /**
     * Balance-only affordability check (NO withdraw, NO economy.has()).
     * Uses the same pricing + override + teleport-per-block rules as chargeOrCancel.
     */
    private boolean hasEnoughBalanceFor(Player p, ChargeConfig.Type type, int blocks) {
        ChargeConfig cfg = runtime.get();

        if (economy == null) {
            plugin.debugMessage("Pre-check cancelled: economy provider is null (Vault not hooked?)", 1);
            return false;
        }

        if (!cfg.isTypeEnabled(type)) return true;

        final double balance;
        try {
            balance = economy.getBalance(p);
        } catch (Throwable t) {
            plugin.debugMessage("economy.getBalance(...) threw: " + t.getMessage(), 2);
            return true;
        }

        double baseCost = cfg.computeCost(type, p, balance);
        if (baseCost <= 0.0) return true;

        OptionalDouble overrideOpt = getChargeOverride(p, type);
        double cost = overrideOpt.orElse(baseCost);

        // TELEPORT + FIXED => per-block
        cost = applyTeleportPerBlockIfNeeded(cfg, type, cost, blocks);

        return balance >= cost;
    }

    /**
     * TELEPORT special rule:
     * If config mode is FIXED, treat the fixed amount as "per block" from player -> grave.
     *
     * Uses reflection to avoid hard-coding a specific ChargeConfig API shape (getMode/mode/isFixed, etc).
     */
    private double applyTeleportPerBlockIfNeeded(ChargeConfig cfg, ChargeConfig.Type type, double cost, int blocks) {
        if (type != ChargeConfig.Type.TELEPORT) return cost;
        if (!isFixedMode(cfg, type)) return cost;

        int b = Math.max(1, blocks);
        return cost * b;
    }

    private boolean isFixedMode(ChargeConfig cfg, ChargeConfig.Type type) {
        // Try common method names without creating compile-time dependencies on a Mode enum.
        // Accepts anything whose .toString() equals "FIXED" (case-insensitive).
        String[] candidates = {"getMode", "mode"};
        for (String name : candidates) {
            try {
                Method m = cfg.getClass().getMethod(name, ChargeConfig.Type.class);
                Object mode = m.invoke(cfg, type);
                if (mode != null && "FIXED".equalsIgnoreCase(mode.toString())) return true;
            } catch (Throwable ignored) {
            }
        }

        // Also allow boolean helpers if you have one in ChargeConfig
        String[] boolCandidates = {"isFixed", "isFixedMode"};
        for (String name : boolCandidates) {
            try {
                Method m = cfg.getClass().getMethod(name, ChargeConfig.Type.class);
                Object r = m.invoke(cfg, type);
                if (r instanceof Boolean && (Boolean) r) return true;
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    /**
     * Distance in blocks from player -> grave (min 1). If worlds differ/unknown, returns 1.
     */
    private int getTeleportBlocks(Player p, Grave grave) {
        if (p == null || grave == null) return 1;

        Location pl = p.getLocation();
        Location gl = grave.getLocationDeath();
        if (gl == null) return 1;
        if (pl.getWorld() == null || gl.getWorld() == null) return 1;
        if (!pl.getWorld().equals(gl.getWorld())) return 1;

        int blocks = (int) Math.ceil(pl.distance(gl));
        return Math.max(1, blocks);
    }

    private void sendMsg(Player p, String key, Map<String, String> placeholders) {
        String locale = p.locale().toLanguageTag();
        locale = locale.toLowerCase(Locale.ROOT).replace('-', '_');

        String msg = i18n.translate(key, placeholders, locale);

        if (msg == null || msg.isBlank() || msg.equals(key)) {
            plugin.debugMessage("Missing i18n key: " + key + " (locale=" + locale + ")", 2);
            return;
        }

        p.sendMessage(msg);
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
