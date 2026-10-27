package dev.hollow.mixin;

import dev.hollow.perf.FpsBoost;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FPS Boost: new particles are skipped while the configured number are alive. */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
	@Shadow
	@Final
	private Map<ParticleRenderType, ParticleGroup<?>> particles;
	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void hollow$cap(Particle particle, CallbackInfo ci) {
		if (FpsBoost.dropParticle(particlesToAdd.size())) {
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void hollow$count(CallbackInfo ci) {
		int count = 0;
		for (ParticleGroup<?> group : particles.values()) {
			count += group.size();
		}
		FpsBoost.particleCount(count);
	}
}
