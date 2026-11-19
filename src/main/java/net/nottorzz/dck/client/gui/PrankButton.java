package net.nottorzz.dck.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.nottorzz.dck.client.gui.render.Colors;
import net.nottorzz.dck.client.gui.render.Draw;
import net.nottorzz.dck.client.gui.render.Easing;
import org.joml.Matrix3x2fStack;

/**
 * A self-contained animated button.
 *
 * <p>Not a vanilla widget on purpose — rolling our own keeps full control over
 * the hover pop, glow and gloss while staying completely cosmetic. The screen
 * owns hit-testing and calls {@link #onPress()} when clicked.</p>
 */
public class PrankButton {

    public enum Style { PRIMARY, GHOST }

    public int x, y, w, h;
    public final Text label;
    public final Style style;
    private final Runnable action;

    private float hover;       // 0..1 animated hover amount
    private boolean wasHover;  // for hover-enter detection

    private static final int RED_BRIGHT = 0xFFFF4356;

    public PrankButton(Text label, Style style, Runnable action) {
        this.label = label;
        this.style = style;
        this.action = action;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void onPress() {
        if (action != null) {
            action.run();
        }
    }

    /** @return true if the pointer has just entered this frame (play a hover tick). */
    public boolean update(double mx, double my, float dtSeconds) {
        boolean hovered = contains(mx, my);
        float target = hovered ? 1f : 0f;
        float speed = 10f * dtSeconds;
        hover += (target - hover) * Math.min(1f, speed);
        boolean entered = hovered && !wasHover;
        wasHover = hovered;
        return entered;
    }

    public void render(DrawContext ctx, TextRenderer tr, long timeMs) {
        float h01 = Easing.clamp01(hover);
        float scale = 1f + 0.045f * Easing.outCubic(h01);
        float cx = x + w / 2f;
        float cy = y + h / 2f;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(scale, scale);
        m.translate(-cx, -cy);

        int radius = 12;

        if (style == Style.PRIMARY) {
            // Outer glow grows on hover.
            Draw.glow(ctx, x, y, w, h, radius, Colors.RED, 6, 0.25f + 0.6f * h01);
            int fill = Colors.lerp(Colors.RED, RED_BRIGHT, h01);
            Draw.roundedRectBordered(ctx, x, y, w, h, radius, fill, 0xFFFF6B79, 2);
            // Glossy top highlight.
            Draw.roundedRect(ctx, x + 4, y + 3, w - 8, h / 2 - 3, radius - 4,
                    Colors.withAlpha(0xFFFFFFFF, 0.10f + 0.06f * h01));
            ctx.drawCenteredTextWithShadow(tr, label, (int) cx, (int) (cy - tr.fontHeight / 2f), Colors.WHITE);
        } else {
            Draw.glow(ctx, x, y, w, h, radius, Colors.WHITE, 5, 0.10f + 0.35f * h01);
            int fill = Colors.lerp(Colors.PANEL_LIGHT, 0xFF2E2E36, h01);
            int border = Colors.lerp(0xFF55555F, Colors.WHITE, h01);
            Draw.roundedRectBordered(ctx, x, y, w, h, radius, fill, border, 2);
            Draw.roundedRect(ctx, x + 4, y + 3, w - 8, h / 2 - 3, radius - 4,
                    Colors.withAlpha(0xFFFFFFFF, 0.05f + 0.05f * h01));
            int textColor = Colors.lerp(0xFFDADAE2, Colors.WHITE, h01);
            ctx.drawCenteredTextWithShadow(tr, label, (int) cx, (int) (cy - tr.fontHeight / 2f), textColor);
        }

        m.popMatrix();
    }
}
