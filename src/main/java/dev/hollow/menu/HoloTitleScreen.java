package dev.hollow.menu;

import com.mojang.realmsclient.RealmsMainScreen;
import dev.hollow.Hollow;
import dev.hollow.core.util.Colors;
import dev.hollow.gui.HollowScreen;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.render.Draw2D;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import com.mojang.authlib.minecraft.BanDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The Holo title screen. It offers everything vanilla's title screen does (plus Holo's studios)
 * over the animated {@link MenuScene}. Turning off the Main Menu module, or the Vanilla Menu
 * button, brings back Minecraft's own title screen.
 */
public final class HoloTitleScreen extends Screen {
	private static final Component COPYRIGHT = Component.translatable("title.credits");
	private static boolean firstOpen = true;

	private final long openedAt = Util.getMillis();
	private final float fadeSeconds;
	private String version = "";
	private int columnTop;
	private int logoY;
	private boolean failed;

	public HoloTitleScreen() {
		super(Component.literal("Holo"));
		// The first appearance after start-up fades in gently; returning from sub-menus is quick.
		this.fadeSeconds = firstOpen ? 0.9f : 0.2f;
		firstOpen = false;
	}

	private MainMenuModule module() {
		return Hollow.module(MainMenuModule.class);
	}

	private int accent() {
		return module().accent.argb();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	protected void init() {
		int rows = 6;
		int step = 22;
		columnTop = Math.max(64, Math.min(height / 4 + 44, height - rows * step - 26));
		logoY = Math.max(10, columnTop / 2 - 16);
		int left = width / 2 - 100;
		int y = columnTop;

		addRenderableWidget(new HoloButton(left, y, 200, 20, Component.translatable("menu.singleplayer"),
			b -> minecraft.setScreen(new SelectWorldScreen(this)), this::accent));
		Component disabled = multiplayerDisabledReason();
		Tooltip tooltip = disabled == null ? null : Tooltip.create(disabled);
		HoloButton multiplayer = addRenderableWidget(new HoloButton(left, y += step, 200, 20, Component.translatable("menu.multiplayer"),
			b -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)),
			this::accent));
		multiplayer.setTooltip(tooltip);
		multiplayer.active = disabled == null;
		HoloButton realms = addRenderableWidget(new HoloButton(left, y += step, 200, 20, Component.translatable("menu.online"),
			b -> minecraft.setScreen(new RealmsMainScreen(this)), this::accent));
		realms.setTooltip(tooltip);
		realms.active = disabled == null;

		y += step;
		addRenderableWidget(new HoloButton(left, y, 98, 20, Component.literal("Character Studio"),
			b -> minecraft.setScreen(new HollowScreen(this).openOn(CharacterStudioModule.class)), this::accent));
		addRenderableWidget(new HoloButton(left + 102, y, 98, 20, Component.literal("Sky Studio"),
			b -> minecraft.setScreen(new HollowScreen(this).openOn(SkyStudioModule.class)), this::accent));
		y += step;
		addRenderableWidget(new HoloButton(left, y, 98, 20, Component.literal("Client Settings"),
			b -> minecraft.setScreen(new HollowScreen(this)), this::accent));
		addRenderableWidget(new HoloButton(left + 102, y, 98, 20, Component.translatable("menu.options"),
			b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)), this::accent));
		y += step + 6;
		SpriteIconButton language = addRenderableWidget(CommonButtons.language(20,
			b -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())), true));
		language.setPosition(left - 24, y);
		HoloButton vanilla = addRenderableWidget(new HoloButton(left, y, 98, 20, Component.literal("Vanilla Menu"),
			b -> TitleScreenSwap.showVanilla(), this::accent));
		vanilla.setTooltip(Tooltip.create(Component.literal(
			"Show Minecraft's own title screen. Turn off Client Settings → Main Menu to keep it permanently.")));
		addRenderableWidget(new HoloButton(left + 102, y, 98, 20, Component.translatable("menu.quit"), b -> minecraft.stop(), this::accent));
		SpriteIconButton accessibility = addRenderableWidget(CommonButtons.accessibility(20,
			b -> minecraft.setScreen(new AccessibilityOptionsScreen(this, minecraft.options)), true));
		accessibility.setPosition(left + 204, y);

		int copyrightWidth = font.width(COPYRIGHT);
		addRenderableWidget(new PlainTextButton(width - copyrightWidth - 2, height - 10, copyrightWidth, 10, COPYRIGHT,
			b -> minecraft.setScreen(new CreditsAndAttributionScreen(this)), font));
		version = "Holo " + modVersion() + "  ·  Minecraft " + SharedConstants.getCurrentVersion().name();
		updateFade();
	}

	private Component multiplayerDisabledReason() {
		if (minecraft.allowsMultiplayer()) {
			return null;
		}
		if (minecraft.isNameBanned()) {
			return Component.translatable("title.multiplayer.disabled.banned.name");
		}
		BanDetails ban = minecraft.multiplayerBan();
		if (ban != null) {
			return ban.expires() != null
				? Component.translatable("title.multiplayer.disabled.banned.temporary")
				: Component.translatable("title.multiplayer.disabled.banned.permanent");
		}
		return Component.translatable("title.multiplayer.disabled");
	}

	/** Content opacity: fades in on open; instant when animation intensity is zero. */
	private float contentAlpha() {
		float motion = module().animation.getFloat();
		if (motion <= 0.01f) {
			return 1f;
		}
		float t = (Util.getMillis() - openedAt) / 1000f / fadeSeconds;
		t = Math.max(0f, Math.min(1f, t));
		return t * t * (3 - 2 * t);
	}

	private void updateFade() {
		float alpha = contentAlpha();
		for (GuiEventListener child : children()) {
			if (child instanceof AbstractWidget widget) {
				widget.setAlpha(alpha);
			}
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (failed) {
			return;
		}
		try {
			MenuScene.render(g, width, height, mouseX, mouseY, width / 2 + 128);
			renderLogo(g, contentAlpha());
		} catch (RuntimeException e) {
			fail(e);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		updateFade();
		super.render(g, mouseX, mouseY, partialTick);
		if (failed) {
			return;
		}
		float alpha = contentAlpha();
		g.drawString(font, version, 3, height - 10, Colors.fade(0xFFB7A9CC, alpha), false);
	}

	private void renderLogo(GuiGraphics g, float alpha) {
		MainMenuModule m = module();
		int accent = m.accent.argb();
		float lighting = m.lighting.getFloat();
		boolean decorations = m.decorations.get();
		float time = MenuScene.time();
		int scale = height < 250 ? 3 : 4;
		String word = "HOLO";
		int textWidth = Draw2D.spacedWidth(font, word, 2) * scale;
		float cx = width / 2f;
		float cy = logoY + 4 * scale;
		float pulse = 0.85f + 0.15f * (float) Math.sin(time * 1.4f);

		MenuScene.glow(g, cx, cy, textWidth * 2.4f, 24f * scale, Colors.fade(Colors.withAlpha(accent, (int) (150 * pulse * Math.min(1.4f, lighting))), alpha));
		if (decorations) {
			ring(g, cx, cy, textWidth * 0.78f, time, accent, alpha, false);
		}
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx - textWidth / 2f, logoY);
		pose.scale(scale, scale);
		int glowColor = Colors.fade(Colors.withAlpha(accent, (int) (90 * Math.min(1.4f, lighting))), alpha);
		for (int i = 0; i < 4; i++) {
			int dx = i == 0 ? -1 : i == 1 ? 1 : 0;
			int dy = i == 2 ? -1 : i == 3 ? 1 : 0;
			pose.pushMatrix();
			pose.translate(dx * 0.5f, dy * 0.5f);
			Draw2D.spaced(g, font, word, 0, 0, glowColor, 2);
			pose.popMatrix();
		}
		Draw2D.spaced(g, font, word, 0, 0, Colors.fade(0xFFF4EEFF, alpha), 2);
		pose.popMatrix();
		if (decorations) {
			ring(g, cx, cy, textWidth * 0.78f, time, accent, alpha, true);
		}
		String subtitle = "C L I E N T";
		Draw2D.centered(g, font, subtitle, (int) cx, (int) (logoY + 8 * scale + 5), Colors.fade(Colors.lerp(accent, 0xFFFFFFFF, 0.45f), alpha));
		Draw2D.scythe(g, cx - 8, logoY - 20, 16, Colors.fade(Colors.lerp(accent, 0xFFFFFFFF, 0.3f), alpha * 0.9f));
	}

	/**
	 * A tilted celestial ring around the logo. The far half is drawn behind the letters and the
	 * near half in front, so the ring appears to circle the word.
	 */
	private static void ring(GuiGraphics g, float cx, float cy, float radius, float time, int accent, float alpha, boolean front) {
		int segments = 64;
		float tilt = 0.26f;
		float spin = time * 0.25f;
		for (int i = 0; i < segments; i++) {
			double a0 = spin + i * Math.PI * 2 / segments;
			double a1 = spin + (i + 1) * Math.PI * 2 / segments;
			boolean isFront = Math.sin(a0) > 0;
			if (isFront != front) {
				continue;
			}
			float depth = (float) (0.5 + 0.5 * Math.sin(a0));
			int color = Colors.fade(Colors.withAlpha(Colors.lerp(accent, 0xFFFFFFFF, 0.25f), (int) (60 + depth * 120)), alpha);
			Draw2D.line(g, cx + (float) Math.cos(a0) * radius, cy + (float) Math.sin(a0) * radius * tilt,
				cx + (float) Math.cos(a1) * radius, cy + (float) Math.sin(a1) * radius * tilt, 1f, color);
		}
		// Gold ornaments travelling along the ring.
		for (int i = 0; i < 6; i++) {
			double a = spin * 1.6 + i * Math.PI / 3;
			if ((Math.sin(a) > 0) != front) {
				continue;
			}
			float depth = (float) (0.5 + 0.5 * Math.sin(a));
			float x = cx + (float) Math.cos(a) * radius;
			float y = cy + (float) Math.sin(a) * radius * tilt;
			float s = 1.2f + depth;
			int color = Colors.fade(Colors.withAlpha(0xFFE6B54A, (int) (120 + depth * 135)), alpha);
			Draw2D.line(g, x - s, y, x + s, y, s * 0.9f, color);
			Draw2D.line(g, x, y - s, x, y + s, s * 0.9f, color);
		}
	}

	private static String modVersion() {
		return FabricLoader.getInstance().getModContainer(Hollow.MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
	}

	private void fail(RuntimeException e) {
		failed = true;
		Hollow.LOGGER.error("Holo main menu failed; restoring the vanilla title screen", e);
		module().setEnabled(false);
		minecraft.execute(() -> minecraft.setScreen(new TitleScreen()));
	}

	/** Whether this screen should be swapped back for vanilla's (module turned off while showing). */
	@Override
	public void tick() {
		if (!module().isEnabled()) {
			minecraft.setScreen(new TitleScreen());
		}
	}
}
