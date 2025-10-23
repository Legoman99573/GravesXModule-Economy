package dev.cwhead.GravesX.modules.economy.integration;

import dev.cwhead.GravesX.module.ModuleContext;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class EconomyPlaceholders extends PlaceholderExpansion{

    private final ModuleContext ctx;
    private final FileConfiguration config;

    public EconomyPlaceholders(@NotNull ModuleContext ctx) {
        this.ctx = ctx;
        this.config = ctx.getConfig();
    }

    @Override
    public @NotNull String getIdentifier() {
        return ctx.getPlugin().getName().toLowerCase(Locale.ROOT);
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", ctx.getPlugin().getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return ctx.getPlugin().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.isEmpty()) return "";

        // enforce lowercase
        String lower = params.toLowerCase(Locale.ROOT).trim();

        if (lower.equals("currency_symbol") || lower.equals("currencysymbol") || lower.equals("currency-symbol")) {
            return getCurrencySymbol();
        }

        if (lower.endsWith("_cost_percentage")) {
            String type = lower.substring(0, lower.length() - "_cost_percentage".length());
            return getTypePercent(type);
        }

        if (lower.endsWith("_cost")) {
            String type = lower.substring(0, lower.length() - "_cost".length());
            return getTypeCostFormatted(type);
        }

        return "";
    }

    private String getCurrencySymbol() {
        return config.getString("economy.currency-symbol", "");
    }

    private String getTypeCostFormatted(String type) {
        ConfigurationSection section = getTypeSection(type);
        if (section == null) return "";

        ConfigurationSection charge = section.getConfigurationSection("charge");
        if (charge == null) return "";

        double fixed = charge.getDouble("fixed", Double.NaN);
        if (Double.isNaN(fixed)) return "";

        String symbol = getCurrencySymbol();
        int decimals = config.getInt("economy.round-to-decimals", 2);

        BigDecimal bd = BigDecimal.valueOf(fixed).setScale(decimals, RoundingMode.HALF_UP);
        String value = bd.stripTrailingZeros().toPlainString();

        return (symbol == null || symbol.isEmpty()) ? value : symbol + value;
    }

    private String getTypePercent(String type) {
        ConfigurationSection section = getTypeSection(type);
        if (section == null) return "";

        ConfigurationSection charge = section.getConfigurationSection("charge");
        if (charge == null) return "";

        double percent = charge.getDouble("percent", Double.NaN);
        if (Double.isNaN(percent)) return "";

        BigDecimal bd = BigDecimal.valueOf(percent).stripTrailingZeros();
        return bd.toPlainString() + "%";
    }

    private ConfigurationSection getTypeSection(String type) {
        if (type == null || type.isEmpty()) return null;
        ConfigurationSection types = config.getConfigurationSection("types");
        if (types == null) return null;

        String search = type.toLowerCase(Locale.ROOT);
        for (String key : types.getKeys(false)) {
            if (key.equalsIgnoreCase(search)) {
                return types.getConfigurationSection(key);
            }
        }
        return null;
    }
}
