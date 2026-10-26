package dev.hollow.character;

import dev.hollow.Hollow;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;

/**
 * Original cosmetic geometry, built on the player mesh so every piece follows the player's head,
 * body and arm animation. Units are skin pixels, like vanilla models.
 */
public final class CosmeticModels {
	public static final ModelLayerLocation PHARAOH = layer("pharaoh");
	public static final ModelLayerLocation WRAITH = layer("wraith");
	public static final ModelLayerLocation HOLLOW_KNIGHT = layer("hollow_knight");

	private CosmeticModels() {
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Hollow.MOD_ID, "cosmetic_" + name), "main");
	}

	public static ModelLayerLocation layerFor(Cosmetic cosmetic) {
		return switch (cosmetic) {
			case PHARAOH -> PHARAOH;
			case WRAITH -> WRAITH;
			case HOLLOW_KNIGHT -> HOLLOW_KNIGHT;
			case NONE -> throw new IllegalArgumentException("No model for NONE");
		};
	}

	private static PartDefinition emptyPlayer(MeshDefinition mesh) {
		return mesh.getRoot().clearRecursively();
	}

	/** Nemes headdress with lappets and uraeus, plus a broad collar. */
	public static LayerDefinition pharaoh() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = emptyPlayer(mesh);
		root.getChild("head").addOrReplaceChild("nemes", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5F, -8.6F, -4.5F, 9.0F, 4.0F, 9.0F, new CubeDeformation(0.2F))
				.texOffs(0, 16).addBox(-5.2F, -5.0F, -2.0F, 1.0F, 10.0F, 3.0F)
				.texOffs(0, 16).addBox(4.2F, -5.0F, -2.0F, 1.0F, 10.0F, 3.0F)
				.texOffs(16, 16).addBox(-4.5F, -5.0F, 3.8F, 9.0F, 8.0F, 1.0F)
				.texOffs(0, 48).addBox(-0.5F, -10.2F, -5.0F, 1.0F, 2.0F, 1.0F),
			PartPose.ZERO);
		root.getChild("body").addOrReplaceChild("collar", CubeListBuilder.create()
				.texOffs(0, 48).addBox(-4.5F, -0.2F, -2.5F, 9.0F, 3.0F, 5.0F, new CubeDeformation(0.1F)),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** Deep hood and a long cloak with a violet-trimmed mantle. */
	public static LayerDefinition wraith() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = emptyPlayer(mesh);
		root.getChild("head").addOrReplaceChild("hood", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -9.2F, -5.2F, 10.0F, 1.0F, 10.0F)
				.texOffs(0, 12).addBox(-5.2F, -8.4F, -5.2F, 1.0F, 9.0F, 10.0F)
				.texOffs(0, 12).addBox(4.2F, -8.4F, -5.2F, 1.0F, 9.0F, 10.0F)
				.texOffs(22, 12).addBox(-5.0F, -8.4F, 4.0F, 10.0F, 9.0F, 1.0F),
			PartPose.ZERO);
		root.getChild("body").addOrReplaceChild("cloak", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-5.0F, -0.4F, 2.6F, 10.0F, 22.0F, 1.0F)
				.texOffs(0, 48).addBox(-5.0F, -0.6F, -3.0F, 10.0F, 3.0F, 6.0F, new CubeDeformation(0.15F)),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** Visored helm with a crest, broad pauldrons and a short cape. */
	public static LayerDefinition hollowKnight() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition root = emptyPlayer(mesh);
		root.getChild("head").addOrReplaceChild("helm", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5F, -8.6F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.15F))
				.texOffs(0, 48).addBox(-3.0F, -5.6F, -4.9F, 6.0F, 1.0F, 1.0F)
				.texOffs(0, 48).addBox(-0.5F, -5.6F, -4.9F, 1.0F, 4.0F, 1.0F)
				.texOffs(40, 0).addBox(-0.5F, -12.5F, -3.0F, 1.0F, 4.0F, 7.0F),
			PartPose.ZERO);
		root.getChild("right_arm").addOrReplaceChild("right_pauldron", CubeListBuilder.create()
				.texOffs(0, 20).addBox(-4.0F, -2.8F, -3.0F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.1F)),
			PartPose.ZERO);
		root.getChild("left_arm").addOrReplaceChild("left_pauldron", CubeListBuilder.create()
				.texOffs(0, 20).addBox(-1.0F, -2.8F, -3.0F, 5.0F, 3.0F, 6.0F, new CubeDeformation(0.1F)),
			PartPose.ZERO);
		root.getChild("body").addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(22, 32).addBox(-4.5F, 0.2F, 2.4F, 9.0F, 18.0F, 1.0F),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}
}
