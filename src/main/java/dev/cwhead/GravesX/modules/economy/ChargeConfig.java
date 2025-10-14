package dev.cwhead.GravesX.modules.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Handles charge computation and rounding for GravesX economy actions.
 */
public final class ChargeConfig {

    public enum Mode { FIXED, PERCENT_BALANCE }
    public enum Type { TELEPORT, OPEN, AUTOLOOT, BLOCK_BREAK }

    private final FileConfiguration cfg;

    public ChargeConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    /** Is this charge type enabled? */
    public boolean isTypeEnabled(Type t) {
        return cfg.getBoolean(path(t, "enabled"), true);
    }

    /** Number of decimal places to round monetary values */
    public int rounding() {
        return Math.max(0, cfg.getInt("economy.round-to-decimals", 2));
    }

    /** Currency symbol */
    public String currency() {
        return cfg.getString("economy.currency-symbol", "$");
    }

    /** Compute the cost for a given player and type */
    public double computeCost(Type t, Player p, double balance) {
        Mode mode = Mode.valueOf(cfg.getString(path(t, "charge.mode"), "FIXED").toUpperCase(Locale.ROOT));
        return switch (mode) {
            case FIXED -> Math.max(0.0, cfg.getDouble(path(t, "charge.fixed"), 0.0));
            case PERCENT_BALANCE -> {
                double pct = cfg.getDouble(path(t, "charge.percent"), 0.0);
                yield Math.max(0.0, balance * (pct / 100.0));
            }
        };
    }

    /** Build config path for a type */
    private static String path(Type t, String tail) {
        return "types." + t.name() + "." + tail;
    }

    /** Format a number using rounding */
    public String fmt(double d) {
        int places = rounding();
        BigDecimal bd = new BigDecimal(d).setScale(places, RoundingMode.HALF_UP);
        return bd.stripTrailingZeros().toPlainString();
    }
}