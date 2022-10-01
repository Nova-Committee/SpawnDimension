package dev.prefex.safenetherspawn.mixin;

import com.mojang.authlib.GameProfile;
import committee.nova.spawndimension.SpawnDimension;
import dev.prefex.safenetherspawn.util.SafeSpawn;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
	@Mutable
	@Final
	@Shadow public final MinecraftServer server;

	public ServerPlayerEntityMixin(MinecraftServer server, ServerWorld world, GameProfile profile) {
		super(world, world.getSpawnPos(), world.getSpawnAngle(), profile);
		this.server = server;
	}

	@Inject(method = "moveToSpawn", at = @At("HEAD"), cancellable = true)
	private void moveToSpawn(ServerWorld origin, CallbackInfo ci) {
		ServerWorld world = null;
        for (ServerWorld serverWorld : origin.getServer().getWorlds()) {
            if (serverWorld.getRegistryKey().getValue().equals(new Identifier(SpawnDimension.CONFIG.spawnDimension))) {
                world = serverWorld;
                break;
            }
        }
		if (world == null) {
			return;
		}
		spawnPointDimension = world.getRegistryKey();
		BlockPos blockPos = world.getSpawnPos();
		if (world.getDimension().isUltrawarm() && world.getServer().getSaveProperties().getGameMode() != GameMode.ADVENTURE) {
			int i = Math.max(0, server.getSpawnRadius(world));
			int j = MathHelper.floor(world.getWorldBorder().getDistanceInsideBorder(blockPos.getX(), blockPos.getZ()));
			if (j < i) {
				i = j;
			}
			if (j <= 1) {
				i = 1;
			}

			long l = i * 2L + 1;
			long m = l * l;
			int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
			int n = this.calculateSpawnOffsetMultiplier(k);
			int o = new Random().nextInt(k);


			for (int p = 0; p < k; ++p) {
				int q = (o + n * p) % k;
				int r = q % (i * 2 + 1);
				int s = q / (i * 2 + 1);
                BlockPos blockPos2 = SafeSpawn.findSpawn(world, blockPos.getX() + r - i, blockPos.getZ() + s - i);
				if (blockPos2 != null) {
					this.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
					if (world.isSpaceEmpty(this)) {
						break;
					}
				}
			}
		} else {
			this.refreshPositionAndAngles(blockPos, 0.0F, 0.0F);

			while(!world.isSpaceEmpty(this) && this.getY() < (double)(world.getTopY() - 1)) {
				this.setPosition(this.getX(), this.getY() + 1.0, this.getZ());
			}
		}
		ci.cancel();
	}

	@Inject(method = "onSpawn", at = @At("HEAD"))
	public void onSpawn(CallbackInfo ci) {
        ServerWorld world = server.getWorld(spawnPointDimension);
        if (SpawnDimension.CONFIG.lockSpawnDimension) {
            for (ServerWorld serverWorld : server.getWorlds()) {
                if (serverWorld.getRegistryKey().getValue().equals(new Identifier(SpawnDimension.CONFIG.spawnDimension))) {
                    world = serverWorld;
                    break;
                }
            }
        }
        if (world == null) {
            return;
        }
		teleport(world, getPos().getX(), getPos().getY(),getPos().getZ(),0,0);
		if (world.getBlockState(getBlockPos()).getMaterial().isLiquid()){
			this.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30));
		}
	}

	@Shadow
	private int calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
		throw new NotImplementedException();
	}

	@Shadow private RegistryKey<World> spawnPointDimension;

	@Shadow @Final public ServerPlayerInteractionManager interactionManager;

	@Shadow public abstract void teleport(ServerWorld targetWorld, double x, double y, double z, float yaw, float pitch);

	@Override
	public boolean isSpectator() {
		return this.interactionManager.getGameMode() == GameMode.SPECTATOR;
	}

	@Override
	public boolean isCreative() {
		return this.interactionManager.getGameMode() == GameMode.CREATIVE;
	}
}