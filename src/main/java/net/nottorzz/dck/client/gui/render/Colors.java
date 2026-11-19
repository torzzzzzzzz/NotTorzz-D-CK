package net.nottorzz.dck.client.gui.render;

/**
 * The mod's colour palette (premium dark theme: near-black background,
 * red + white accents) plus small ARGB helpers.
 *
 * <p>All colours are 0xAARRGGBB integers, the format Minecraft's
 * {@code DrawContext} expects.</p>
 */
public final class Colors {
    private Colors() {
    }

    // Backgrounds
    public static final int BG_TOP      = 0xFF0B0B0F; // near-black (slightly blue)
    public static final int BG_BOTTOM   = 0xFF050507; // deeper black
    public static final int PANEL       = 0xF2141418; // card fill (slightly translucent)
    public static final int PANEL_LIGHT = 0xFF1C1C22; // raised element
    public static final int PANEL_EDGE  = 0xFF2A2A33; // hairline divider

    // Accents
    public static final int RED         = 0xFFE02030; // primary red
    public static final int RED_DEEP    = 0xFF8C1420; // pressed / deep red
    public static final int RED_SOFT    = 0x55E02030; // red glow
    public static final int WHITE       = 0xFFF4F4F8; // primary text
    public static final int GREY        = 0xFF9A9AA6; // muted text
    public static final int GREY_DIM    = 0xFF55555F; // very muted text
    public static final int GREEN       = 0xFF32D66A; // "you're safe" accent

    /** Pack ARGB components (0-255) into a single int. */
    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /** Replace the alpha channel of a colour (alpha 0-255). */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Replace the alpha channel of a colour (alpha 0.0-1.0). */
    public static int withAlpha(int color, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return withAlpha(color, a);
    }

    /** Linearly interpolate between two ARGB colours. {@code t} is clamped to [0,1]. */
    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return argb(ra, rr, rg, rb);
    }
}
