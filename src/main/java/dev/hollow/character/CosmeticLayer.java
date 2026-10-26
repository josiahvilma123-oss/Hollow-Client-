package dev.hollow.character;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hollow.Hollow;
import dev.hollow.modules.CharacterStudioModule;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Draws the selected Character Studio cosmetic on the local player only. It is purely
 * client-side: other players never see it and nothing is sent to the server.
 */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Map<Cosmetic, Identifier> TEXTURES = new EnumMap<>(Cosmetic.class);
	private final Map<Cosmetic, PlayerModel> models = new EnumMap<>(Cosmetic.class);

	static {
		for (Cosmetic cosmetic : Cosmetic.values()) {
			TEXTURES.put(cosmetic, Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "textures/entity/cosmetics/" + cosmetic.asset() + ".png"));
		}
	}

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
		super(parent);
		for (Cosmetic cosmetic : Cosmetic.values()) {
			if (cosmetic != Cosmetic.NONE) {
				models.put(cosmetic, new PlayerModel(modelSet.bakeLayer(CosmeticModels.layerFor(cosmetic)), false));
			}
		}
	}

	public static Identifier texture(Cosmetic cosmetic) {
		return TEXTURES.get(cosmetic);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
		CharacterStudioModule studio = Hollow.module(CharacterStudioModule.class);
		Minecraft minecraft = Minecraft.getInstance();
		if (!studio.isEnabled() || state.isInvisible || minecraft.player == null || state.id != minecraft.player.getId()) {
			return;
		}
		Cosmetic cosmetic = studio.cosmetic.get();
		PlayerModel model = models.get(cosmetic);
		if (model == null) {
			return;
		}
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
		collector.submitModel(model, state, poseStack, RenderTypes.entityCutoutNoCull(texture(cosmetic)), light, overlay, state.outlineColor, null);
	}
}
