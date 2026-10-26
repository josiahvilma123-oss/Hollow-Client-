package dev.hollow.menu;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hollow.Hollow;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Loads a user's custom menu background. Decoding happens off the render thread; only the GPU
 * upload runs on it. Missing, oversized or corrupt images fall back to the built-in scene.
 */
public final class MenuBackgrounds {
	public static final int MAX_SIZE = 4096;
	private static final Identifier CUSTOM = Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "menu/custom_background");

	private static String requested = "";
	private static CompletableFuture<NativeImage> pending;
	private static DynamicTexture texture;
	private static int width;
	private static int height;
	private static String error = "";

	private MenuBackgrounds() {
	}

	public static Path directory() {
		Path dir = FabricLoader.getInstance().getConfigDir().resolve("hollow").resolve("backgrounds");
		try {
			Files.createDirectories(dir);
		} catch (Exception e) {
			Hollow.LOGGER.warn("Could not create backgrounds folder {}", dir);
		}
		return dir;
	}

	public static String error() {
		return error;
	}

	/** The custom background texture once it is ready, otherwise null (callers use their fallback). */
	public static Identifier custom(String fileName) {
		if (!fileName.equals(requested)) {
			requested = fileName;
			release();
			error = "";
			Path file = directory().resolve(fileName).normalize();
			if (!file.startsWith(directory())) {
				error = "Invalid file name";
				return null;
			}
			pending = CompletableFuture.supplyAsync(() -> read(file));
		}
		if (pending != null && pending.isDone()) {
			NativeImage image = pending.getNow(null);
			pending = null;
			if (image != null) {
				width = image.getWidth();
				height = image.getHeight();
				texture = new DynamicTexture(() -> "Holo menu background", image);
				Minecraft.getInstance().getTextureManager().register(CUSTOM, texture);
			}
		}
		return texture != null ? CUSTOM : null;
	}

	public static int width() {
		return width;
	}

	public static int height() {
		return height;
	}

	private static NativeImage read(Path file) {
		try {
			if (!Files.isRegularFile(file)) {
				error = "File not found: " + file.getFileName();
				return null;
			}
			try (InputStream in = Files.newInputStream(file)) {
				NativeImage image = NativeImage.read(in);
				if (image.getWidth() > MAX_SIZE || image.getHeight() > MAX_SIZE || image.getWidth() < 16 || image.getHeight() < 16) {
					error = "Image must be between 16 and " + MAX_SIZE + " pixels per side";
					image.close();
					return null;
				}
				return image;
			}
		} catch (Exception e) {
			error = "Not a readable PNG: " + e.getMessage();
			Hollow.LOGGER.warn("Custom menu background {} could not be read: {}", file, e.getMessage());
			return null;
		}
	}

	/** Frees the uploaded texture. */
	public static void release() {
		if (texture != null) {
			Minecraft.getInstance().getTextureManager().release(CUSTOM);
			texture = null;
		}
	}
}
