package com.zerog.neoessentials.api;

import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;

/**
 * Functional interface for providing placeholder values.
 * Implementations should be thread-safe as they may be called from multiple threads.
 *
 * <p>Register via {@link PlaceholderAPI#registerPlaceholder(String, PlaceholderProvider)}:</p>
 * <pre>{@code
 * // Lambda (functional) usage
 * PlaceholderAPI.registerPlaceholder("mymod_health", (player, params) ->
 *     player != null ? String.valueOf((int) player.getHealth()) : "N/A"
 * );
 *
 * // Class implementation
 * public class HealthProvider implements PlaceholderProvider {
 *     {@literal @}Override
 *     public String onRequest(ServerPlayer player, String params) {
 *         return player != null ? String.valueOf((int) player.getHealth()) : "N/A";
 *     }
 * }
 * }</pre>
 *
 * @see PlaceholderAPI
 * @see PlaceholderExpansion
 */
@FunctionalInterface
public interface PlaceholderProvider {

    /**
     * Resolve a placeholder value for the given player and parameters.
     *
     * @param player The player context (can be null for server-wide placeholders)
     * @param params Optional parameters for the placeholder (can be null).
     *               Parameters are separated from the identifier by a colon in the text,
     *               e.g. {@code {mymod_stat:kills}} produces params={@code "kills"}.
     * @return The resolved placeholder value, or null if the placeholder cannot be resolved.
     *         Returning null leaves the original placeholder token in the text unchanged.
     */
    @Nullable
    String onRequest(@Nullable ServerPlayer player, @Nullable String params);
}

