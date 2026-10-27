package dev.hollow.bridge;

import dev.hollow.core.bridge.BridgeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Minecraft side of bridging: reading blocks, choosing a block from the hotbar and placing it
 * through the normal "use item on block" interaction, exactly as a right-click would.
 */
public final class Bridging {
	public enum Result {
		PLACED("Placed"),
		NOTHING_TO_DO("Ready"),
		NO_BLOCKS("Out of blocks"),
		OUT_OF_REACH("Out of reach"),
		NO_SURFACE("No surface to place against"),
		REJECTED("Placement rejected");

		private final String label;

		Result(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	private Bridging() {
	}

	public static BridgeMath.World world(Level level) {
		return new BridgeMath.World() {
			@Override
			public boolean solid(int x, int y, int z) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = level.getBlockState(pos);
				return !state.canBeReplaced() && !state.getCollisionShape(level, pos).isEmpty();
			}

			@Override
			public boolean replaceable(int x, int y, int z) {
				return level.getBlockState(new BlockPos(x, y, z)).canBeReplaced();
			}
		};
	}

	/** Full, non-falling blocks: the ones that make a safe bridge. */
	public static boolean isBridgeBlock(ItemStack stack) {
		if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
			return false;
		}
		Block block = blockItem.getBlock();
		if (block instanceof FallingBlock) {
			return false;
		}
		BlockState state = block.defaultBlockState();
		return Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
	}

	/** Hotbar slot holding bridge blocks: the selected one if suitable, otherwise the largest stack, or -1. */
	public static int hotbarBlockSlot(Inventory inventory) {
		if (isBridgeBlock(inventory.getItem(inventory.getSelectedSlot()))) {
			return inventory.getSelectedSlot();
		}
		int best = -1;
		int bestCount = 0;
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isBridgeBlock(stack) && stack.getCount() > bestCount) {
				best = slot;
				bestCount = stack.getCount();
			}
		}
		return best;
	}

	/** Total count of an item across the whole inventory. */
	public static int count(Inventory inventory, Item item) {
		int total = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/** Horizontal movement direction: actual velocity, or the input direction when nearly still. */
	public static Vec3 movement(LocalPlayer player) {
		Vec3 velocity = player.getDeltaMovement();
		if (velocity.x * velocity.x + velocity.z * velocity.z > 1e-5) {
			return new Vec3(velocity.x, 0, velocity.z);
		}
		float forward = player.input.getMoveVector().y;
		float strafe = player.input.getMoveVector().x;
		if (forward == 0 && strafe == 0) {
			return Vec3.ZERO;
		}
		double yaw = Math.toRadians(player.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		return new Vec3(strafe * cos - forward * sin, 0, forward * cos + strafe * sin);
	}

	/** Hit result for clicking the neighbour face named by the plan. */
	public static BlockHitResult hitFor(BridgeMath.Placement plan) {
		BlockPos neighbor = new BlockPos(plan.neighborX(), plan.neighborY(), plan.neighborZ());
		Direction face = Direction.getApproximateNearest(plan.faceX(), plan.faceY(), plan.faceZ());
		Vec3 hit = Vec3.atCenterOf(neighbor).add(plan.faceX() * 0.5, plan.faceY() * 0.5, plan.faceZ() * 0.5);
		return new BlockHitResult(hit, face, neighbor, false);
	}

	/**
	 * Places a bridge block for the plan. Uses the selected slot, or switches to a hotbar block when
	 * {@code autoSelect} is on. With {@code face} the player turns to look at the clicked face first.
	 */
	public static Result place(Minecraft minecraft, LocalPlayer player, BridgeMath.Placement plan, boolean autoSelect, boolean face) {
		Inventory inventory = player.getInventory();
		if (!isBridgeBlock(player.getMainHandItem())) {
			int slot = autoSelect ? hotbarBlockSlot(inventory) : -1;
			if (slot < 0) {
				return Result.NO_BLOCKS;
			}
			inventory.setSelectedSlot(slot);
		}
		BlockHitResult hit = hitFor(plan);
		if (player.getEyePosition().distanceTo(hit.getLocation()) > player.blockInteractionRange()) {
			return Result.OUT_OF_REACH;
		}
		if (face) {
			Vec3 eye = player.getEyePosition();
			Vec3 d = hit.getLocation().subtract(eye);
			player.setYRot((float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
			player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
		}
		InteractionResult result = minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
		if (result.consumesAction()) {
			player.swing(InteractionHand.MAIN_HAND);
			return Result.PLACED;
		}
		return Result.REJECTED;
	}
}
