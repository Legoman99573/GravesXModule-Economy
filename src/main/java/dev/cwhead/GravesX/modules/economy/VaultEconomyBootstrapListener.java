package dev.cwhead.GravesX.modules.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;

/**
 * Listener that waits for a Vault {@link Economy} provider to be registered,
 * then invokes a supplied callback. Useful when Vault loads before the economy
 * plugin registers its service.
 */
final class VaultEconomyBootstrapListener implements Listener {

    /**
     * Callback invoked once an {@link Economy} service is observed.
     */
    interface HookCallback {
        /**
         * Called when a Vault Economy provider becomes available.
         */
        void hooked();
    }

    /** Callback to run after hook detection. */
    private final HookCallback callback;

    /**
     * Creates a bootstrap listener that triggers the given callback when an
     * {@link Economy} service is registered.
     *
     * @param callback action to run once hooked
     */
    VaultEconomyBootstrapListener(HookCallback callback) {
        this.callback = callback;
    }

    /**
     * Monitors newly registered services and triggers the callback when the
     * service type is {@link Economy}.
     *
     * @param e service registration event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceRegister(ServiceRegisterEvent e) {
        if (e.getProvider().getService() == Economy.class) {
            callback.hooked();
        }
    }
}