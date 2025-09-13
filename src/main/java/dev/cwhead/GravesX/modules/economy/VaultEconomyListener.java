package dev.cwhead.GravesX.modules.economy;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import dev.cwhead.GravesX.event.GraveAutoLootEvent;
import dev.cwhead.GravesX.event.GraveBreakEvent;
import dev.cwhead.GravesX.event.GraveOpenEvent;
import dev.cwhead.GravesX.event.GravePreTeleportEvent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Listener that applies Vault charges to GravesX actions and cancels the action
 * when the player cannot pay according to {@link ChargeConfig}.
 */
final class VaultEconomyListener implements Listener {

    /** GravesX plugin instance for context. */
    private final Graves plugin;

    /** Active Vault economy provider. */
    private final Economy economy;

    /** Runtime access to current charging configuration. */
    private final EconomyRuntime runtime;

    /**
     * Creates a new listener bound to a Vault economy and the runtime config.
     *
     * @param plugin  Graves plugin
     * @param economy Vault economy provider
     * @param runtime supplier for current {@link ChargeConfig}
     */
    VaultEconomyListener(Graves plugin, Economy economy, EconomyRuntime runtime) {
        this.plugin = plugin;
        this.economy = economy;
        this.runtime = runtime;
    }

    /**
     * Charges for teleporting to a grave.
     *
     * @param e pre-teleport event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreTeleport(GravePreTeleportEvent e) {
        Player p = e.isPlayer() ? e.getPlayer() : null;
        if (p == null) return;
        if (!chargeOrCancel(p, ChargeConfig.Type.TELEPORT, "teleport")) e.setCancelled(true);
    }

    /**
     * Charges for opening a grave.
     *
     * @param e open event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(GraveOpenEvent e) {
        Player p = e.getPlayer();
        if (!chargeOrCancel(p, ChargeConfig.Type.OPEN, "open a grave")) e.setCancelled(true);
    }

    /**
     * Charges for auto-looting a grave.
     *
     * @param e auto-loot event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAutoLoot(GraveAutoLootEvent e) {
        Player p = e.getPlayer();
        if (p == null) return;
        if (!chargeOrCancel(p, ChargeConfig.Type.AUTOLOOT, "auto-loot")) e.setCancelled(true);
    }

    /**
     * Charges for breaking a grave block.
     *
     * @param e break event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(GraveBreakEvent e) {
        Player p = e.getPlayer();
        if (!chargeOrCancel(p, ChargeConfig.Type.BLOCK_BREAK, "break a grave")) e.setCancelled(true);
    }

    /**
     * Computes the cost for an action and attempts to withdraw it.
     * Cancels the action (returns {@code false}) if disabled, exempt, or insufficient funds.
     *
     * @param p           player being charged
     * @param type        charge type
     * @param humanAction human-readable action for messages
     * @return {@code true} if action may proceed; {@code false} to cancel
     */
    private boolean chargeOrCancel(Player p, ChargeConfig.Type type, String humanAction) {
        ChargeConfig cfg = runtime.get();

        if (!cfg.isTypeEnabled(type)) {
            return true;
        }

        if (cfg.requirePermission(type) && !p.hasPermission(cfg.permissionNode(type))) {
            return true;
        }

        double balance = economy.getBalance(p);
        double cost = round(cfg.computeCost(type, p, balance), cfg.rounding());

        if (cost <= 0.0) {
            return false;
        }

        if (balance < cost) {
            sendMsg(p, cfg.msgInsufficient(cost, humanAction));
            return false;
        }

        EconomyResponse r = economy.withdrawPlayer(p, cost);
        if (r == null || !r.transactionSuccess()) {
            sendMsg(p, cfg.msgInsufficient(cost, humanAction));
            return false;
        }

        sendMsg(p, cfg.msgCharged(cost, humanAction));
        return true;
    }

    /**
     * Sends a colorized message to the player if non-empty.
     *
     * @param p   player
     * @param msg message with '&' color codes
     */
    private void sendMsg(Player p, String msg) {
        if (msg == null || msg.isEmpty()) return;
        if (plugin.getIntegrationManager().hasMiniMessage()) {
            p.sendMessage(MiniMessage.convertLegacyToMiniMessage(msg));
        } else {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    /**
     * Rounds a value to the given decimal places using HALF_UP.
     *
     * @param v      value
     * @param places number of decimal places; negative returns the original value
     * @return rounded value
     */
    private static double round(double v, int places) {
        if (places < 0) return v;
        return new BigDecimal(v).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}