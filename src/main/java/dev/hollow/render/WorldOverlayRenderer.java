package dev.hollow.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.core.util.Colors;
import dev.hollow.detection.BlockResults;
import dev.hollow.detection.EntityTracker;
import dev.hollow.modules.BlockEspModule;
import dev.hollow.modules.BlockFinderModule;
import dev.hollow.modules.EntityTracersModule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws Block ESP and Entity Tracers geometry at the end of the main world pass, and collects
 * labels for the HUD. Geometry is batched by render type: all fills, then all lines.
 */
public final class WorldOverlayRenderer {
	public static final WorldOverlayRenderer INSTANCE = new WorldOverlayRenderer();

	private final List<OverlayLabel> labels = new ArrayList<>();
	private final List<Box> fills = new ArrayList<>();
	private final List<Box> outlines = new ArrayList<>();
	private final List<Line> lines = new ArrayList<>();
	private BlockResults blockResults;

	private record Box(AABB box, int color, float width, boolean seeThrough) {
	}

	private record Line(Vector3f from, Vector3f to, int color, float width) {
	}

	private WorldOverlayRenderer() {
	}

	public void setBlockResults(BlockResults results) {
		this.blockResults = results;
	}

	/** Labels gathered during the last world render, nearest first. */
	public List<OverlayLabel> labels() {
		return labels;
	}

	public void render(WorldRenderContext context) {
		labels.clear();
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}
		Vec3 camera = context.worldState().cameraRenderState.pos;
		Camera mainCamera = minecraft.gameRenderer.getMainCamera();
		float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		fills.clear();
		outlines.clear();
		lines.clear();

		collectBlocks(camera, mainCamera);
		collectEntities(camera, mainCamera, partialTick);
		if (fills.isEmpty() && outlines.isEmpty() && lines.isEmpty()) {
			return;
		}

		PoseStack poses = context.matrices();
		poses.pushPose();
		PoseStack.Pose pose = poses.last();
		MultiBufferSource consumers = context.consumers();
		for (boolean seeThrough : new boolean[] {false, true}) {
			VertexConsumer quads = null;
			for (Box box : fills) {
				if (box.seeThrough() == seeThrough) {
					if (quads == null) {
						quads = consumers.getBuffer(HollowPipelines.fill(seeThrough));
					}
					WorldDraw.boxFill(pose, quads, box.box(), box.color());
				}
			}
		}
		for (boolean seeThrough : new boolean[] {false, true}) {
			VertexConsumer buffer = null;
			for (Box box : outlines) {
				if (box.seeThrough() == seeThrough) {
					if (buffer == null) {
						buffer = consumers.getBuffer(HollowPipelines.lines(seeThrough));
					}
					WorldDraw.boxOutline(pose, buffer, box.box(), box.color(), box.width());
				}
			}
			if (seeThrough && !lines.isEmpty()) {
				if (buffer == null) {
					buffer = consumers.getBuffer(HollowPipelines.lines(true));
				}
				for (Line line : lines) {
					WorldDraw.line(pose, buffer, line.from().x(), line.from().y(), line.from().z(),
						line.to().x(), line.to().y(), line.to().z(), line.color(), line.width());
				}
			}
		}
		poses.popPose();
		labels.sort(Comparator.comparingDouble(OverlayLabel::distance));
	}

	private void collectBlocks(Vec3 camera, Camera mainCamera) {
		BlockFinderModule finder = Hollow.module(BlockFinderModule.class);
		BlockEspModule esp = Hollow.module(BlockEspModule.class);
		if (blockResults == null || !finder.isEnabled() || !esp.isEnabled()) {
			return;
		}
		BoxStyle style = esp.style.get();
		float lineWidth = esp.lineWidth.getFloat();
		boolean seeThrough = esp.seeThrough.get();
		Vector3f origin = tracerOrigin(mainCamera, esp.tracerOrigin.get());
		int labelBudget = esp.labels.get() ? esp.maxLabels.getInt() : 0;
		for (BlockResults.Cluster cluster : blockResults.visible()) {
			BlockFinderModule.CategorySettings settings = finder.category(cluster.categoryId());
			double range = Math.min(finder.radius.get(), settings.range.get());
			float fade = esp.fade.get() ? (float) (1 - 0.65 * Math.min(1, cluster.distance() / Math.max(1, range))) : 1f;
			int color = settings.color.argb();
			AABB box = cluster.box().move(-camera.x, -camera.y, -camera.z);
			if (style.fill()) {
				fills.add(new Box(box, Colors.withAlpha(color, Math.round(esp.fillOpacity.getFloat() * 255 * fade)), 0, seeThrough));
			}
			if (style.outline()) {
				outlines.add(new Box(box, Colors.fade(color, fade), lineWidth, seeThrough));
			}
			if (esp.tracers.get() && settings.tracer.get()) {
				Vec3 center = box.getCenter();
				lines.add(new Line(origin, new Vector3f((float) center.x, (float) center.y, (float) center.z),
					Colors.fade(color, esp.tracerOpacity.getFloat() * fade), esp.tracerWidth.getFloat()));
			}
			if (labelBudget > 0) {
				labelBudget--;
				Vec3 top = cluster.box().getCenter().add(0, cluster.box().getYsize() / 2 + 0.25, 0);
				String title = cluster.count() > 1 ? settings.name() + " ×" + cluster.count() : settings.name();
				String detail = esp.distance.get() ? Alert.formatDistance(cluster.distance()) : "";
				labels.add(new OverlayLabel(top.x, top.y, top.z, title, detail, color, cluster.distance(), esp.labelScale.getFloat()));
			}
		}
	}

	private void collectEntities(Vec3 camera, Camera mainCamera, float partialTick) {
		EntityTracersModule module = Hollow.module(EntityTracersModule.class);
		EntityTracker tracker = Hollow.entities();
		if (!module.isEnabled() || tracker == null) {
			return;
		}
		Vector3f origin = tracerOrigin(mainCamera, module.origin.get());
		List<EntityTracker.Tracked> tracked = new ArrayList<>(tracker.tracked());
		Vec3 eye = mainCamera.position();
		tracked.sort(Comparator.comparingDouble(t -> t.entity().position().distanceToSqr(eye)));
		int labelBudget = module.names.get() || module.health.get() || module.distance.get() ? module.maxLabels.getInt() : 0;
		float opacity = module.opacity.getFloat();
		for (EntityTracker.Tracked entry : tracked) {
			Entity entity = entry.entity();
			if (entity.isRemoved()) {
				continue;
			}
			int color = module.category(entry.category()).color.argb();
			Vec3 position = entity.getPosition(partialTick);
			AABB worldBox = entity.getBoundingBox().move(position.subtract(entity.position()));
			AABB box = worldBox.move(-camera.x, -camera.y, -camera.z);
			if (module.boxes.get()) {
				if (module.boxFill.get()) {
					fills.add(new Box(box, Colors.withAlpha(color, 40), 0, module.seeThrough.get()));
				}
				outlines.add(new Box(box, Colors.fade(color, Math.max(opacity, 0.6f)), module.thickness.getFloat(), module.seeThrough.get()));
			}
			if (module.lines.get()) {
				double targetY = switch (module.target.get()) {
					case FEET -> box.minY;
					case BODY -> (box.minY + box.maxY) / 2;
					case HEAD -> box.minY + entity.getEyeHeight();
				};
				Vec3 center = box.getCenter();
				lines.add(new Line(origin, new Vector3f((float) center.x, (float) targetY, (float) center.z),
					Colors.fade(color, opacity), module.thickness.getFloat()));
			}
			if (labelBudget > 0) {
				labelBudget--;
				double distance = Math.sqrt(worldBox.distanceToSqr(eye));
				labels.add(entityLabel(module, entity, worldBox, color, distance));
			}
		}
	}

	private static OverlayLabel entityLabel(EntityTracersModule module, Entity entity, AABB worldBox, int color, double distance) {
		String title = module.names.get() ? entity.getDisplayName().getString() : "";
		List<String> parts = new ArrayList<>();
		if (module.health.get() && entity instanceof LivingEntity living) {
			parts.add(String.format(Locale.ROOT, "♥ %.0f/%.0f", living.getHealth(), living.getMaxHealth()));
		}
		if (module.distance.get()) {
			parts.add(Alert.formatDistance(distance));
		}
		Vec3 top = worldBox.getCenter().add(0, worldBox.getYsize() / 2 + 0.35, 0);
		return new OverlayLabel(top.x, top.y, top.z, title, String.join(" · ", parts), color, distance, module.labelScale.getFloat());
	}

	/** Camera-relative start point for tracers, just in front of the view. */
	private static Vector3f tracerOrigin(Camera camera, TracerOrigin origin) {
		Vector3fc forward = camera.forwardVector();
		Vector3fc up = camera.upVector();
		Vector3f point = new Vector3f(forward).mul(0.6f);
		switch (origin) {
			case SCREEN_BOTTOM -> point.add(new Vector3f(up).mul(-0.3f));
			case SCREEN_TOP -> point.add(new Vector3f(up).mul(0.3f));
			default -> {
			}
		}
		return point;
	}
}
