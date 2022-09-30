package dev.prefex.safenetherspawn.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import dev.prefex.safenetherspawn.util.SafeSpawn;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.TagKey;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

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
		ServerWorld world = ((ServerWorld)origin).getServer().getWorld(World.NETHER);
		if (world == null) {
			return;
		}

		spawnPointDimension = World.NETHER;
		BlockPos blockPos = world.getSpawnPos();
		if (world.getDimension().isUltrawarm() && world.getServer().getSaveProperties().getGameMode() != GameMode.ADVENTURE) {
			int i = Math.max(0, server.getSpawnRadius(world));
			int j = MathHelper.floor(world.getWorldBorder().getDistanceInsideBorder((double)blockPos.getX(), (double)blockPos.getZ()));
			if (j < i) {
				i = j;
			}

			if (j <= 1) {
				i = 1;
			}

			long l = (long)(i * 2 + 1);
			long m = l * l;
			int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
			int n = this.calculateSpawnOffsetMultiplier(k);
			int o = (new Random()).nextInt(k);

			for(int p = 0; p < k; ++p) {
				int q = (o + n * p) % k;
				int r = q % (i * 2 + 1);
				int s = q / (i * 2 + 1);
				BlockPos blockPos2 = SafeSpawn.findNetherSpawn(world, blockPos.getX() + r - i, blockPos.getZ() + s - i);
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
		ServerWorld world = server.getWorld(World.NETHER);
		if (world == null) {
			return;
		}
		teleport(world, getPos().getX(), getPos().getY(),getPos().getZ(),0,0);
		if (world.getBlockState(getBlockPos()).getMaterial().isLiquid()){
			this.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 30));
		}
		/*else if (!world.getBlockState(getBlockPos()).isAir()||(world.getBlockState(getBlockPos().down()).isAir() && world.getBlockState(getBlockPos().down(2)).isAir())){
			Registry<ConfiguredStructureFeature<?, ?>> registry = world.getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY);

			var pos = world.locateStructure(TagKey.of(registry.getKey(),new Identifier("minecraft:ruined_portal")),getBlockPos(), 100, false);
			teleport(world,pos.getX(),getBlockY(),pos.getZ(),0,0);

		}*/
		//moveToWorld(world);
	}

	@Shadow
	private int calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
		throw new NotImplementedException();
	}

	@Shadow private RegistryKey<World> spawnPointDimension;

	@Shadow @Final public ServerPlayerInteractionManager interactionManager;

	@Shadow public abstract void teleport(ServerWorld targetWorld, double x, double y, double z, float yaw, float pitch);

	@Shadow public abstract @Nullable BlockPos getSpawnPointPosition();

	@Override
	public boolean isSpectator() {
		return this.interactionManager.getGameMode() == GameMode.SPECTATOR;
	}

	@Override
	public boolean isCreative() {
		return this.interactionManager.getGameMode() == GameMode.CREATIVE;
	}
}