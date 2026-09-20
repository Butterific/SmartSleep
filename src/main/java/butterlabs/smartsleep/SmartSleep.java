package butterlabs.smartsleep;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SmartSleep implements ModInitializer {
	public static final String modId = "smart-sleep";
	public static final Logger logger = LoggerFactory.getLogger(modId);

	private final Map<UUID, Long> cooldowns = new HashMap<>();

	private boolean isBedBlock(Block block) {
		if (block instanceof BedBlock) {
			return true;
		}
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		return id != null && id.getPath().contains("straw_bed");
	}

	@Override
	public void onInitialize() {
		logger.info("Hello Fabric world!");

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide()) {
				long currentTime = System.currentTimeMillis();
				UUID playerUuid = player.getUUID();
				if (cooldowns.containsKey(playerUuid) && (currentTime - cooldowns.get(playerUuid)) < 3000) {
					return InteractionResult.PASS;
				}

				BlockPos pos = hitResult.getBlockPos();
				BlockState state = world.getBlockState(pos);

				if (isBedBlock(state.getBlock())) {
					cooldowns.put(playerUuid, currentTime);

					AABB searchArea = new AABB(pos).inflate(16.0);
					List<Monster> monsters = world.getEntitiesOfClass(
							Monster.class,
							searchArea,
							mob -> mob.isAlive()
					);

					Scoreboard scoreboard = world.getScoreboard();
					PlayerTeam redTeam = scoreboard.getPlayerTeam("red_glow");

					if (redTeam == null) {
						redTeam = scoreboard.addPlayerTeam("red_glow");
						redTeam.setColor(Optional.ofNullable(TeamColor.byName("red")));
					}

					int counters = 0;
					double closestDistance = Double.MAX_VALUE;

					for (Monster monster : monsters) {
						counters += 1;
						double dist = monster.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
						if (dist < closestDistance) {
							closestDistance = dist;
						}

						scoreboard.addPlayerToTeam(monster.getScoreboardName(), redTeam);
						monster.addEffect(new MobEffectInstance(
								MobEffects.GLOWING,
								100,
								0
						));
					}

					if (counters > 0) {
						world.playSound(
								null,
								pos,
								SoundEvents.ANVIL_LAND,
								SoundSource.BLOCKS,
								0.5F,
								1.5F
						);

						if (world instanceof ServerLevel serverWorld) {
							serverWorld.sendParticles(
									ParticleTypes.ANGRY_VILLAGER,
									pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
									5,
									0.2, 0.2, 0.2,
									0.05
							);
						}

						if (closestDistance <= 25.0) {
							player.sendSystemMessage(Component.literal("Mobs are right next to you! (" + counters + " total)")
									.withStyle(ChatFormatting.DARK_RED));
						} else {
							player.sendSystemMessage(Component.literal("There are " + counters + " mobs nearby!")
									.withStyle(ChatFormatting.YELLOW));
						}
					} else {
						player.sendSystemMessage(Component.literal("No mobs nearby, area is safe!")
								.withStyle(ChatFormatting.GREEN));
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(modId, path);
	}
}