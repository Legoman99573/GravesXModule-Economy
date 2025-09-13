package dev.cwhead.GravesX.modules.economy;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.module.GravesXModule;
import dev.cwhead.GravesX.module.ModuleContext;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/**
 * Vault-backed economy module for GravesX.
 * <p>
 * Charges players for configured GravesX actions via a {@link VaultEconomyListener}.
 * Binds to a Vault {@link Economy} provider at startup or waits for a late
 * service registration if none is present yet.
 * </p>
 */
public final class VaultEconomyModule implements GravesXModule {

    /** Module context provided by the framework lifecycle. */
    private ModuleContext ctx;

    /** Cached Vault economy provider, once discovered. */
    private Economy economy;

    /** Listener that performs charges and cancels when funds are insufficient. */
    private VaultEconomyListener chargeListener;

    /** Temporary listener to detect late Economy service registration. */
    private VaultEconomyBootstrapListener bootstrapListener;

    /** Runtime charging configuration exposed as a service. */
    private EconomyRuntime runtime;

    /**
     * Loads the module and ensures a default config exists.
     *
     * @param ctx module context
     */
    @Override
    public void onModuleLoad(ModuleContext ctx) {
        this.ctx = ctx;
        ctx.saveDefaultConfig();
    }

    /**
     * Enables the module, publishing {@link EconomyRuntime} and hooking Vault.
     * <ul>
     *   <li>If disabled via {@code enabled=false}, the method logs and returns.</li>
     *   <li>If a Vault {@link Economy} provider is not yet present, a bootstrap
     *       listener is registered to wait for one.</li>
     *   <li>Otherwise, the charge listener is registered immediately.</li>
     * </ul>
     *
     * @param ctx module context
     */
    @Override
    public void onModuleEnable(ModuleContext ctx) {
        this.ctx = ctx;

        if (!ctx.getConfig().getBoolean("enabled", true)) {
            ctx.getLogger().info("[Economy-Vault] Module disabled via config.");
            return;
        }

        this.runtime = new EconomyRuntime(new ChargeConfig(ctx.getConfig()));
        ctx.registerService(EconomyRuntime.class, runtime, ServicePriority.Normal);

        // Defer hook to the server thread.
        ctx.runTask(() -> {
            if (tryHookEconomy()) {
                ctx.getLogger().warning("[Economy-Vault] Vault found but no Economy provider yet. Waiting for service registration...");
                this.bootstrapListener = ctx.registerListener(new VaultEconomyBootstrapListener(this::onEconomyAvailable));
            } else {
                onEconomyAvailable();
            }
        });
    }

    /**
     * Disables the module and releases references.
     * <p>
     * Listeners registered via {@link ModuleContext#registerListener(Listener)} are
     * expected to be unregistered by the framework; here we null references only.
     * </p>
     *
     * @param ctx module context
     */
    @Override
    public void onModuleDisable(ModuleContext ctx) {
        this.chargeListener = null;
        this.bootstrapListener = null;
        this.economy = null;
        this.runtime = null;
    }

    /**
     * Attempts to resolve and cache a Vault {@link Economy} provider.
     *
     * @return {@code false} if a provider was found and cached (hook succeeded);
     *         {@code true} if no provider is available yet or an error occurred (must wait).
     */
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

    /**
     * Finalizes setup once a Vault {@link Economy} provider is available.
     * <p>
     * Registers the {@link VaultEconomyListener} and logs the hooked provider name.
     * If a provider still cannot be resolved, logs a severe message and aborts.
     * </p>
     */
    private void onEconomyAvailable() {
        if (this.economy == null && tryHookEconomy()) {
            ctx.getLogger().severe("[Economy-Vault] Vault Economy provider still not available.");
            return;
        }
        Graves plugin = ctx.getPlugin();
        this.chargeListener = ctx.registerListener(new VaultEconomyListener(plugin, economy, runtime));
        ctx.getLogger().info("[Economy-Vault] Hooked Vault Economy: " + economy.getName());
    }
}
