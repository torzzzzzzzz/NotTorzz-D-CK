package net.nottorzz.dck.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.nottorzz.dck.NotTorzzDck;

import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Best-effort, <b>read-only</b> reader for the player's publicly displayed
 * balance.
 *
 * <p>It never asks the server for anything and never runs a command. It simply
 * looks at text the game has already received and drawn:</p>
 * <ul>
 *   <li>chat / system messages that mention a balance, and</li>
 *   <li>the sidebar scoreboard (scanned about once a second).</li>
 * </ul>
 *
 * <p>If nothing balance-like is visible, {@link #current()} is empty and the
 * prank falls back to a configurable demo number. Because different servers
 * display money differently this is intentionally forgiving, and every
 * scoreboard access is guarded so an unexpected API shape can never crash the
 * game.</p>
 */
public class BalanceReader {

    private double lastBalance = -1;
    private long lastUpdateMs = 0;
    private long lastScoreboardScan = 0;

    /** Current detected balance, if we have one. */
    public OptionalDouble current() {
        return lastBalance >= 0 ? OptionalDouble.of(lastBalance) : OptionalDouble.empty();
    }

    public boolean hasBalance() {
        return lastBalance >= 0;
    }

    public void reset() {
        lastBalance = -1;
        lastUpdateMs = 0;
    }

    /** Feed an incoming chat / system message. */
    public void onMessage(Text message) {
        if (message == null) {
            return;
        }
        String s = message.getString();
        if (!looksLikeBalance(s)) {
            return;
        }
        OptionalDouble v = Money.parseLargest(s);
        if (v.isPresent()) {
            set(v.getAsDouble());
        }
    }

    /** Periodically scan the sidebar scoreboard (cheap, ~1 Hz). */
    public void tickScoreboard(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastScoreboardScan < 1000) {
            return;
        }
        lastScoreboardScan = now;
        if (client == null || client.world == null) {
            return;
        }
        try {
            Scoreboard sb = client.world.getScoreboard();
            ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (obj == null) {
                return;
            }
            StringBuilder text = new StringBuilder();
            text.append(obj.getDisplayName().getString()).append(' ');
            for (ScoreboardEntry entry : sb.getScoreboardEntries(obj)) {
                Text name = entry.name();
                if (name != null) {
                    text.append(name.getString()).append(' ');
                }
            }
            String joined = text.toString();
            if (looksLikeBalance(joined)) {
                OptionalDouble v = Money.parseLargest(joined);
                // Only trust the scoreboard if chat hasn't given us something newer.
                if (v.isPresent() && (now - lastUpdateMs) > 1500) {
                    set(v.getAsDouble());
                }
            }
        } catch (Throwable t) {
            // Server scoreboard shapes vary; never let reading one break the game.
            NotTorzzDck.LOGGER.debug("[NotTorzz] scoreboard scan skipped: {}", t.toString());
        }
    }

    private void set(double value) {
        lastBalance = value;
        lastUpdateMs = System.currentTimeMillis();
    }

    private static boolean looksLikeBalance(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        boolean hasDigit = false;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                hasDigit = true;
                break;
            }
        }
        if (!hasDigit) {
            return false;
        }
        String l = s.toLowerCase(Locale.ROOT);
        return l.indexOf('$') >= 0
                || l.contains("balance")
                || l.contains("bal:")
                || l.contains("money")
                || l.contains("coins")
                || l.contains("worth");
    }
}
