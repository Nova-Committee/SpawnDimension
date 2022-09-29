package dev.prefex.safenetherspawn.mixin;

import com.mojang.authlib.GameProfile;
import dev.prefex.safenetherspawn.util.SafeSpawn;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
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
		System.out.println("WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!WARNING!!!");
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

	@Inject(method = "moveToSpawn", at = @At("HEAD"))
	public void onSpawn(ServerWorld origin, CallbackInfo ci) {
		ServerWorld world = ((ServerWorld)origin).getServer().getWorld(World.NETHER);
		if (world == null) {
			return;
		}
		moveToWorld(world);
	}

	@Shadow
	private int calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
		throw new NotImplementedException();
	}

	@Inject(method = "getSpawnPointDimension", at = @At("RETURN"), cancellable = true)
	public void getSpawnPointDimension(CallbackInfoReturnable<RegistryKey<World>> cir) {
		cir.setReturnValue(World.NETHER);
	}

	@Inject(method = "setSpawnPoint", at = @At("HEAD"), cancellable = true)
	public void setSpawnPoint(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
		if (pos != null) {
			boolean bl = pos.equals(this.spawnPointPosition) && dimension.equals(this.spawnPointDimension);
			if (sendMessage && !bl) {
				this.sendSystemMessage(new TranslatableText("block.minecraft.set_spawn"), Util.NIL_UUID);
			}

			this.spawnPointPosition = pos;
			this.spawnPointDimension = World.NETHER;
			this.spawnAngle = angle;
			this.spawnForced = forced;
		} else {
			this.spawnPointPosition = null;
			this.spawnPointDimension = World.NETHER;
			this.spawnAngle = 0.0F;
			this.spawnForced = false;
		}

		ci.cancel();
	}
	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci){
		spawnPointDimension = World.NETHER;
	}

	@Shadow private RegistryKey<World> spawnPointDimension;
	@Shadow private BlockPos spawnPointPosition;
	@Shadow private float spawnAngle;
	@Shadow private boolean spawnForced;


	@Override
	public boolean isSpectator() {
		return false;
	}

	@Override
	public boolean isCreative() {
		return false;
	}

	/*@Shadow private RegistryKey<World> spawnPointDimension;

	@Inject(
			method = "<init>",
			at = @At("HEAD")
	)
	private static void ServerPlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile, CallbackInfo ci){
		spawnPointDimension = World.NETHER;
	}*/
}