package dev.hollow.hud;

import dev.hollow.Hollow;
import dev.hollow.core.hud.HudSnap;
import dev.hollow.core.util.Colors;
import dev.hollow.menu.HoloButton;
import dev.hollow.modules.HudLayoutModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-and-drop editor for Holo's HUD. Elements snap to the screen edges, centre lines and each
 * other while dragged (gold guide lines show the snap). Right-click resets an element; arrow keys
 * nudge the selected one (Shift for 10 pixels). Hidden elements are shown dimmed with the reason.
 */
public final class HudEditorScreen extends Screen {
	private static final float MARGIN = 4f;
	private static final float SNAP_DISTANCE = 5f;
	private final Screen parent;
	private HudElement selected;
	private HudElement dragging;
	private float dragOffsetX;
	private float dragOffsetY;
	private List<HudSnap.Guide> guides = List.of();
	private HoloButton snapButton;

	private HudEditorScreen(Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	public static void open() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new HudEditorScreen(minecraft.screen));
	}

	private static HudLayoutModule layout() {
		return Hollow.module(HudLayoutModule.class);
	}

	@Override
	protected void init() {
		int y = height - 28;
		int cx = width / 2;
		addRenderableWidget(new HoloButton(cx - 154, y, 100, 18, Component.literal("Reset All"), b -> layout().positions.set(List.of()),
			Theme::accent));
		snapButton = addRenderableWidget(new HoloButton(cx - 50, y, 100, 18, snapLabel(), b -> {
			layout().snap.set(!layout().snap.get());
			snapButton.setMessage(snapLabel());
		}, Theme::accent));
		addRenderableWidget(new HoloButton(cx + 54, y, 100, 18, Component.literal("Done"), b -> onClose(), Theme::accent));
	}

	private static Component snapLabel() {
		return Component.literal("Snapping: " + (layout().snap.get() ? "On" : "Off"));
	}

	private float[] box(HudElement element) {
		float[] pos = HudElements.screenPosition(element, width, height);
		return new float[] {pos[0], pos[1], element.width() * element.scale(), element.height() * element.scale()};
	}

	private HudElement elementAt(double mouseX, double mouseY) {
		List<HudElement> all = HudElements.all();
		for (int i = all.size() - 1; i >= 0; i--) {
			float[] b = box(all.get(i));
			if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
				return all.get(i);
			}
		}
		return null;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x60000000);
		int faint = Colors.withAlpha(Theme.accent(), 40);
		g.fill(width / 2, 0, width / 2 + 1, height, faint);
		g.fill(0, height / 2, width, height / 2 + 1, faint);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		HudElement hovered = dragging != null ? dragging : elementAt(mouseX, mouseY);
		for (HudElement element : HudElements.all()) {
			float[] b = box(element);
			Matrix3x2fStack pose = g.pose();
			pose.pushMatrix();
			pose.translate(b[0], b[1]);
			pose.scale(element.scale(), element.scale());
			element.render(g, true);
			pose.popMatrix();
			boolean shown = element.drawnElsewhere() || element.visible();
			if (!shown) {
				g.fill((int) b[0], (int) b[1], (int) (b[0] + b[2]), (int) (b[1] + b[3]), 0x90000000);
			}
			int border = element == selected ? Theme.accentLight() : element == hovered ? Theme.accent() : Colors.withAlpha(Theme.accent(), 90);
			Draw2D.border(g, (int) b[0] - 1, (int) b[1] - 1, (int) b[2] + 2, (int) b[3] + 2, border);
			String label = element.name() + (shown ? "" : " · " + element.hiddenReason());
			int labelY = b[1] >= 11 ? (int) b[1] - 10 : (int) (b[1] + b[3] + 2);
			Draw2D.text(g, font, Draw2D.ellipsize(font, label, Math.max(60, (int) b[2] + 60)), (int) b[0], labelY,
				shown ? Theme.TEXT : Theme.TEXT_DIM);
		}
		int gold = 0xFFE6B54A;
		for (HudSnap.Guide guide : guides) {
			if (guide.vertical()) {
				g.fill(Math.round(guide.position()), 0, Math.round(guide.position()) + 1, height, gold);
			} else {
				g.fill(0, Math.round(guide.position()), width, Math.round(guide.position()) + 1, gold);
			}
		}
		Draw2D.centered(g, font, "Drag to move · Right-click to reset · Arrow keys nudge · Esc when done", width / 2, height - 42, Theme.TEXT_DIM);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		HudElement element = elementAt(event.x(), event.y());
		selected = element;
		if (element == null) {
			return false;
		}
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			layout().resetPosition(element.id());
			return true;
		}
		float[] b = box(element);
		dragging = element;
		dragOffsetX = (float) event.x() - b[0];
		dragOffsetY = (float) event.y() - b[1];
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) {
			return super.mouseDragged(event, dragX, dragY);
		}
		float[] b = box(dragging);
		HudSnap.Box moved = new HudSnap.Box((float) event.x() - dragOffsetX, (float) event.y() - dragOffsetY, b[2], b[3]);
		float x;
		float y;
		if (layout().snap.get()) {
			List<HudSnap.Box> others = new ArrayList<>();
			for (HudElement other : HudElements.all()) {
				if (other != dragging) {
					float[] o = box(other);
					others.add(new HudSnap.Box(o[0], o[1], o[2], o[3]));
				}
			}
			HudSnap.Result result = HudSnap.snap(moved, width, height, MARGIN, SNAP_DISTANCE, others);
			x = result.x();
			y = result.y();
			guides = result.guides();
		} else {
			x = Math.max(0, Math.min(width - b[2], moved.x()));
			y = Math.max(0, Math.min(height - b[3], moved.y()));
		}
		place(dragging, x, y, b[2], b[3]);
		return true;
	}

	private void place(HudElement element, float x, float y, float w, float h) {
		layout().setPosition(element.id(), HudSnap.toFraction(x, w, width), HudSnap.toFraction(y, h, height));
		if (element.drawnElsewhere()) {
			element.onMoved();
		}
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		guides = List.of();
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (selected != null) {
			int step = event.hasShiftDown() ? 10 : 1;
			int dx = switch (event.key()) {
				case GLFW.GLFW_KEY_LEFT -> -step;
				case GLFW.GLFW_KEY_RIGHT -> step;
				default -> 0;
			};
			int dy = switch (event.key()) {
				case GLFW.GLFW_KEY_UP -> -step;
				case GLFW.GLFW_KEY_DOWN -> step;
				default -> 0;
			};
			if (dx != 0 || dy != 0) {
				float[] b = box(selected);
				place(selected, Math.max(0, Math.min(width - b[2], b[0] + dx)), Math.max(0, Math.min(height - b[3], b[1] + dy)), b[2], b[3]);
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (Hollow.config() != null) {
			Hollow.config().save();
		}
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
