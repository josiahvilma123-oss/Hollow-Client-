package dev.hollow.menu;

import dev.hollow.Hollow;
import dev.hollow.modules.MainMenuModule;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/** Decides when Minecraft's title screen is replaced by the Holo one. */
public final class TitleScreenSwap {
	private static WeakReference<Screen> vanillaKept = new WeakReference<>(null);

	private TitleScreenSwap() {
	}

	/** Called for every {@code Minecraft.setScreen}; returns the screen to actually show. */
	public static Screen swap(Screen screen) {
		if (screen == null || screen.getClass() != TitleScreen.class || Hollow.modules() == null) {
			return screen;
		}
		Minecraft minecraft = Minecraft.getInstance();
		// The demo has its own title-screen flow, and a vanilla menu the user asked for stays vanilla.
		if (minecraft.isDemo() || screen == vanillaKept.get() || !Hollow.module(MainMenuModule.class).isEnabled()) {
			return screen;
		}
		return new HoloTitleScreen();
	}

	/** Whether Holo's menu backdrop should replace vanilla's panorama. */
	public static boolean sceneEnabled() {
		return Hollow.modules() != null && Hollow.module(MainMenuModule.class).isEnabled() && !Minecraft.getInstance().isDemo();
	}

	/** Shows vanilla's title screen once. Sub-menus opened from it return to it. */
	public static void showVanilla() {
		TitleScreen vanilla = new TitleScreen();
		vanillaKept = new WeakReference<>(vanilla);
		Minecraft.getInstance().setScreen(vanilla);
	}
}
