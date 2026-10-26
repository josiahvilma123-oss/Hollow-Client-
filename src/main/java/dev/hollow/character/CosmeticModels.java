package dev.hollow.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hollow.Hollow;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

/**
 * Builds outfit geometry from {@code assets/hollow/characters/<id>.json} (written by
 * {@code tools/characters.py}). Every piece hangs off the player mesh's head, body, arms or legs,
 * so it follows the player's own animation. Units are skin pixels, like vanilla models.
 */
public final class CosmeticModels {
	private static final String[] BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

	private CosmeticModels() {
	}

	/** Registers every outfit's model layer and attaches {@link CosmeticLayer} to player renderers. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		for (Cosmetic cosmetic : Cosmetic.values()) {
			if (cosmetic.layer() != null) {
				EntityModelLayerRegistry.registerModelLayer(cosmetic.layer(), () -> load(cosmetic));
			}
		}
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new CosmeticLayer((RenderLayerParent) avatar, context.getModelSet()));
			}
		});
	}

	/** Reads an outfit definition. A missing or broken file gives an empty model instead of a crash. */
	static LayerDefinition load(Cosmetic cosmetic) {
		String path = "/assets/" + Hollow.MOD_ID + "/characters/" + cosmetic.asset() + ".json";
		try (InputStream in = CosmeticModels.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IOException("missing " + path);
			}
			return build(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
		} catch (IOException | RuntimeException e) {
			Hollow.LOGGER.error("Could not load the {} outfit; it will be invisible", cosmetic.displayName(), e);
			MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
			mesh.getRoot().clearRecursively();
			return LayerDefinition.create(mesh, 64, 64);
		}
	}

	static LayerDefinition build(JsonObject model) {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = mesh.getRoot().clearRecursively();
		Map<String, PartDefinition> parts = new HashMap<>();
		for (String bone : BONES) {
			parts.put(bone, root.getChild(bone));
		}
		for (JsonElement element : model.getAsJsonArray("parts")) {
			JsonObject part = element.getAsJsonObject();
			PartDefinition parent = parts.get(part.get("parent").getAsString());
			if (parent == null) {
				throw new IllegalArgumentException("Unknown parent " + part.get("parent") + " for " + part.get("name"));
			}
			CubeListBuilder cubes = CubeListBuilder.create();
			for (JsonElement cubeElement : part.getAsJsonArray("cubes")) {
				JsonObject cube = cubeElement.getAsJsonObject();
				float[] uv = floats(cube.getAsJsonArray("uv"));
				float[] from = floats(cube.getAsJsonArray("from"));
				float[] size = floats(cube.getAsJsonArray("size"));
				cubes.texOffs((int) uv[0], (int) uv[1])
					.addBox(from[0], from[1], from[2], size[0], size[1], size[2], new CubeDeformation(cube.get("inflate").getAsFloat()));
			}
			float[] pivot = floats(part.getAsJsonArray("pivot"));
			float[] rotation = floats(part.getAsJsonArray("rotation"));
			String name = part.get("name").getAsString();
			parts.put(name, parent.addOrReplaceChild(name, cubes,
				PartPose.offsetAndRotation(pivot[0], pivot[1], pivot[2], rotation[0], rotation[1], rotation[2])));
		}
		JsonArray texture = model.getAsJsonArray("texture");
		return LayerDefinition.create(mesh, texture.get(0).getAsInt(), texture.get(1).getAsInt());
	}

	private static float[] floats(JsonArray array) {
		float[] values = new float[array.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = array.get(i).getAsFloat();
		}
		return values;
	}
}
