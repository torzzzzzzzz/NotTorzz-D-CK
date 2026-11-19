package net.nottorzz.dck.client.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.nottorzz.dck.client.Money;
import net.nottorzz.dck.client.config.PrankConfig;
import net.nottorzz.dck.client.gui.render.Colors;
import net.nottorzz.dck.client.gui.render.Draw;
import net.nottorzz.dck.client.gui.render.Easing;
import net.nottorzz.dck.client.gui.render.Mascot;
import net.nottorzz.dck.client.sound.PrankSounds;
import org.joml.Matrix3x2fStack;

/**
 * The premium, animated prank interface.
 *
 * <p>Flow: {@code MENU} (branding + fake balance + two buttons) →
 * {@code COUNTDOWN} (dramatic 3-2-1) → {@code REVEAL} (huge number, laughing
 * mascot, flashing lights, "NOTTORZZ GOT YOU!") → {@code SAFE}
 * ("Relax. Your balance is safe.").</p>
 *
 * <p>Absolutely nothing here sends a command or moves currency. The numbers are
 * display-only; the whole screen is a visual gag.</p>
 */
public class PrankScreen extends Screen {

    private enum Phase { MENU, COUNTDOWN, REVEAL, SAFE }

    private static final long COUNTDOWN_MS = 2400;
    private static final long REVEAL_MS = 4200;

    private final PrankConfig config;
    private final double balance;
    private final double amount;
    private final boolean balanceKnown;

    private Phase phase = Phase.MENU;
    private boolean choseDontPay = true;

    private long screenOpenMs;
    private long phaseStartMs;
    private double lastRenderMs = 0;
    private float dt;

    // per-phase scheduling flags
    private int lastCountdownStep = -1;
    private boolean sndGotcha, sndLaugh2, sndLaugh3;

    private PrankButton payBtn;
    private PrankButton dontPayBtn;
    private PrankButton againBtn;
    private PrankButton closeBtn;

    public PrankScreen(double balance, boolean balanceKnown, PrankConfig config) {
        super(Text.literal("NotTorzz D*CK"));
        this.config = config;
        this.balance = Math.max(0, balance);
        this.balanceKnown = balanceKnown;
        this.amount = this.balance * config.payPercent;
    }

    @Override
    protected void init() {
        long now = System.currentTimeMillis();
        if (screenOpenMs == 0) {
            screenOpenMs = now;
            phaseStartMs = now;
        }
        buildButtons();
    }

    private void buildButtons() {
        int pw = Math.min(this.width - 40, 440);
        int ph = Math.min(this.height - 40, 320);
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;

        int gap = 14;
        int bw = (pw - 48 - gap) / 2;
        int bh = 40;
        int by = py + ph - 64;
        int bx = px + 24;

        payBtn = new PrankButton(Text.literal("PAY NOTTORZZ"), PrankButton.Style.GHOST, this::chosePay);
        payBtn.setBounds(bx, by, bw, bh);
        dontPayBtn = new PrankButton(Text.literal("DON'T PAY NOTTORZZ"), PrankButton.Style.PRIMARY, this::choseDontPay);
        dontPayBtn.setBounds(bx + bw + gap, by, bw, bh);

        againBtn = new PrankButton(Text.literal("Play again"), PrankButton.Style.GHOST, this::playAgain);
        againBtn.setBounds(bx, by, bw, bh);
        closeBtn = new PrankButton(Text.literal("Close"), PrankButton.Style.PRIMARY, this::close);
        closeBtn.setBounds(bx + bw + gap, by, bw, bh);
    }

    // --- button actions ---------------------------------------------------

    private void chosePay() {
        choseDontPay = false;
        PrankSounds.click();
        setPhase(Phase.COUNTDOWN);
    }

    private void choseDontPay() {
        choseDontPay = true;
        PrankSounds.click();
        setPhase(Phase.COUNTDOWN);
    }

    private void playAgain() {
        PrankSounds.click();
        setPhase(Phase.MENU);
        screenOpenMs = System.currentTimeMillis(); // replay the entrance pop
    }

    private void setPhase(Phase p) {
        phase = p;
        phaseStartMs = System.currentTimeMillis();
        lastCountdownStep = -1;
        sndGotcha = sndLaugh2 = sndLaugh3 = false;
    }

    // --- dev-preview helpers (used only by the screenshot harness) ---------
    public void devForceCountdown(boolean dontPay) { this.choseDontPay = dontPay; setPhase(Phase.COUNTDOWN); }
    public void devForceReveal(boolean dontPay) { this.choseDontPay = dontPay; setPhase(Phase.REVEAL); }
    public void devForceSafe() { setPhase(Phase.SAFE); }

    // --- lifecycle --------------------------------------------------------

    @Override
    public boolean shouldPause() {
        return false; // keep the world running; this mod stays unobtrusive
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty: render() paints its own opaque premium background.
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0) {
            switch (phase) {
                case MENU -> {
                    if (payBtn.contains(mx, my)) { payBtn.onPress(); return true; }
                    if (dontPayBtn.contains(mx, my)) { dontPayBtn.onPress(); return true; }
                }
                case REVEAL -> { setPhase(Phase.SAFE); return true; }
                case SAFE -> {
                    if (againBtn.contains(mx, my)) { againBtn.onPress(); return true; }
                    if (closeBtn.contains(mx, my)) { closeBtn.onPress(); return true; }
                }
                default -> { }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEnterOrSpace() && phase == Phase.REVEAL) {
            setPhase(Phase.SAFE);
            return true;
        }
        return super.keyPressed(input);
    }

    // --- rendering --------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        dt = lastRenderMs == 0 ? 0f : Math.min(0.1f, (float) ((now - lastRenderMs) / 1000.0));
        lastRenderMs = now;

        advancePhase(now);
        drawBackground(ctx, now);

        switch (phase) {
            case MENU -> renderMenu(ctx, mouseX, mouseY, now);
            case COUNTDOWN -> renderCountdown(ctx, now);
            case REVEAL -> renderReveal(ctx, now);
            case SAFE -> renderSafe(ctx, mouseX, mouseY, now);
        }
    }

    private void advancePhase(long now) {
        long tPhase = now - phaseStartMs;
        if (phase == Phase.COUNTDOWN && tPhase >= COUNTDOWN_MS) {
            setPhase(Phase.REVEAL);
        } else if (phase == Phase.REVEAL && tPhase >= REVEAL_MS) {
            setPhase(Phase.SAFE);
        }
    }

    private void drawBackground(DrawContext ctx, long now) {
        Draw.verticalGradient(ctx, 0, 0, this.width, this.height, Colors.BG_TOP, Colors.BG_BOTTOM);
        // Drifting red glow blobs.
        float t = now / 1000f;
        Draw.blob(ctx, this.width * (0.30f + 0.10f * (float) Math.sin(t * 0.6)),
                this.height * (0.28f + 0.06f * (float) Math.cos(t * 0.5)), this.width * 0.22f, Colors.RED, 0.16f);
        Draw.blob(ctx, this.width * (0.74f + 0.08f * (float) Math.cos(t * 0.4)),
                this.height * (0.72f + 0.06f * (float) Math.sin(t * 0.7)), this.width * 0.26f, Colors.RED_DEEP, 0.18f);
        Draw.scanlines(ctx, this.width, this.height, 0.05f);
        Draw.vignette(ctx, this.width, this.height, 1.0f);
    }

    // ------------------------------------------------------------------ MENU

    private void renderMenu(DrawContext ctx, int mouseX, int mouseY, long now) {
        int pw = Math.min(this.width - 40, 440);
        int ph = Math.min(this.height - 40, 320);
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;
        int cx = this.width / 2;

        float entrance = Easing.outBack(Easing.clamp01((now - screenOpenMs) / 450f));
        float scale = Easing.lerp(0.92f, 1f, Easing.clamp01((now - screenOpenMs) / 450f));
        float slide = (1f - entrance) * 18f;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, py + ph / 2f + slide);
        m.scale(scale, scale);
        m.translate(-cx, -(py + ph / 2f));

        // Panel + accent bar.
        Draw.glow(ctx, px, py, pw, ph, 16, Colors.RED, 5, 0.18f);
        Draw.roundedRectBordered(ctx, px, py, pw, ph, 16, Colors.PANEL, Colors.PANEL_EDGE, 1);
        Draw.roundedRect(ctx, px + 16, py + 12, pw - 32, 4, 2, Colors.RED);

        // Status chip.
        String chip = "● ACTIVATED ON DONUTSMP";
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(chip), cx, py + 22, Colors.RED);

        // Branding.
        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("NotTorzz"), cx, py + 52, 2.0f, Colors.WHITE);
        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("D*CK"), cx, py + 78, 1.5f, Colors.RED);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("P R E M I U M   P R A N K   C L I E N T"), cx, py + 94, Colors.GREY_DIM);

        // Divider.
        ctx.fill(px + 40, py + 110, px + pw - 40, py + 111, Colors.PANEL_EDGE);

        // Balance block.
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("YOUR DETECTED BALANCE"),
                cx, py + 122, Colors.GREY);
        String balStr = Money.format(balance) + (balanceKnown ? "" : "  (demo)");
        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal(balStr), cx, py + 142, 1.4f, Colors.WHITE);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("AMOUNT IN QUESTION (" + Math.round(config.payPercent * 100) + "%)"),
                cx, py + 166, Colors.GREY);
        // soft glow behind the red number
        int amtY = py + 188;
        Draw.bigCenteredTextNoShadow(ctx, this.textRenderer, Text.literal(Money.format(amount)),
                cx, amtY, 1.9f, Colors.withAlpha(Colors.RED, 0.35f));
        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal(Money.format(amount)),
                cx, amtY, 1.8f, Colors.RED);

        // Buttons.
        if (payBtn.update(mouseX, mouseY, dt)) PrankSounds.hover();
        if (dontPayBtn.update(mouseX, mouseY, dt)) PrankSounds.hover();
        payBtn.render(ctx, this.textRenderer, now);
        dontPayBtn.render(ctx, this.textRenderer, now);

        // Readable disclaimer under the buttons.
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Buttons are part of a visual prank. No real payment is executed."),
                cx, py + ph - 18, Colors.GREY);

        m.popMatrix();
    }

    // ------------------------------------------------------------- COUNTDOWN

    private void renderCountdown(DrawContext ctx, long now) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        long tPhase = now - phaseStartMs;

        ctx.fill(0, 0, this.width, this.height, Colors.withAlpha(0xFF000000, 0.45f));

        int step = 3 - (int) (tPhase / 800); // 3,2,1
        step = Math.max(1, Math.min(3, step));
        if (step != lastCountdownStep) {
            PrankSounds.countdownTick(3 - step);
            lastCountdownStep = step;
        }

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("P R O C E S S I N G   P A Y M E N T"), cx, cy - 70, Colors.GREY);

        float within = (tPhase % 800) / 800f;
        float numScale = Easing.lerp(6.5f, 3.8f, Easing.outCubic(within));
        int alpha = (int) (255 * (1f - within * 0.3f));
        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal(String.valueOf(step)),
                cx, cy + 6, numScale, Colors.withAlpha(Colors.RED, alpha));

        // ring of ticking dots
        for (int i = 0; i < 12; i++) {
            double ang = Math.PI * 2 * i / 12 - Math.PI / 2;
            boolean on = (i / 12f) <= (tPhase / (float) COUNTDOWN_MS);
            int r = 60;
            int dx = (int) (cx + Math.cos(ang) * r);
            int dy = (int) (cy + 6 + Math.sin(ang) * r);
            Draw.roundedRect(ctx, dx - 2, dy - 2, 4, 4, 2, on ? Colors.RED : Colors.GREY_DIM);
        }
    }

    // ---------------------------------------------------------------- REVEAL

    private void renderReveal(DrawContext ctx, long now) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        long tPhase = now - phaseStartMs;
        float in = Easing.clamp01(tPhase / 450f);

        // Flashing red/white overlay (accessibility-gated).
        if (config.flashingEffects) {
            boolean white = (now / 150) % 2 == 0;
            float pulse = 0.10f + 0.06f * (float) Math.abs(Math.sin(now / 150.0));
            int flash = Colors.withAlpha(white ? 0xFFFFFFFF : Colors.RED, pulse);
            ctx.fill(0, 0, this.width, this.height, flash);
        } else {
            ctx.fill(0, 0, this.width, this.height, Colors.withAlpha(0xFF000000, 0.3f));
        }

        // Scheduled comedy sounds.
        if (!sndGotcha && tPhase > 60) { PrankSounds.gotcha(); sndGotcha = true; }
        if (!sndLaugh2 && tPhase > 1100) { PrankSounds.laugh(); sndLaugh2 = true; }
        if (!sndLaugh3 && tPhase > 2200) { PrankSounds.laugh(); sndLaugh3 = true; }

        // Laughing mascot.
        float mascotSize = Math.min(this.width, this.height) * 0.13f * Easing.outBack(in);
        Mascot.render(ctx, cx, cy - 58, mascotSize, now, in);

        // A little screen-shake for drama.
        float shake = (choseDontPay ? 3f : 1.2f) * (0.5f + 0.5f * (float) Math.sin(now / 40.0)) * in;
        float sx = (float) (Math.sin(now / 23.0) * shake);
        float sy = (float) (Math.cos(now / 19.0) * shake);

        if (choseDontPay) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("NOTTORZZ IS TAKING..."), cx, cy + 6, Colors.WHITE);
            Draw.bigCenteredTextNoShadow(ctx, this.textRenderer, Text.literal(Money.format(amount)),
                    cx + sx, cy + 44 + sy, 3.4f, Colors.withAlpha(Colors.WHITE, 0.25f));
            Draw.bigCenteredText(ctx, this.textRenderer, Text.literal(Money.format(amount)),
                    cx + sx, cy + 44 + sy, 3.2f, Colors.RED);
            Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("NOTTORZZ GOT YOU!"),
                    cx, cy + 92, 1.6f, Colors.WHITE);
        } else {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("NOTTORZZ RECEIVED..."), cx, cy + 6, Colors.WHITE);
            Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("$0"),
                    cx + sx, cy + 44 + sy, 3.2f, Colors.GREEN);
            Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("you tried to PAY him? weird flex"),
                    cx, cy + 86, 1.1f, Colors.WHITE);
            Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("NOTTORZZ GOT YOU ANYWAY!"),
                    cx, cy + 104, 1.2f, Colors.RED);
        }

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("(click to continue)"), cx, this.height - 20, Colors.GREY);
    }

    // ------------------------------------------------------------------ SAFE

    private void renderSafe(DrawContext ctx, int mouseX, int mouseY, long now) {
        int pw = Math.min(this.width - 40, 440);
        int ph = Math.min(this.height - 40, 320);
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;
        int cx = this.width / 2;
        long tPhase = now - phaseStartMs;
        float in = Easing.outCubic(Easing.clamp01(tPhase / 350f));

        Draw.glow(ctx, px, py, pw, ph, 16, Colors.GREEN, 4, 0.12f);
        Draw.roundedRectBordered(ctx, px, py, pw, ph, 16, Colors.PANEL, Colors.PANEL_EDGE, 1);

        // Green check in a ring.
        int gx = cx, gy = py + 70;
        int ringR = (int) (26 * in);
        Draw.roundedRect(ctx, gx - ringR, gy - ringR, ringR * 2, ringR * 2, ringR,
                Colors.withAlpha(Colors.GREEN, 0.18f));
        if (in > 0.2f) {
            Draw.thickLine(ctx, gx - 12, gy, gx - 3, gy + 10, 4, Colors.GREEN);
            Draw.thickLine(ctx, gx - 3, gy + 10, gx + 14, gy - 10, 4, Colors.GREEN);
        }

        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("Relax."), cx, py + 118, 1.8f, Colors.WHITE);
        Draw.bigCenteredText(ctx, this.textRenderer, Text.literal("Your balance is safe."),
                cx, py + 146, 1.5f, Colors.GREEN);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("NotTorzz never touched a thing — none of it was real."),
                cx, py + 176, Colors.GREY);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("No commands were sent. No currency moved. 100% cosmetic."),
                cx, py + 190, Colors.GREY_DIM);

        if (againBtn.update(mouseX, mouseY, dt)) PrankSounds.hover();
        if (closeBtn.update(mouseX, mouseY, dt)) PrankSounds.hover();
        againBtn.render(ctx, this.textRenderer, now);
        closeBtn.render(ctx, this.textRenderer, now);
    }
}
