package dev.prefex.safenetherspawn.util;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Random;

public class SpawnHelper
{
	public static void respawnInNether(Entity player)
	{
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

		if (player.world instanceof ServerWorld)
		{
			ServerWorld overworld = (ServerWorld) player.world;
			MinecraftServer minecraftserver = overworld.getServer();
			RegistryKey<World> registrykey = player.world.getRegistryKey() == World.NETHER ? World.OVERWORLD : World.NETHER;
			ServerWorld nether = minecraftserver.getWorld(registrykey);

			if (nether != null && minecraftserver.isNetherAllowed() && !player.hasPassengers())
			{
				BlockPos spawnPos;

				double posX = player.getX();
				double posZ = player.getZ();
				posX /= 8.0D;
				posZ /= 8.0D;

				double d0 = Math.min(-2.9999872E7D, nether.getWorldBorder().getBoundNorth() + 16.0D);
				double d1 = Math.min(-2.9999872E7D, nether.getWorldBorder().getBoundWest() + 16.0D);
				double d2 = Math.min(2.9999872E7D, nether.getWorldBorder().getBoundSouth() - 16.0D);
				double d3 = Math.min(2.9999872E7D, nether.getWorldBorder().getBoundEast() - 16.0D);
				posX = MathHelper.clamp(posX, d0, d2);
				posZ = MathHelper.clamp(posZ, d1, d3);
				spawnPos = new BlockPos(posX, player.getY(), posZ);

				// Teleport to the Nether
				serverPlayer.teleport(nether, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYaw(0.0f), player.getPitch(0.0f));

				// Find a safe spawn position
				spawnPos = getSafeSpawn(player, nether);

				// Teleport to safe spawn position
				serverPlayer.teleport(nether, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYaw(0.0f), player.getPitch(0.0f));

				player.world.getProfiler().endTick();
			}
		}
	}

	public static BlockPos getSafeSpawn(Entity entityIn, ServerWorld world)
	{
		Random random = new Random(world.getSeed());
		double d0 = -1.0D;
		int xPos = MathHelper.floor(entityIn.getX());
		int yPos = MathHelper.floor(entityIn.getY());
		int zPos = MathHelper.floor(entityIn.getZ());
		int xPosCopy = xPos;
		int yPosCopy = yPos;
		int zPosCopy = zPos;
		int l1 = 0;
		int i2 = random.nextInt(4);
		BlockPos.Mutable blockPosMutable = new BlockPos.Mutable();

		for (int j2 = xPos - 16; j2 <= xPos + 16; ++j2)
		{
			double d1 = (double) j2 + 0.5D - entityIn.getX();

			for (int l2 = zPos - 16; l2 <= zPos + 16; ++l2)
			{
				double d2 = (double) l2 + 0.5D - entityIn.getZ();

				label276:
				for (int j3 = world.getTopY() - 1; j3 >= 0; --j3)
				{
					if (world.isAir(blockPosMutable.set(j2, j3, l2)))
					{
						while (j3 > 0 && world.isAir(blockPosMutable.set(j2, j3 - 1, l2)))
						{
							--j3;
						}

						for (int k3 = i2; k3 < i2 + 4; ++k3)
						{
							int l3 = k3 % 2;
							int i4 = 1 - l3;
							if (k3 % 4 >= 2)
							{
								l3 = -l3;
								i4 = -i4;
							}

							for (int j4 = 0; j4 < 3; ++j4)
							{
								for (int k4 = 0; k4 < 4; ++k4)
								{
									for (int l4 = -1; l4 < 4; ++l4)
									{
										int i5 = j2 + (k4 - 1) * l3 + j4 * i4;
										int j5 = j3 + l4;
										int k5 = l2 + (k4 - 1) * i4 - j4 * l3;
										blockPosMutable.set(i5, j5, k5);
										if (l4 < 0 && !world.getBlockState(blockPosMutable).getMaterial().isSolid() || l4 >= 0 && !world.isAir(blockPosMutable))
										{
											continue label276;
										}
									}
								}
							}

							double d5 = (double) j3 + 0.5D - entityIn.getY();
							double d7 = d1 * d1 + d5 * d5 + d2 * d2;
							if (d0 < 0.0D || d7 < d0)
							{
								d0 = d7;
								xPosCopy = j2;
								yPosCopy = j3;
								zPosCopy = l2;
								l1 = k3 % 4;
							}
						}
					}
				}
			}
		}

		if (d0 < 0.0D)
		{
			for (int l5 = xPos - 16; l5 <= xPos + 16; ++l5)
			{
				double d3 = (double) l5 + 0.5D - entityIn.getX();

				for (int j6 = zPos - 16; j6 <= zPos + 16; ++j6)
				{
					double d4 = (double) j6 + 0.5D - entityIn.getZ();

					label214:
					for (int i7 = world.getTopY() - 1; i7 >= 0; --i7)
					{
						if (world.isAir(blockPosMutable.set(l5, i7, j6)))
						{
							while (i7 > 0 && world.isAir(blockPosMutable.set(l5, i7 - 1, j6)))
							{
								--i7;
							}

							for (int l7 = i2; l7 < i2 + 2; ++l7)
							{
								int l8 = l7 % 2;
								int k9 = 1 - l8;

								for (int i10 = 0; i10 < 4; ++i10)
								{
									for (int k10 = -1; k10 < 4; ++k10)
									{
										int i11 = l5 + (i10 - 1) * l8;
										int j11 = i7 + k10;
										int k11 = j6 + (i10 - 1) * k9;
										blockPosMutable.set(i11, j11, k11);
										if (k10 < 0 && !world.getBlockState(blockPosMutable).getMaterial().isSolid() || k10 >= 0 && !world.isAir(blockPosMutable))
										{
											continue label214;
										}
									}
								}

								double d6 = (double) i7 + 0.5D - entityIn.getY();
								double d8 = d3 * d3 + d6 * d6 + d4 * d4;
								if (d0 < 0.0D || d8 < d0)
								{
									d0 = d8;
									xPosCopy = l5;
									yPosCopy = i7;
									zPosCopy = j6;
									l1 = l7 % 2;
								}
							}
						}
					}
				}
			}
		}

		int i6 = xPosCopy;
		int k2 = yPosCopy;
		int k6 = zPosCopy;
		int l6 = l1 % 2;
		int i3 = 1 - l6;
		if (l1 % 4 >= 2)
		{
			l6 = -l6;
			i3 = -i3;
		}

		if (d0 < 0.0D)
		{
			yPosCopy = MathHelper.clamp(yPosCopy, 70, world.getTopY() - 10);
			k2 = yPosCopy;

			for (int j7 = -1; j7 <= 1; ++j7)
			{
				for (int i8 = 1; i8 < 3; ++i8)
				{
					for (int i9 = -1; i9 < 3; ++i9)
					{
						int l9 = i6 + (i8 - 1) * l6 + j7 * i3;
						int j10 = k2 + i9;
						int l10 = k6 + (i8 - 1) * i3 - j7 * l6;
						boolean flag = i9 < 0;
						blockPosMutable.set(l9, j10, l10);
						world.setBlockState(blockPosMutable, flag ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState());
					}
				}
			}
		}

		for(int k7 = -1; k7 < 3; ++k7) {
			for(int j8 = -1; j8 < 4; ++j8) {
				if (k7 == -1 || k7 == 2 || j8 == -1 || j8 == 3) {
					blockPosMutable.set(i6 + k7 * l6, k2 + j8, k6 + k7 * i3);
				}
			}
		}

		for(int k8 = 0; k8 < 2; ++k8) {
			for(int j9 = 0; j9 < 3; ++j9) {
				blockPosMutable.set(i6 + k8 * l6, k2 + j9 - 2, k6 + k8 * i3);
			}
		}

		return blockPosMutable;
	}
}