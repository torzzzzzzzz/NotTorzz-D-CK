package net.nottorzz.dck.client.gui.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * Low-level rendering helpers built on vanilla {@link DrawContext} primitives.
 *
 * <p>Everything here is drawn with plain rectangle fills and the font renderer,
 * so the mod ships <b>zero</b> binary texture assets and stays lightweight.
 * Rounded corners, gradients, glows and oversized text are all synthesised
 * procedurally.</p>
 */
public final class Draw {
    private Draw() {
    }

    // ------------------------------------------------------------------
    // Rounded rectangles
    // ------------------------------------------------------------------

    /** Filled rounded rectangle. */
    public static void roundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        // Straight bands.
        ctx.fill(x + r, y, x + w - r, y + h, color);           // centre column (full height)
        ctx.fill(x, y + r, x + r, y + h - r, color);           // left edge
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);   // right edge

        // Corner arcs, one scan-line at a time.
        for (int yy = 0; yy < r; yy++) {
            int dist = r - yy;
            int dx = (int) Math.ceil(Math.sqrt((double) r * r - (double) dist * dist));
            int topY = y + yy;
            int botY = y + h - 1 - yy;
            ctx.fill(x + r - dx, topY, x + r, topY + 1, color);                 // top-left
            ctx.fill(x + w - r, topY, x + w - r + dx, topY + 1, color);         // top-right
            ctx.fill(x + r - dx, botY, x + r, botY + 1, color);                 // bottom-left
            ctx.fill(x + w - r, botY, x + w - r + dx, botY + 1, color);         // bottom-right
        }
    }

    /** Rounded rectangle with a solid border drawn over a solid fill. */
    public static void roundedRectBordered(DrawContext ctx, int x, int y, int w, int h, int radius,
                                           int fill, int border, int thickness) {
        roundedRect(ctx, x, y, w, h, radius, border);
        int t = Math.max(1, thickness);
        roundedRect(ctx, x + t, y + t, w - 2 * t, h - 2 * t, Math.max(0, radius - t), fill);
    }

    /** Soft outer glow around a rounded rectangle (concentric translucent rings). */
    public static void glow(DrawContext ctx, int x, int y, int w, int h, int radius, int color, int spread, float alpha) {
        int layers = Math.max(1, spread);
        for (int i = layers; i >= 1; i--) {
            float a = alpha * (1f - (float) i / (layers + 1)) * 0.6f;
            int c = Colors.withAlpha(color, a);
            roundedRect(ctx, x - i, y - i, w + 2 * i, h + 2 * i, radius + i, c);
        }
    }

    // ------------------------------------------------------------------
    // Gradients
    // ------------------------------------------------------------------

    /** Vertical gradient, approximated with horizontal bands. */
    public static void verticalGradient(DrawContext ctx, int x, int y, int w, int h, int top, int bottom) {
        int bands = Math.min(h, 96);
        if (bands <= 0) {
            return;
        }
        for (int i = 0; i < bands; i++) {
            int y0 = y + (int) ((long) i * h / bands);
            int y1 = y + (int) ((long) (i + 1) * h / bands);
            if (y1 <= y0) {
                y1 = y0 + 1;
            }
            float t = bands == 1 ? 0f : (float) i / (bands - 1);
            ctx.fill(x, y0, x + w, y1, Colors.lerp(top, bottom, t));
        }
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    /** Draw text scaled around a centre point using the 2D matrix stack. */
    public static void bigCenteredText(DrawContext ctx, TextRenderer tr, Text text,
                                       float cx, float cy, float scale, int color) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        ctx.drawCenteredTextWithShadow(tr, text, 0, -tr.fontHeight / 2, color);
        m.popMatrix();
    }

    /** Draw text scaled around a centre point, no shadow (used for glow layers). */
    public static void bigCenteredTextNoShadow(DrawContext ctx, TextRenderer tr, Text text,
                                               float cx, float cy, float scale, int color) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        ctx.drawText(tr, text, -tr.getWidth(text) / 2, -tr.fontHeight / 2, color, false);
        m.popMatrix();
    }

    public static void centered(DrawContext ctx, TextRenderer tr, Text text, int cx, int y, int color) {
        ctx.drawCenteredTextWithShadow(tr, text, cx, y, color);
    }

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------

    /** Soft radial-ish glow blob, used for the animated background. */
    public static void blob(DrawContext ctx, float cx, float cy, float size, int color, float alpha) {
        int s = (int) size;
        int x = (int) cx, y = (int) cy;
        roundedRect(ctx, x - s, y - s, s * 2, s * 2, s, Colors.withAlpha(color, alpha * 0.35f));
        int s2 = (int) (size * 0.62f);
        roundedRect(ctx, x - s2, y - s2, s2 * 2, s2 * 2, s2, Colors.withAlpha(color, alpha * 0.55f));
    }

    /** Darkened edges (nested translucent frames). */
    public static void vignette(DrawContext ctx, int width, int height, float maxAlpha) {
        int frames = 18;
        for (int i = 0; i < frames; i++) {
            float a = maxAlpha * (1f - (float) i / frames);
            int c = Colors.withAlpha(0xFF000000, a * 0.12f);
            ctx.fill(i, i, width - i, i + 1, c);                 // top
            ctx.fill(i, height - i - 1, width - i, height - i, c); // bottom
            ctx.fill(i, i, i + 1, height - i, c);                 // left
            ctx.fill(width - i - 1, i, width - i, height - i, c);  // right
        }
    }

    /** Thin horizontal scanlines for a subtle CRT/premium feel. */
    public static void scanlines(DrawContext ctx, int width, int height, float alpha) {
        int c = Colors.withAlpha(0xFF000000, alpha);
        for (int y = 0; y < height; y += 3) {
            ctx.fill(0, y, width, y + 1, c);
        }
    }

    /** A thick line via DDA stepping (used for the check-mark). */
    public static void thickLine(DrawContext ctx, int x1, int y1, int x2, int y2, int thick, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            ctx.fill(x1, y1, x1 + thick, y1 + thick, color);
            return;
        }
        float sx = (x2 - x1) / (float) steps;
        float sy = (y2 - y1) / (float) steps;
        int r = Math.max(1, thick / 2);
        for (int i = 0; i <= steps; i++) {
            int px = Math.round(x1 + sx * i);
            int py = Math.round(y1 + sy * i);
            ctx.fill(px - r, py - r, px - r + thick, py - r + thick, color);
        }
    }
}
