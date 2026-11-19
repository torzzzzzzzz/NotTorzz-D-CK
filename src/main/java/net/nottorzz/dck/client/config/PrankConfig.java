package net.nottorzz.dck.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.nottorzz.dck.NotTorzzDck;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User-editable settings, stored as {@code config/nottorzz_dck.json}.
 *
 * <p>Every option here is cosmetic. Nothing in this file can cause a command to
 * be sent or currency to move — there is intentionally no such capability
 * anywhere in the mod.</p>
 */
public class PrankConfig {

    /** Master on/off switch for the whole prank. */
    public boolean enabled = true;

    /** Automatically open the prank interface a moment after joining DonutSMP. */
    public boolean autoOpenOnJoin = true;

    /** Play the (vanilla) comedy sound effects. */
    public boolean playSounds = true;

    /**
     * Flashing red/white strobe during the reveal.
     * <p><b>Accessibility:</b> set to {@code false} to disable all flashing for
     * photosensitive users. The prank still works, just without the strobe.</p>
     */
    public boolean flashingEffects = true;

    /** Fraction of the detected balance shown as the dramatic "fake" amount. */
    public double payPercent = 0.90;

    /** Case-insensitive substring matched against the server address to detect DonutSMP. */
    public String serverMatch = "donutsmp";

    /** Fallback amount used only when no real balance could be read from the HUD. */
    public double demoBalance = 1_000_000.0;

    /** Ticks to wait after join before the activation fires (20 ticks = 1 second). */
    public int activationDelayTicks = 40;

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(NotTorzzDck.MOD_ID + ".json");
    }

    /** Load the config, creating a default file if none exists. Never throws. */
    public static PrankConfig load() {
        Path p = path();
        PrankConfig cfg = new PrankConfig();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    PrankConfig loaded = GSON.fromJson(r, PrankConfig.class);
                    if (loaded != null) {
                        cfg = loaded;
                    }
                }
            } else {
                cfg.save();
            }
        } catch (Exception e) {
            NotTorzzDck.LOGGER.warn("[NotTorzz] Could not read config, using defaults", e);
        }
        cfg.sanitise();
        return cfg;
    }

    /** Write the config to disk. Never throws. */
    public void save() {
        sanitise();
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            NotTorzzDck.LOGGER.warn("[NotTorzz] Could not write config", e);
        }
    }

    private void sanitise() {
        if (payPercent < 0) payPercent = 0;
        if (payPercent > 1) payPercent = 1;
        if (serverMatch == null || serverMatch.isBlank()) serverMatch = "donutsmp";
        if (demoBalance < 0) demoBalance = 0;
        if (activationDelayTicks < 0) activationDelayTicks = 0;
        if (activationDelayTicks > 200) activationDelayTicks = 200;
    }
}
