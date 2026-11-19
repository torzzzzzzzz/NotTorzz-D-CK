package net.nottorzz.dck.client.gui.render;

import net.minecraft.client.gui.DrawContext;

/**
 * A fully procedural "laughing red mascot" — drawn entirely from circles and
 * ellipses, no texture files.
 *
 * <p>Design note: this is an <b>original</b> cartoon character (a round red
 * giggling blob), intentionally NOT a copy of any trademarked puppet. It fills
 * the same comedic "someone is laughing at you" role the prank calls for while
 * keeping the mod free of third-party intellectual property.</p>
 */
public final class Mascot {
    private Mascot() {
    }

    private static final int FUR       = 0xFFE23A2E; // warm red fur
    private static final int FUR_DARK  = 0xFF9E1F18; // shadow red
    private static final int FUR_LIGHT = 0xFFF2645A; // highlight red
    private static final int MOUTH     = 0xFF2A0608; // dark mouth cavity
    private static final int TONGUE    = 0xFFF26A86; // pink tongue
    private static final int TEETH     = 0xFFF6F4EC; // off-white
    private static final int EYE_WHITE = 0xFFF8F8FA;
    private static final int EYE_BLACK = 0xFF101014;
    private static final int NOSE      = 0xFFF28A2E; // orange nose

    /**
     * @param cx           centre x
     * @param cy           centre y (baseline before bounce)
     * @param size         overall radius in px
     * @param timeMs       animation clock
     * @param intensity    0..1 how hard it is laughing (mouth + bounce amplitude)
     */
    public static void render(DrawContext ctx, float cx, float cy, float size, long timeMs, float intensity) {
        intensity = Easing.clamp01(intensity);
        double t = timeMs / 1000.0;

        // Bounce + a little squash/stretch while cackling.
        float laugh = (float) Math.sin(t * 16.0);              // fast giggle wobble
        float bounce = (float) Math.abs(Math.sin(t * 8.0)) * size * 0.18f * intensity;
        float cyB = cy - bounce;
        float wobble = (float) Math.sin(t * 6.0) * 0.04f * intensity;

        float R = size;
        float bodyW = R * (1f + wobble);
        float bodyH = R * (1f - wobble);

        // Soft shadow under the mascot.
        ellipse(ctx, cx, cy + size * 0.95f, size * 0.8f, size * 0.22f, 0x66000000);

        // Fuzzy outline: a ring of little tufts around the body.
        int tufts = 26;
        for (int i = 0; i < tufts; i++) {
            double ang = (Math.PI * 2 * i / tufts) + t * 0.6;
            float tx = cx + (float) Math.cos(ang) * bodyW * 1.02f;
            float ty = cyB + (float) Math.sin(ang) * bodyH * 1.02f;
            circle(ctx, tx, ty, size * 0.14f, FUR_DARK);
        }
        // Body.
        ellipse(ctx, cx, cyB, bodyW, bodyH, FUR);
        // Highlight.
        ellipse(ctx, cx - R * 0.33f, cyB - R * 0.36f, R * 0.30f, R * 0.26f, FUR_LIGHT);

        // Mouth: opens and closes with the laugh.
        float open = (0.55f + 0.45f * laugh) * intensity + 0.15f;
        float mouthW = R * 0.62f;
        float mouthH = R * 0.46f * open + R * 0.06f;
        float mouthY = cyB + R * 0.26f;
        ellipse(ctx, cx, mouthY, mouthW, mouthH, MOUTH);
        // Top teeth strip.
        ellipse(ctx, cx, mouthY - mouthH * 0.72f, mouthW * 0.92f, mouthH * 0.34f, TEETH);
        // Tongue at the bottom of the mouth.
        ellipse(ctx, cx, mouthY + mouthH * 0.45f, mouthW * 0.60f, mouthH * 0.45f, TONGUE);

        // Nose (sits just above the mouth).
        circle(ctx, cx, cyB + R * 0.02f, R * 0.17f, NOSE);
        circle(ctx, cx - R * 0.05f, cyB - R * 0.03f, R * 0.06f, 0xFFF7B36A); // nose shine

        // Eyes: tall white ovals, pupils look down at "you". They squeeze to happy
        // crescents at peak laughter.
        float eyeDX = R * 0.34f;
        float eyeY = cyB - R * 0.34f;
        float squint = 0.5f + 0.5f * Math.max(0f, laugh); // 0..1
        float eyeH = R * 0.34f * (1f - 0.55f * squint * intensity);
        float eyeW = R * 0.26f;
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * eyeDX;
            ellipse(ctx, ex, eyeY, eyeW, eyeH, EYE_WHITE);
            circle(ctx, ex + s * R * 0.02f, eyeY + eyeH * 0.35f, R * 0.09f, EYE_BLACK);
            // Eyebrow (angled, amused).
            float bw = R * 0.22f;
            ctx.fill((int) (ex - bw), (int) (eyeY - eyeH - R * 0.10f),
                     (int) (ex + bw), (int) (eyeY - eyeH - R * 0.04f), FUR_DARK);
        }

        // Laugh motion lines flanking the head.
        float lineAlpha = Easing.clamp01(intensity);
        int lc = Colors.withAlpha(0xFFFFFFFF, 0.35f * lineAlpha * (0.6f + 0.4f * laugh));
        for (int s = -1; s <= 1; s += 2) {
            float base = cx + s * (R * 1.35f);
            for (int k = 0; k < 3; k++) {
                float yy = cyB - R * 0.3f + k * R * 0.3f;
                ctx.fill((int) (base), (int) yy, (int) (base + s * R * 0.35f), (int) (yy + 2), lc);
            }
        }
    }

    // --- primitives -------------------------------------------------------

    private static void circle(DrawContext ctx, float cx, float cy, float r, int color) {
        ellipse(ctx, cx, cy, r, r, color);
    }

    private static void ellipse(DrawContext ctx, float cx, float cy, float rx, float ry, int color) {
        if (rx <= 0 || ry <= 0) {
            return;
        }
        int iry = (int) Math.ceil(ry);
        for (int yy = -iry; yy <= iry; yy++) {
            double norm = yy / (double) ry;
            if (norm < -1 || norm > 1) {
                continue;
            }
            double half = rx * Math.sqrt(Math.max(0.0, 1.0 - norm * norm));
            int y = Math.round(cy + yy);
            ctx.fill(Math.round(cx - (float) half), y, Math.round(cx + (float) half), y + 1, color);
        }
    }
}
