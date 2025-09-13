package dev.cwhead.GravesX.modules.economy.command;

import dev.cwhead.GravesX.module.command.GravesXModuleTabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tab completer for the {@code /graveecon} command.
 * <p>
 * Provides a single subcommand suggestion: {@code reload}.
 * </p>
 */
public final class EconReloadTab implements GravesXModuleTabCompleter {

    /**
     * Suggests {@code reload} for the first argument if it matches the user's partial input.
     * Returns an empty list for all other positions.
     *
     * @param sender the command sender
     * @param command the command being executed
     * @param alias the alias used
     * @param args the command arguments
     * @return a list containing {@code reload} when appropriate; otherwise an empty list
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], Collections.singletonList("reload"), out);
            return out;
        }
        return Collections.emptyList();
    }
}