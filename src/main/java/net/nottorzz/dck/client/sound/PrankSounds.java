package net.nottorzz.dck.client.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Comedy sound effects, built entirely from vanilla sound events so the mod
 * ships no audio files and adds no download weight.
 *
 * <p>The witch "celebrate" cackle stands in nicely for a cartoon laugh.</p>
 */
public final class PrankSounds {
    private PrankSounds() {
    }

    private static boolean soundsOn = true;

    public static void setEnabled(boolean enabled) {
        soundsOn = enabled;
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        if (!soundsOn) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null) {
            return;
        }
        client.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
    }

    /** Bright chime when the prank activates on join. */
    public static void activation() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.5f, 0.8f);
        play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 2.0f, 0.5f);
    }

    /** Subtle tick when the mouse first moves onto a button. */
    public static void hover() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f);
    }

    /** Firm click when a button is pressed. */
    public static void click() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.6f);
    }

    /** One beat of the pre-reveal countdown. {@code step} rises 0,1,2,... */
    public static void countdownTick(int step) {
        float pitch = 0.8f + step * 0.22f;
        play(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), pitch, 0.7f);
    }

    /** The cartoon cackle during the reveal. */
    public static void laugh() {
        play(SoundEvents.ENTITY_WITCH_CELEBRATE, 1.1f, 1.0f);
    }

    /** Dramatic "gotcha" hit shown with the big number. */
    public static void gotcha() {
        play(SoundEvents.ENTITY_WITHER_SPAWN, 1.4f, 0.35f);
        play(SoundEvents.ENTITY_WITCH_CELEBRATE, 0.9f, 0.9f);
    }

    /** Gentle, reassuring chime for the "your balance is safe" reveal. */
    public static void relief() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.33f, 0.7f);
        play(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 2.0f, 0.7f);
    }
}
