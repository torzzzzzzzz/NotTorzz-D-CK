package net.nottorzz.dck.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.nottorzz.dck.NotTorzzDck;
import net.nottorzz.dck.client.config.PrankConfig;
import net.nottorzz.dck.client.gui.PrankScreen;

import java.io.File;

/**
 * Screenshot harness — <b>not</b> part of normal gameplay.
 *
 * <p>Only runs when the environment variable {@code NOTTORZZ_DEVPREVIEW=1} is
 * set (which only happens in the automated build/screenshot pipeline). It opens
 * the prank interface, steps it through every visual phase and saves real
 * framebuffer PNGs. It is a no-op for every real player.</p>
 */
public final class DevPreview {

    public static boolean active() {
        return "1".equals(System.getenv("NOTTORZZ_DEVPREVIEW"));
    }

    private int ticks = 0;
    private PrankScreen screen;
    private final PrankConfig config;

    public DevPreview(PrankConfig config) {
        this.config = config;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.getFramebuffer() == null) {
            return;
        }
        ticks++;
        switch (ticks) {
            case 70 -> capture(client, "00_title");
            case 80 -> {
                screen = new PrankScreen(13_370_000d, true, config);
                client.setScreen(screen);
            }
            case 108 -> capture(client, "01_menu");
            case 120 -> { if (screen != null) screen.devForceReveal(true); }
            case 150 -> capture(client, "02_reveal_dontpay");
            case 165 -> { if (screen != null) screen.devForceReveal(false); }
            case 195 -> capture(client, "03_reveal_pay");
            case 210 -> { if (screen != null) screen.devForceSafe(); }
            case 238 -> capture(client, "04_safe");
            case 250 -> { if (screen != null) screen.devForceCountdown(true); }
            case 272 -> capture(client, "05_countdown");
            case 300 -> NotTorzzDck.LOGGER.info("[NotTorzz][devpreview] capture sequence complete");
            default -> { }
        }
    }

    private void capture(MinecraftClient client, String name) {
        try {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            ScreenshotRecorder.saveScreenshot(gameDir, name + ".png", client.getFramebuffer(), 1, text -> { });
            NotTorzzDck.LOGGER.info("[NotTorzz][devpreview] saved {}", name);
        } catch (Throwable t) {
            NotTorzzDck.LOGGER.warn("[NotTorzz][devpreview] capture failed for {}: {}", name, t.toString());
        }
    }
}
