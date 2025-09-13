package dev.cwhead.GravesX.modules.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and evaluates charging rules for GravesX economy actions.
 * <p>
 * Supports fixed fees or a percentage of the player's balance, optional
 * permission-based cost overrides, message templates, and rounding.
 * </p>
 */
public final class ChargeConfig {

    /**
     * Charging mode.
     */
    enum Mode {
        /** Charge a fixed amount. */
        FIXED,
        /** Charge a percentage of the player's current balance. */
        PERCENT_BALANCE
    }

    /**
     * Chargeable action types.
     */
    enum Type {
        /** GravePreTeleportEvent. */
        TELEPORT,
        /** GraveOpenEvent */
        OPEN,
        /** GraveAutoLootEvent */
        AUTOLOOT,
        /** GraveBreakEvent */
        BLOCK_BREAK
    }

    private final FileConfiguration cfg;

    /**
     * Creates a new config view backed by a Bukkit {@link FileConfiguration}.
     *
     * @param cfg configuration source
     */
    public ChargeConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    /**
     * Whether charging is enabled for the given action type.
     *
     * @param t action type
     * @return {@code true} if enabled (default), otherwise {@code false}
     */
    boolean isTypeEnabled(Type t) {
        return cfg.getBoolean(path(t, "enabled"), true);
    }

    /**
     * Whether a permission is required for a player to be subject to charge.
     * <p>If {@code true} and the player lacks the node, they are exempt.</p>
     *
     * @param t action type
     * @return {@code true} if permission is required (default), else {@code false}
     */
    boolean requirePermission(Type t) {
        return cfg.getBoolean(path(t, "require-permission"), true);
    }

    /**
     * Returns the permission node associated with the action type.
     *
     * @param t action type
     * @return permission node string (default: {@code graves.economy.<key>})
     */
    String permissionNode(Type t) {
        String def = "graves.economy." + key(t);
        return cfg.getString(path(t, "permission"), def);
    }

    /**
     * Number of decimal places to round monetary values to.
     *
     * @return non-negative decimal places (default {@code 2})
     */
    int rounding() {
        return Math.max(0, cfg.getInt("economy.round-to-decimals", 2));
    }

    /**
     * Currency symbol used in messages.
     *
     * @return currency symbol (default {@code "$"})
     */
    String currency() {
        return cfg.getString("economy.currency-symbol", "$");
    }

    /**
     * Computes the charge for an action, applying permission overrides if enabled.
     * <ul>
     *   <li>If {@code overrides.from-permission} is true (default), scans the
     *       player's effective permissions for a pattern (default:
     *       {@code graves.economy.<key>.cost.(?<amount>\\d+(?:\\.\\d{1,2})?)})
     *       and uses the captured {@code amount} if present.</li>
     *   <li>Otherwise, uses the configured mode: fixed amount or {@code percent}
     *       of the provided {@code balance}.</li>
     *   <li>Result is clamped to be non-negative.</li>
     * </ul>
     *
     * @param t       action type
     * @param p       player (for permission overrides)
     * @param balance player's current balance (used for percent mode)
     * @return computed non-negative cost
     */
    double computeCost(Type t, Player p, double balance) {
        if (cfg.getBoolean(path(t, "overrides.from-permission"), true)) {
            String rx = cfg.getString(path(t, "overrides.pattern"),
                    "graves\\." + "economy\\." + key(t) + "\\.cost\\.(?<amount>\\d+(?:\\.\\d{1,2})?)");
            Pattern pat = Pattern.compile(rx, Pattern.CASE_INSENSITIVE);
            for (org.bukkit.permissions.PermissionAttachmentInfo pai : p.getEffectivePermissions()) {
                if (!pai.getValue()) continue;
                Matcher m = pat.matcher(pai.getPermission());
                if (m.matches()) {
                    try {
                        return Double.parseDouble(m.group("amount"));
                    } catch (Throwable ignored) {}
                }
            }
        }

        Mode mode = Mode.valueOf(cfg.getString(path(t, "charge.mode"), "FIXED").toUpperCase(Locale.ROOT));
        return switch (mode) {
            case FIXED -> Math.max(0.0, cfg.getDouble(path(t, "charge.fixed"), 0.0));
            case PERCENT_BALANCE -> {
                double pct = cfg.getDouble(path(t, "charge.percent"), 0.0);
                yield Math.max(0.0, balance * (pct / 100.0));
            }
        };
    }

    /**
     * Formats the "charged" message.
     *
     * @param amount      charge amount
     * @param actionWord  human-readable action (e.g., "teleport")
     * @return color-code string with %currency%, %amount%, %type% replaced
     */
    String msgCharged(double amount, String actionWord) {
        String s = cfg.getString("messages.charged", "&cCharged %currency%%amount% for %type%.");
        return s.replace("%currency%", currency()).replace("%amount%", fmt(amount)).replace("%type%", actionWord);
    }

    /**
     * Formats the "insufficient funds" message.
     *
     * @param amount      required amount
     * @param actionWord  human-readable action
     * @return color-code string with placeholders resolved
     */
    String msgInsufficient(double amount, String actionWord) {
        String s = cfg.getString("messages.insufficient", "&cYou need %currency%%amount% to %type%.");
        return s.replace("%currency%", currency()).replace("%amount%", fmt(amount)).replace("%type%", actionWord);
    }

    /**
     * Maps a type to its short configuration key.
     *
     * @param t action type
     * @return key used in defaults and messages
     */
    private static String key(Type t) {
        return switch (t) {
            case TELEPORT -> "teleport";
            case OPEN -> "open";
            case AUTOLOOT -> "autoloot";
            case BLOCK_BREAK -> "block_break";
        };
    }

    /**
     * Builds a configuration path under {@code types.<TYPE>.tail}.
     *
     * @param t    action type
     * @param tail suffix key
     * @return full config path
     */
    private static String path(Type t, String tail) {
        return "types." + t.name() + "." + tail;
    }

    /**
     * Formats a number using the configured rounding and strips trailing zeros.
     *
     * @param d value
     * @return plain string representation (no scientific notation)
     */
    private String fmt(double d) {
        int places = rounding();
        java.math.BigDecimal bd = new java.math.BigDecimal(d).setScale(places, java.math.RoundingMode.HALF_UP);
        return bd.stripTrailingZeros().toPlainString();
    }
}