package com.orbitclient.legacy.mixin;

import com.orbitclient.cosmetics.Cosmetic;
import com.orbitclient.cosmetics.CosmeticManager;
import com.orbitclient.cosmetics.CosmeticSlot;
import com.orbitclient.cosmetics.Cosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Orbit capes: a player wearing one shows its texture where the vanilla cape would be. */
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
	// Legacy yarn names these two getters the wrong way round: "getSkinId" is the one the cape layer reads
	// (and "getCapeId" returns the skin).
	@Inject(method = "getSkinId", at = @At("HEAD"), cancellable = true)
	private void orbit$cape(CallbackInfoReturnable<Identifier> cir) {
		AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
		CosmeticManager.Loadout loadout = CosmeticManager.of(self.getUuid());
		CosmeticManager.Worn worn = loadout == null ? null : loadout.get(CosmeticSlot.CAPE);
		Cosmetic cape = worn == null ? null : Cosmetics.get(worn.id());
		if (cape == null) return;
		Identifier texture = cape.capeTexture(System.nanoTime() / 1e9 + (self.getEntityId() % 97) * 0.37);
		com.orbitclient.legacy.Platform.ensureTexture(net.minecraft.client.MinecraftClient.getInstance(), texture);
		cir.setReturnValue(texture);
	}
}
