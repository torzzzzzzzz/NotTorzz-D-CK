package net.nottorzz.dck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for the NotTorzz D*CK prank mod.
 *
 * <p>This class holds nothing but identifiers and a logger. All of the actual
 * behaviour lives in the {@code client} sub-package, because this is a
 * client-only cosmetic mod: it never runs on a server, never sends a command
 * and never moves any in-game currency.</p>
 */
public final class NotTorzzDck {
    public static final String MOD_ID = "nottorzz_dck";
    public static final String MOD_NAME = "NotTorzz D*CK";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private NotTorzzDck() {
    }
}
