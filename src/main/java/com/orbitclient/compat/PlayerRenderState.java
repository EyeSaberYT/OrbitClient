package com.orbitclient.compat;

//? if <1.21.2 {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;

/^*
 * Before 1.21.2 (no entity render states): the parts of 1.21.2's PlayerRenderState that Orbit's cape and
 * cosmetics read, filled from the player for one frame the way 1.21.2 does it. build.gradle swaps the import.
 ^/
public final class PlayerRenderState {
	public int id;
	public boolean isInvisible;
	public boolean showCape;
	public PlayerSkin skin;
	public ItemStack chestItem = ItemStack.EMPTY;
	/^* Where the player is drawn this frame, and facing. ^/
	public double x, y, z;
	public float bodyRot;
	public float walkAnimationPos, walkAnimationSpeed;
	public boolean isFallFlying;
	/^* Degrees the cape leans back (from moving forward) and flaps up (falling, walking, crouching). ^/
	public float capeLean;
	public float capeFlap;

	public static PlayerRenderState of(AbstractClientPlayer player, float partialTick) {
		PlayerRenderState state = new PlayerRenderState();
		state.id = player.getId();
		state.isInvisible = player.isInvisible();
		state.showCape = player.isModelPartShown(PlayerModelPart.CAPE);
		state.skin = player.getSkin();
		state.chestItem = player.getItemBySlot(EquipmentSlot.CHEST);
		state.x = Mth.lerp(partialTick, player.xo, player.getX());
		state.y = Mth.lerp(partialTick, player.yo, player.getY());
		state.z = Mth.lerp(partialTick, player.zo, player.getZ());
		state.bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
		state.walkAnimationPos = player.walkAnimation.position(partialTick);
		state.walkAnimationSpeed = player.walkAnimation.speed(partialTick);
		state.isFallFlying = player.isFallFlying();
		// Vanilla's cape swing (CapeLayer before 1.21.2, the render state after).
		double dx = Mth.lerp(partialTick, player.xCloakO, player.xCloak) - Mth.lerp(partialTick, player.xo, player.getX());
		double dy = Mth.lerp(partialTick, player.yCloakO, player.yCloak) - Mth.lerp(partialTick, player.yo, player.getY());
		double dz = Mth.lerp(partialTick, player.zCloakO, player.zCloak) - Mth.lerp(partialTick, player.zo, player.getZ());
		double sin = Mth.sin(state.bodyRot * Mth.DEG_TO_RAD), cos = -Mth.cos(state.bodyRot * Mth.DEG_TO_RAD);
		float flap = Mth.clamp((float) dy * 10f, -6f, 32f);
		state.capeLean = Mth.clamp((float) (dx * sin + dz * cos) * 100f, 0f, 150f);
		float bob = Mth.lerp(partialTick, player.oBob, player.bob);
		flap += Mth.sin(Mth.lerp(partialTick, player.walkDistO, player.walkDist) * 6f) * 32f * bob;
		if (player.isCrouching()) flap += 25f;
		state.capeFlap = flap;
		return state;
	}
}
*///?}
