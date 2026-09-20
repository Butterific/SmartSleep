package butterlabs.smartsleep;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SmartSleep implements ModInitializer {
	public static final String MOD_ID = "smart-sleep";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

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

					for (Monster monster : monsters) {
						monster.addEffect(new MobEffectInstance(
								MobEffects.GLOWING,
								10000,
								0
						));
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}