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

/**
 * Tells a player, once, when they walk up to an NPC wearing a model they do not have.
 *
 * <p>Since a model shared with a server is not forced on anybody already playing, an NPC can be
 * wearing something a given player cannot see yet. It falls back to the ordinary look, which is
 * fine, but silently looking wrong is worse than saying so. Talking to it offers the fix and lets
 * them decide when to take it.
 */
public final class NpcModelHint {

    private NpcModelHint() {}

    /** Which models have already been mentioned, so this says its piece once and then shuts up. */
    private static final Set<String> mentioned = new HashSet<>();

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClientSide) return InteractionResult.PASS;
            if (!(entity instanceof NotchNpcEntity npc)) return InteractionResult.PASS;

            String modelId = npc.getModelId();
            String bundleId = NpcModelBundle.bundleIdOf(modelId);
            // Only for a model this NPC is wearing that we do not have a copy of.
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

            // Never eats the interaction. The NPC still does whatever it was going to do.
            return InteractionResult.PASS;
        });
    }

    /** A new server is a new set of models, so anything said on the last one is forgotten. */
    public static void reset() {
        mentioned.clear();
    }
}
