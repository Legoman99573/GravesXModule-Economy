package dev.cwhead.GravesX.modules.economy.command;

import dev.cwhead.GravesX.module.ModuleContext;
import dev.cwhead.GravesX.module.command.GravesXModuleCommand;
import dev.cwhead.GravesX.modules.economy.ChargeConfig;
import dev.cwhead.GravesX.modules.economy.EconomyRuntime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Implements {@code /graveecon reload} to hot-reload the Economy-Vault module config.
 * <p>
 * Requires {@code graves.economy.reload}. After reloading the file via
 * {@link ModuleContext#reloadConfig()}, publishes the new {@link ChargeConfig}
 * into the shared {@link EconomyRuntime} (if available) so listeners pick up changes.
 * </p>
 */
public final class EconReloadCommand implements GravesXModuleCommand {

    /** Module context used for config access and reloads. */
    private final ModuleContext ctx;

    /**
     * Creates the reload command bound to a module context.
     *
     * @param ctx module context
     */
    public EconReloadCommand(ModuleContext ctx) { this.ctx = ctx; }

    /** {@inheritDoc} */
    @Override public String getName() {
        return "graveecon";
    }

    /** {@inheritDoc} */
    @Override public String getDescription() {
        return "Reload Economy-Vault config.";
    }

    /** {@inheritDoc} */
    @Override public String getUsage() {
        return "/graveecon reload";
    }

    /** {@inheritDoc} */
    @Override public String getPermission() {
        return "graves.economy.reload";
    }

    /**
     * Handles {@code /graveecon reload}.
     * <ul>
     *   <li>Checks permission.</li>
     *   <li>Validates subcommand usage.</li>
     *   <li>Reloads the module config and updates the shared {@link EconomyRuntime}.</li>
     * </ul>
     *
     * @param sender command sender
     * @param command command being executed
     * @param label alias used
     * @param args arguments; expects single {@code reload}
     * @return always {@code true} (command handled)
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: " + getUsage());
            return true;
        }

        ctx.reloadConfig();
        ChargeConfig fresh = new ChargeConfig(ctx.getConfig());

        EconomyRuntime runtime = Bukkit.getServicesManager().load(EconomyRuntime.class);
        if (runtime != null) {
            runtime.set(fresh);
            sender.sendMessage(ChatColor.GREEN + "[Economy-Vault] Config reloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "[Economy-Vault] Runtime not available; is the module enabled?");
        }
        return true;
    }
}