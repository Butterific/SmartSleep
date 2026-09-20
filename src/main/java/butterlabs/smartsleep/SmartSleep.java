package butterlabs.smartsleep;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class SmartSleep implements ModInitializer {
	public static final String modId = "smart-sleep";
	public static final Logger logger = LoggerFactory.getLogger(modId);

	@Override
	public void onInitialize() {
		logger.info("Hello Fabric world!");

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide()) {
				BlockPos pos = hitResult.getBlockPos();
				BlockState state = world.getBlockState(pos);

				if (state.getBlock() instanceof BedBlock) {
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
					for (Monster monster : monsters) {
						counters += 1;
						scoreboard.addPlayerToTeam(monster.getScoreboardName(), redTeam);
						monster.addEffect(new MobEffectInstance(
								MobEffects.GLOWING,
								100,
								0
						));
					}

					if (counters > 0) {
						player.sendSystemMessage(Component.literal("There are " + counters + " mobs outside!"));
					} else {
						player.sendSystemMessage(Component.literal("No mobs nearby!"));
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