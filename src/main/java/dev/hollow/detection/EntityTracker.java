package dev.hollow.detection;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.modules.EntityTracersModule;
import dev.hollow.modules.ServerRulesModule;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Builds the per-tick list of entities Entity Tracers draws and raises approach alerts.
 * Only entities the client already tracks for rendering are considered; invisible entities
 * and spectators are skipped so nothing hidden is revealed.
 */
public final class EntityTracker {
	public record Tracked(Entity entity, EntityCategory category) {
	}

	private List<Tracked> tracked = List.of();
	private IntOpenHashSet insideAlertRadius = new IntOpenHashSet();

	public List<Tracked> tracked() {
		return tracked;
	}

	public void clear() {
		tracked = List.of();
		insideAlertRadius.clear();
	}

	public void tick(Minecraft minecraft) {
		EntityTracersModule module = Hollow.module(EntityTracersModule.class);
		ServerRulesModule rules = Hollow.module(ServerRulesModule.class);
		LocalPlayer player = minecraft.player;
		if (!module.isEnabled() || minecraft.level == null || player == null || !rules.detectionAllowed()) {
			clear();
			return;
		}
		EntityFilter filter = module.filter(rules.playersAllowed());
		double maxDistance = module.maxDistance.get();
		double maxSq = maxDistance * maxDistance;
		double alertRadius = module.alertRadius.get();
		double alertSq = alertRadius * alertRadius;

		List<Tracked> next = new ArrayList<>();
		IntOpenHashSet inside = new IntOpenHashSet();
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity == player || entity.isRemoved() || entity.isInvisibleTo(player)
					|| entity instanceof Player other && other.isSpectator()) {
				continue;
			}
			double distanceSq = entity.distanceToSqr(player);
			if (distanceSq > maxSq) {
				continue;
			}
			String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			EntityCategory category = filter.resolve(typeId, EntityClassifier.classify(entity));
			if (category == null) {
				continue;
			}
			next.add(new Tracked(entity, category));
			if (distanceSq <= alertSq) {
				inside.add(entity.getId());
				if (!insideAlertRadius.contains(entity.getId()) && module.category(category).alert.get()) {
					raise(entity, category, typeId, Math.sqrt(distanceSq));
				}
			}
		}
		tracked = List.copyOf(next);
		insideAlertRadius = inside;
	}

	private static void raise(Entity entity, EntityCategory category, String typeId, double distance) {
		String name = entity.getDisplayName().getString();
		Hollow.alerts().report(new Alert(category.alertSource(), category.singular(), "Nearby", distance,
			"entity:" + category.id() + ":" + typeId, name));
	}
}
