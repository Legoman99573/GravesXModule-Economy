package dev.cwhead.GravesX.modules.economy;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.GravesXModule;
import dev.cwhead.GravesX.module.ModuleContext;
import dev.cwhead.GravesX.modules.economy.integration.EconomyPlaceholders;
import dev.cwhead.GravesX.modules.economy.util.I18n;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/**
 * GravesX Vault economy module with I18n support.
 */
public final class VaultEconomyModule extends GravesXModule {

    private ModuleContext ctx;
    private Economy economy;
    private VaultEconomyListener listener;
    private VaultEconomyBootstrapListener bootstrapListener;
    private EconomyRuntime runtime;
    private I18n i18n;
    private EconomyPlaceholders economyPlaceholders;

    @Override
    public void onModuleLoad(ModuleContext ctx) {
        this.ctx = ctx;
        ctx.saveDefaultConfig();
    }

    @Override
    public void onModuleEnable(ModuleContext ctx) {
        this.ctx = ctx;
        if (!ctx.getConfig().getBoolean("enabled", true)) {
            ctx.getLogger().info("[Economy-Vault] Module disabled via config.");
            ctx.getGravesXModules().disableModule();
            return;
        }

        this.runtime = new EconomyRuntime(new ChargeConfig(ctx.getConfig()));
        ctx.registerService(EconomyRuntime.class, runtime, ServicePriority.Normal);

        String defaultLang = ctx.getConfig().getString("default-language", "en_us");
        this.i18n = new I18n(ctx.getPlugin(), defaultLang);

        ctx.runTask(() -> {
            if (tryHookEconomy()) {
                ctx.getLogger().warning("[Economy-Vault] Vault found but no provider yet, waiting...");
                this.bootstrapListener = ctx.registerListener(new VaultEconomyBootstrapListener(this::onEconomyAvailable));
            } else {
                onEconomyAvailable();
            }
        });
    }

    @Override
    public void onModuleDisable(ModuleContext ctx) {
        this.listener = null;
        this.bootstrapListener = null;
        this.economy = null;
        this.runtime = null;
        this.i18n = null;
        this.economyPlaceholders = null;
    }

    private boolean tryHookEconomy() {
        try {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) return true;
            this.economy = rsp.getProvider();
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

    private void onEconomyAvailable() {
        if (this.economy == null && tryHookEconomy()) {
            ctx.getLogger().severe("[Economy-Vault] Vault provider still missing. Disabling module.");
            ctx.getGravesXModules().disableModule();
            return;
        }
        Graves plugin = ctx.getPlugin();
        this.listener = ctx.registerListener(new VaultEconomyListener(plugin, economy, runtime, i18n));
        ctx.getLogger().info("[Economy-Vault] Hooked Vault Economy: " + economy.getName());
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");

        if (placeholderAPI != null && placeholderAPI.isEnabled()) {
            try {
                this.economyPlaceholders = new EconomyPlaceholders(ctx);
                economyPlaceholders.register();
                ctx.getLogger().info("[Economy-Vault] Hooked into " + placeholderAPI.getName() + " v." + placeholderAPI.getDescription().getVersion());
                ctx.getLogger().info("[Economy-Vault] PlaceholderAPI expansion registered: gravesx_<type>_cost and gravesx_<type>_cost_percentage");
            } catch (Throwable t) {
                ctx.getLogger().info("[Economy-Vault] Failed to hook into " + placeholderAPI.getName() + " v." + placeholderAPI.getDescription().getVersion() + ". Placeholders will not work.");
                ctx.getPlugin().logStackTrace(t);
            }
        }
    }
}