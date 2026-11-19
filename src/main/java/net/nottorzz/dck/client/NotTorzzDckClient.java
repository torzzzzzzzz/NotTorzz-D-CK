package net.nottorzz.dck.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.nottorzz.dck.NotTorzzDck;
import net.nottorzz.dck.client.config.PrankConfig;
import net.nottorzz.dck.client.gui.PrankScreen;
import net.nottorzz.dck.client.sound.PrankSounds;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Client entry point. Wires up:
 * <ul>
 *   <li>DonutSMP join detection,</li>
 *   <li>the activation notification + auto-opened interface,</li>
 *   <li>passive balance reading, and</li>
 *   <li>a configurable keybind to reopen the interface.</li>
 * </ul>
 *
 * <p>It never registers a server-side anything, never sends a packet of its own
 * and never issues a command. Everything is observe-and-draw.</p>
 */
public class NotTorzzDckClient implements ClientModInitializer {

    private PrankConfig config;
    private final BalanceReader balanceReader = new BalanceReader();
    private KeyBinding openKey;

    private int activationTicksLeft = -1;
    private boolean activatedThisConnection = false;
    private DevPreview devPreview;

    @Override
    public void onInitializeClient() {
        config = PrankConfig.load();
        PrankSounds.setEnabled(config.playSounds);

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nottorzz_dck.open_menu",
                GLFW.GLFW_KEY_UNKNOWN,            // unbound by default; set it in Controls
                KeyBinding.Category.MISC));

        // Detect joining DonutSMP and arm a short, deferred activation.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (config.enabled && !activatedThisConnection && isDonutSmp(client)) {
                activationTicksLeft = config.activationDelayTicks;
                NotTorzzDck.LOGGER.info("[NotTorzz] DonutSMP detected — prank armed.");
            }
        });

        // Reset on disconnect so reconnecting re-triggers the gag.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            activatedThisConnection = false;
            activationTicksLeft = -1;
            balanceReader.reset();
        });

        // Passively read balance from chat / system messages.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> balanceReader.onMessage(message));

        // Per-tick: keybind, scoreboard scan, deferred activation.
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        if (DevPreview.active()) {
            devPreview = new DevPreview(config);
            NotTorzzDck.LOGGER.info("[NotTorzz] dev-preview screenshot harness enabled.");
        }

        NotTorzzDck.LOGGER.info("[NotTorzz] {} loaded (cosmetic-only).", NotTorzzDck.MOD_NAME);
    }

    private void onClientTick(MinecraftClient client) {
        if (devPreview != null) {
            devPreview.tick(client);
        }
        if (openKey != null) {
            while (openKey.wasPressed()) {
                openScreen(client);
            }
        }

        balanceReader.tickScoreboard(client);

        if (activationTicksLeft > 0) {
            activationTicksLeft--;
        } else if (activationTicksLeft == 0) {
            activationTicksLeft = -1;
            activatedThisConnection = true;
            activate(client);
        }
    }

    private void activate(MinecraftClient client) {
        if (client == null) {
            return;
        }
        PrankSounds.activation();

        // On-screen notification using the vanilla title system (animated, reliable).
        if (client.inGameHud != null) {
            client.inGameHud.setTitleTicks(5, 55, 15);
            client.inGameHud.setTitle(Text.literal("NotTorzz D*CK").formatted(Formatting.RED, Formatting.BOLD));
            client.inGameHud.setSubtitle(Text.literal("has activated.").formatted(Formatting.WHITE));
        }

        if (config.autoOpenOnJoin && client.currentScreen == null) {
            openScreen(client);
        }
    }

    private void openScreen(MinecraftClient client) {
        if (client == null) {
            return;
        }
        OptionalDouble bal = balanceReader.current();
        double value = bal.orElse(config.demoBalance);
        boolean known = bal.isPresent();
        client.setScreen(new PrankScreen(value, known, config));
    }

    private boolean isDonutSmp(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null || info.address == null) {
            return false; // singleplayer / realms / no server entry
        }
        String addr = info.address.toLowerCase(Locale.ROOT);
        return addr.contains(config.serverMatch.toLowerCase(Locale.ROOT));
    }
}
