package dev.hollow.menu;

import dev.hollow.core.util.Colors;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A rounded Holo-styled button. It extends vanilla's Button, so keyboard focus, Tab navigation,
 * narration and tooltips all behave exactly as in the vanilla menu.
 */
public final class HoloButton extends Button {
	private final IntSupplier accent;
	private float hover;
	private long lastFrame = -1;
	private float alpha = 1f;

	public HoloButton(int x, int y, int width, int height, Component label, OnPress onPress, IntSupplier accent) {
		super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
		this.accent = accent;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	@Override
	protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		float delta = lastFrame < 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		boolean highlighted = isHoveredOrFocused() && active;
		hover += ((highlighted ? 1f : 0f) - hover) * Math.min(1f, delta * 12f);
		int accentColor = accent.getAsInt();
		float radius = getHeight() / 2f;
		int fill = Colors.lerp(0xE0120E19, Colors.lerp(accentColor, 0xFF000000, 0.35f), hover * 0.85f);
		if (hover > 0.02f) {
			Draw2D.glow(g, getX(), getY(), getWidth(), getHeight(), radius, Colors.fade(Colors.withAlpha(accentColor, 170), hover * alpha), 6, 1f);
		}
		Draw2D.rounded(g, getX(), getY(), getWidth(), getHeight(), radius, Colors.fade(fill, alpha));
		Draw2D.roundedBorder(g, getX(), getY(), getWidth(), getHeight(), radius, 1,
			Colors.fade(Colors.lerp(Theme.BORDER, accentColor, 0.25f + hover * 0.75f), alpha));
		int textColor = active ? Colors.lerp(Theme.TEXT_DIM, 0xFFFFFFFF, hover) : Theme.TEXT_FAINT;
		var font = Minecraft.getInstance().font;
		String text = getMessage().getString();
		g.drawString(font, text, getX() + (getWidth() - font.width(text)) / 2, getY() + (getHeight() - 8) / 2, Colors.fade(textColor, alpha), false);
	}
}
