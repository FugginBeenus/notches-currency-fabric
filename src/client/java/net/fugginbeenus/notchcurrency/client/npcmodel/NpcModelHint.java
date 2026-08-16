package net.fugginbeenus.notchcurrency.client.npcmodel;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fugginbeenus.notchcurrency.compat.Chat;
import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;

import java.util.HashSet;
import java.util.Set;

public final class NpcModelHint {

    private NpcModelHint() {}
    private static final Set<String> mentioned = new HashSet<>();

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClientSide) return InteractionResult.PASS;
            if (!(entity instanceof NotchNpcEntity npc)) return InteractionResult.PASS;

            String modelId = npc.getModelId();
            String bundleId = NpcModelBundle.bundleIdOf(modelId);
            if (bundleId == null || NpcModelRegistry.forModelId(modelId) != null) {
                return InteractionResult.PASS;
            }
            if (!mentioned.add(bundleId)) return InteractionResult.PASS;

            Component get = Component.literal("[Get it now]").setStyle(Style.EMPTY
                    .withColor(ChatFormatting.GREEN)
                    .withBold(true)
                    .withClickEvent(Chat.runCommand("/npcmodels sync")));

            Msg.chat(player, Component.literal(npc.getName().getString())
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" is wearing a model you do not have, so it looks "
                            + "ordinary. It arrives on its own next time you join. ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(get));
            return InteractionResult.PASS;
        });
    }
    public static void reset() {
        mentioned.clear();
    }
}
