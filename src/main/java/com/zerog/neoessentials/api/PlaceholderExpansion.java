package com.zerog.neoessentials.api;

import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Abstract base class for placeholder expansions.
 * An expansion groups many related placeholders under a common identifier prefix.
 *
 * <p>All placeholders registered through an expansion use the format
 * {@code {expansionId_placeholderName}}, for example an expansion with id {@code "mymod"}
 * providing placeholder {@code "kills"} is accessed as {@code {mymod_kills}}.</p>
 *
 * <h3>Example usage from an external mod:</h3>
 * <pre>{@code
 * public class MyModExpansion extends PlaceholderExpansion {
 *
 *     {@literal @}Override public String getIdentifier() { return "mymod"; }
 *     {@literal @}Override public String getVersion()    { return "1.0.0"; }
 *     {@literal @}Override public String getAuthor()     { return "YourName"; }
 *
 *     {@literal @}Override
 *     public Set<String> getPlaceholders() {
 *         return Set.of("kills", "deaths", "playtime");
 *     }
 *
 *     {@literal @}Override
 *     public String onPlaceholderRequest(ServerPlayer player, String identifier, String params) {
 *         if (player == null) return null;
 *         return switch (identifier) {
 *             case "kills"    -> String.valueOf(StatsManager.getKills(player.getUUID()));
 *             case "deaths"   -> String.valueOf(StatsManager.getDeaths(player.getUUID()));
 *             case "playtime" -> StatsManager.getPlaytime(player.getUUID());
 *             default         -> null;
 *         };
 *     }
 * }
 *
 * // Registration (call during your mod's init or server starting):
 * PlaceholderAPI.registerExpansion(new MyModExpansion());
 * }</pre>
 *
 * @see PlaceholderAPI
 * @see PlaceholderProvider
 * @see PlaceholderManager
 */
public abstract class PlaceholderExpansion {

    /**
     * Get the unique identifier for this expansion.
     * This is used as the prefix for all placeholders in this expansion.
     * Must be lowercase, alphanumeric (underscores allowed).
     *
     * @return The expansion identifier (e.g., "neoessentials", "mymod")
     */
    public abstract String getIdentifier();

    /**
     * Get the version of this expansion.
     *
     * @return The expansion version string (e.g., "1.0.0")
     */
    public abstract String getVersion();

    /**
     * Get the author/owner of this expansion.
     *
     * @return The expansion author (e.g., "YourName" or "YourMod Team")
     */
    public abstract String getAuthor();

    /**
     * Called when a placeholder from this expansion is requested.
     * The {@code identifier} parameter is the part <em>after</em> the expansion prefix underscore.
     * For example, if the full placeholder is {@code {mymod_kills}}, {@code identifier} = {@code "kills"}.
     *
     * @param player     The player context (can be null for server-wide placeholders)
     * @param identifier The placeholder identifier without expansion prefix
     * @param params     Optional colon-separated parameters (can be null)
     * @return The resolved placeholder value, or null if not handled by this expansion.
     *         Returning null leaves the original token in the text unchanged.
     */
    @Nullable
    public abstract String onPlaceholderRequest(@Nullable ServerPlayer player,
                                                String identifier,
                                                @Nullable String params);

    /**
     * Get all placeholder identifiers that this expansion provides.
     * These should NOT include the expansion prefix — just the suffix portion
     * (e.g. "kills", not "mymod_kills").
     *
     * @return An unmodifiable set of placeholder identifiers
     */
    public abstract Set<String> getPlaceholders();
}

