package net.fugginbeenus.notchcurrency.npc;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.shop.PlayerShop;
import net.fugginbeenus.notchcurrency.shop.PlayerShopManager;
import net.fugginbeenus.notchcurrency.shop.ShopState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class NotchNpcManager {

    public static final String ITEM_TAG = "NotchNpc";

    private NotchNpcManager() {}

    // ---- interaction ----

    public static void dispatchRole(ServerPlayer sp, NotchNpcEntity npc) {
        NpcRoleDispatch.open(sp, npc.getRole(), npc.getRoleTarget(), npc);
    }

    public static void openEditor(ServerPlayer sp, NotchNpcEntity npc) {
        if (!npc.canEdit(sp)) {
            sp.displayClientMessage(Component.literal("Only the owner can edit this NPC.").withStyle(ChatFormatting.RED), false);
            return;
        }
        // Back to '&' on the way out. The name is stored with real § codes because that is what
        // renders, but the editor field has to show what was typed or every save would hand back
        // section signs and the codes would stop being editable.
        String name = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString().replace('\u00a7', '&') : "";
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        buf.writeVarInt(npc.getRole().ordinal());
        buf.writeUtf(name);
        buf.writeUtf(npc.getOwnerName());
        buf.writeBoolean(true);
        buf.writeUtf(npc.getModelId());
        buf.writeUtf(npc.getSkinType());
        buf.writeUtf(npc.getSkinValue());
        buf.writeBoolean(npc.isSlim());
        buf.writeFloat(npc.npcScale());
        buf.writeFloat(npc.getScaleY());
        buf.writeFloat(npc.getScaleZ());
        buf.writeFloat(npc.getNameOffset());
        buf.writeVarInt(npc.getBehavior().ordinal());
        buf.writeVarInt(npc.getWanderRadius());
        buf.writeVarInt(npc.getDialogue().size());
        buf.writeBoolean(npc.getDialogue().isFlat()); // flat = Quick Lines; branching = Studio only
        buf.writeVarInt(statsBits(npc));
        buf.writeVarInt(npc.getDialogueMode().ordinal());
        buf.writeVarInt(npc.getWaypoints().size());
        buf.writeVarInt(patrolSpeedIndex(npc));
        buf.writeVarInt(patrolWaitIndex(npc));
        buf.writeVarInt(npc.getNpcPose());
        buf.writeVarInt(npc.getPoseAnim());
        buf.writeVarInt((int) Math.round(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)));
        buf.writeVarInt((int) Math.round(npc.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) * 100));
        buf.writeVarInt(npc.getRegen());
        buf.writeUtf(npc.getFollowPlayerName());
        buf.writeVarInt((npc.avoidsMonsters() ? 1 : 0) | (npc.watchesPlayers() ? 2 : 0)
                | (npc.protectsOwner() ? 4 : 0) | (npc.attacksMonsters() ? 8 : 0)
                | (npc.isHostileToPlayers() ? 16 : 0) | (npc.fightsBack() ? 32 : 0)
                | (npc.fightsRivalFactions() ? 64 : 0));
        buf.writeUtf(npc.getFarewellText());
        buf.writeUtf(npc.getBillboard());
        buf.writeUtf(npc.getSubtitle());
        buf.writeUtf(npc.getVoice());
        buf.writeVarInt(npc.getVoicePitchPercent());
        Net.sendToClient(sp, NotchPackets.NPC_EDITOR_OPEN, buf);
    }

    public static void setDialogueMode(ServerPlayer sp, NotchNpcEntity npc, int modeOrdinal) {
        if (!guard(sp, npc)) return;
        NotchNpcEntity.DialogueMode[] modes = NotchNpcEntity.DialogueMode.values();
        npc.setDialogueMode((modeOrdinal >= 0 && modeOrdinal < modes.length)
                ? modes[modeOrdinal] : NotchNpcEntity.DialogueMode.WINDOW);
    }

    public static int statsBits(NotchNpcEntity npc) {
        int bits = 0;
        if (npc.isProtectedNpc()) bits |= 1;
        if (npc.isSilent()) bits |= 2;
        if (npc.isCurrentlyGlowing()) bits |= 4;
        if (npc.isCustomNameVisible()) bits |= 8;
        if (npc.isNoGravity()) bits |= 16;
        if (npc.opensDoors()) bits |= 32;
        if (npc.isLeashable()) bits |= 64;
        if (npc.isManualInvisible()) bits |= 128;
        bits |= (npc.getVisibility() & 3) << 8; // bits 8-9 reserved for the visibility rule
        if (npc.isNpcPushable()) bits |= 1024;
        // Bits 2048/4096 used to carry hostile-to-players and fights-back. They live with the rest of
        // the combat settings on the Moves tab now: see the moves bits below.
        return bits;
    }

    public static void setStats(ServerPlayer sp, NotchNpcEntity npc, int bits) {
        if (!guard(sp, npc)) return;
        npc.setProtectedNpc((bits & 1) != 0);
        npc.setSilent((bits & 2) != 0);
        npc.setGlowingTag((bits & 4) != 0);
        npc.setCustomNameVisible((bits & 8) != 0);
        npc.setNoGravity((bits & 16) != 0);
        npc.setOpensDoors((bits & 32) != 0);
        npc.setLeashable((bits & 64) != 0);
        npc.setManualInvisible((bits & 128) != 0);
        npc.setVisibility((bits >> 8) & 3);
        npc.setNpcPushable((bits & 1024) != 0);
        // Apply the effective invisibility now rather than waiting for the next tick window.
        npc.setInvisible(npc.isManualInvisible() || npc.isRuleHidden());
    }

    public static void setAttrs(ServerPlayer sp, NotchNpcEntity npc, int maxHealth, int speedPct, int regen) {
        if (!guard(sp, npc)) return;
        npc.setBaseStats(maxHealth, speedPct);
        npc.setRegen(regen);
    }

    // ---- equipment ----

    public static void openEquipScreen(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Component.literal("NPC Equipment"),
                (containerId, inv, p) -> new NpcEquipScreenHandler(containerId, inv, new NpcEquipmentInventory(npc), npc),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUUID(npc.getUUID());
                });
    }

    public static void setAppearance(ServerPlayer sp, NotchNpcEntity npc,
                                     String model, String skinType, String skinValue, boolean slim,
                                     float scaleX, float scaleY, float scaleZ, float nameOffset) {
        if (!guard(sp, npc)) return;
        // URL skins are fetched by every client that sees the NPC: only allow real web URLs.
        if (NotchNpcEntity.SKIN_URL.equals(skinType) && !skinValue.isBlank()) {
            String lower = skinValue.trim().toLowerCase();
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                sp.displayClientMessage(Component.literal("Skin URLs must start with http:// or https://.")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
        }
        npc.setAppearance(model, skinType, skinValue, slim, scaleX, scaleY, scaleZ, nameOffset);
    }

    // Caps live on the entity, so a hand-made packet cannot post a wall of text.
    public static void setBillboard(ServerPlayer sp, NotchNpcEntity npc, String text) {
        if (!guard(sp, npc)) return;
        npc.setBillboard(text);
    }

    public static void setPose(ServerPlayer sp, NotchNpcEntity npc, int pose) {
        if (!guard(sp, npc)) return;
        npc.setNpcPose(pose);
    }

    public static void setPosePart(ServerPlayer sp, NotchNpcEntity npc, int part, int x, int y, int z) {
        if (!guard(sp, npc)) return;
        npc.setCustomPosePart(part, x, y, z);
        if (npc.getNpcPose() != NotchNpcEntity.POSE_CUSTOM) {
            npc.setNpcPose(NotchNpcEntity.POSE_CUSTOM);
        }
    }

    public static void setPoseAnim(ServerPlayer sp, NotchNpcEntity npc, int anim) {
        if (!guard(sp, npc)) return;
        npc.setPoseAnim(anim);
    }

    public static void transform(ServerPlayer sp, NotchNpcEntity npc, double dx, double dy, double dz,
                                 float yawDeg, boolean applyYaw) {
        if (!guard(sp, npc)) return;
        dx = net.minecraft.util.Mth.clamp(dx, -16.0, 16.0);
        dy = net.minecraft.util.Mth.clamp(dy, -16.0, 16.0);
        dz = net.minecraft.util.Mth.clamp(dz, -16.0, 16.0);
        double x = npc.getX() + dx;
        double y = Math.max(npc.level().getMinBuildHeight(), npc.getY() + dy);
        double z = npc.getZ() + dz;
        float yaw = applyYaw ? net.minecraft.util.Mth.wrapDegrees(yawDeg) : npc.getYRot();
        npc.moveTo(x, y, z, yaw, npc.getXRot());
        npc.setYHeadRot(yaw);
        npc.yBodyRot = yaw;
        npc.getNavigation().stop();
        npc.setHome(npc.blockPosition());
    }

    public static void setBehavior(ServerPlayer sp, NotchNpcEntity npc, int modeOrdinal, int radius,
                                   String followName, int movesBits) {
        if (!guard(sp, npc)) return;
        NotchNpcEntity.Behavior[] all = NotchNpcEntity.Behavior.values();
        NotchNpcEntity.Behavior mode = (modeOrdinal >= 0 && modeOrdinal < all.length)
                ? all[modeOrdinal] : NotchNpcEntity.Behavior.STATIONARY;
        npc.setWanderRadius(radius);
        npc.setFollowPlayerName(followName);
        npc.setAvoidMonsters((movesBits & 1) != 0);
        npc.setWatchPlayers((movesBits & 2) != 0);
        npc.setProtectOwner((movesBits & 4) != 0);
        npc.setAttackMonsters((movesBits & 8) != 0);
        npc.setHostileToPlayers((movesBits & 16) != 0);
        npc.setFightsBack((movesBits & 32) != 0);
        npc.setFightRivalFactions((movesBits & 64) != 0);
        npc.setBehavior(mode);
        String desc = switch (mode) {
            case STATIONARY -> "Stationary";
            case WANDER -> "Wander (radius " + npc.getWanderRadius() + ")";
            case FOLLOW_OWNER -> npc.getFollowPlayerName().isEmpty()
                    ? "Follow owner" : "Follow " + npc.getFollowPlayerName();
            case PATROL -> "Patrol (" + npc.getWaypoints().size() + " waypoint"
                    + (npc.getWaypoints().size() == 1 ? "" : "s") + ")";
            case GUARD -> "Guard (radius " + npc.getWanderRadius() + ")";
        };
        sp.displayClientMessage(Component.literal("Behavior set: " + desc + ".").withStyle(ChatFormatting.GREEN), false);

        // Follow with an unresolvable target does nothing: say why. (Common in dev, where each
        // launch gets a fresh random username/UUID.)
        if (mode == NotchNpcEntity.Behavior.FOLLOW_OWNER && npc.resolveFollowTarget() == null) {
            String who = npc.getFollowPlayerName().isEmpty()
                    ? "its owner (" + (npc.getOwnerName().isEmpty() ? "unknown" : npc.getOwnerName()) + ")"
                    : npc.getFollowPlayerName();
            sp.displayClientMessage(Component.literal("Note: " + who + " isn't online, so it has no one to follow.")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        if (mode == NotchNpcEntity.Behavior.PATROL && npc.getWaypoints().isEmpty()) {
            sp.displayClientMessage(Component.literal("Add waypoints (stand somewhere and click 'Add waypoint here') so it has a route.")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
    }

    // Index is shared with the editor's Speed and Wait cycles: reorder one and reorder both.
    public static final float[] PATROL_SPEEDS = {0.6f, 0.9f, 1.2f};
    public static final String[] PATROL_SPEED_NAMES = {"Stroll", "Walk", "Jog"};

    public static int patrolSpeedIndex(NotchNpcEntity npc) {
        int best = 0;
        for (int i = 1; i < PATROL_SPEEDS.length; i++) {
            if (Math.abs(npc.getPatrolSpeed() - PATROL_SPEEDS[i]) < Math.abs(npc.getPatrolSpeed() - PATROL_SPEEDS[best])) {
                best = i;
            }
        }
        return best;
    }

    public static final int[] PATROL_WAITS = {0, 40, 100, 200, 400};
    public static final String[] PATROL_WAIT_NAMES = {"None", "2s", "5s", "10s", "20s"};

    public static int patrolWaitIndex(NotchNpcEntity npc) {
        int best = 0;
        for (int i = 1; i < PATROL_WAITS.length; i++) {
            if (Math.abs(npc.getPatrolWaitTicks() - PATROL_WAITS[i]) < Math.abs(npc.getPatrolWaitTicks() - PATROL_WAITS[best])) {
                best = i;
            }
        }
        return best;
    }

    public static void patrolAction(ServerPlayer sp, NotchNpcEntity npc, int action, int value) {
        if (!guard(sp, npc)) return;
        switch (action) {
            case 0 -> {
                ItemStack tool = new ItemStack(ModItems.ROUTE_PLANNER);
                String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                        ? npc.getCustomName().getString() : "NPC";
                StackData.putUuid(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_KEY, npc.getUUID());
                StackData.putString(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_NAME_KEY, npcName);
                if (!sp.getInventory().add(tool)) {
                    sp.drop(tool, false);
                }
                sp.displayClientMessage(Component.literal("Route tool added - walk the route and right-click the ground to drop waypoints.")
                        .withStyle(ChatFormatting.GREEN), false);
            }
            case 1 -> {
                npc.clearWaypoints();
                sp.displayClientMessage(Component.literal("Waypoints cleared.").withStyle(ChatFormatting.GREEN), false);
            }
            case 2 -> confirmRoute(sp, npc);
            case 3 -> {
                int idx = Math.max(0, Math.min(PATROL_SPEEDS.length - 1, value));
                npc.setPatrolSpeed(PATROL_SPEEDS[idx]);
                sp.displayClientMessage(Component.literal("Patrol speed: " + PATROL_SPEED_NAMES[idx] + ".").withStyle(ChatFormatting.GREEN), false);
            }
            case 4 -> {
                int idx = Math.max(0, Math.min(PATROL_WAITS.length - 1, value));
                npc.setPatrolWaitTicks(PATROL_WAITS[idx]);
                sp.displayClientMessage(Component.literal("Waypoint wait: " + PATROL_WAIT_NAMES[idx] + ".").withStyle(ChatFormatting.GREEN), false);
            }
            default -> { }
        }
    }

    public static void addWaypointAt(ServerPlayer sp, NotchNpcEntity npc, net.minecraft.core.BlockPos pos) {
        if (!guard(sp, npc)) return;
        if (npc.addWaypoint(pos)) {
            sp.displayClientMessage(Component.literal("Waypoint " + npc.getWaypoints().size() + " added.")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            sp.displayClientMessage(Component.literal("Route is full (16 waypoints).").withStyle(ChatFormatting.RED), true);
        }
    }

    public static void removeLastWaypoint(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        if (npc.removeLastWaypoint()) {
            sp.displayClientMessage(Component.literal("Removed last waypoint (" + npc.getWaypoints().size() + " left).")
                    .withStyle(ChatFormatting.YELLOW), true);
        } else {
            sp.displayClientMessage(Component.literal("The route is already empty.").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    public static void confirmRoute(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        if (npc.getWaypoints().size() < 2) {
            sp.displayClientMessage(Component.literal("Add at least 2 waypoints first (right-click the ground).")
                    .withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        int removed = 0;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack st = sp.getInventory().getItem(i);
            if (st.getItem() instanceof net.fugginbeenus.notchcurrency.item.RoutePlannerItem
                    && npc.getUUID().equals(StackData.getUuid(st, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_KEY))) {
                sp.getInventory().setItem(i, ItemStack.EMPTY);
                removed++;
            }
        }
        npc.setBehavior(NotchNpcEntity.Behavior.PATROL);
        sp.displayClientMessage(Component.literal("Route confirmed - " + npc.getWaypoints().size()
                + " waypoints, patrol started." + (removed > 0 ? " The route tool vanished." : ""))
                .withStyle(ChatFormatting.GREEN), false);
    }

    // ---- dialogue setup ----

    private static net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree buildStarterTree(NotchNpcEntity npc) {
        var tree = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree();

        var about = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueNode("about");
        about.setText("I'm %npc%. I work here! Come see me any time.");
        about.withChoice(new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice("Back", "start"));
        about.withChoice(new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice("Goodbye", ""));

        var start = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueNode("start");
        start.setText("Hello, %player%! What can I do for you?");
        start.withChoice(new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice("Who are you?", "about"));
        if (npc.getRole() != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE
                && npc.getRole() != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.GREETER) {
            start.withChoice(roleEntryChoice(npc));
        }
        start.withChoice(new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice("Goodbye", ""));

        tree.put(start);
        tree.put(about);
        tree.setStartId("start");
        return tree;
    }

    public static void createDialogueTemplate(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        npc.setDialogue(buildStarterTree(npc));
        sp.displayClientMessage(Component.literal("Starter dialogue created - talk to the NPC to try it.").withStyle(ChatFormatting.GREEN), false);
    }

    public static void clearDialogue(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        npc.getDialogue().clear();
        sp.displayClientMessage(Component.literal("Dialogue cleared.").withStyle(ChatFormatting.GREEN), false);
    }

    public static void openStudio(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        var tree = npc.getDialogue().isEmpty() ? buildStarterTree(npc) : npc.getDialogue();
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        buf.writeNbt(tree.toNbt());
        Net.sendToClient(sp, NotchPackets.NPC_STUDIO_DATA, buf);
    }

    // Sizes come from the client, so everything is clamped here rather than trusted.
    public static void saveDialogue(ServerPlayer sp, NotchNpcEntity npc,
                                    net.minecraft.nbt.CompoundTag treeNbt) {
        if (!guard(sp, npc)) return;
        if (treeNbt == null) return;
        var tree = net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(treeNbt);
        if (tree.size() > 64) {
            sp.displayClientMessage(Component.literal("That dialogue is too large (max 64 pages).").withStyle(ChatFormatting.RED), false);
            return;
        }
        var clean = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree();
        boolean allowCommands = net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp);
        boolean strippedCommands = false;
        for (var node : tree.nodes().values()) {
            if (node.id().isBlank() || node.id().length() > 32) continue; // ids are <=24 in the studio
            if (node.text().length() > 500) node.setText(node.text().substring(0, 500));
            while (node.choices().size() > 6) node.choices().remove(node.choices().size() - 1);
            if (!allowCommands) {
                for (var choice : node.choices()) {
                    strippedCommands |= choice.actions().removeIf(a ->
                            net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.isAdminOnly(a.type()));
                }
            }
            clean.put(node);
        }
        if (strippedCommands) {
            sp.displayClientMessage(Component.literal("Actions that hand out coins, items or run commands were removed - those are admin-only.")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        clean.setStartId(tree.startId());
        if (clean.get(clean.startId()) == null && clean.size() > 0) {
            clean.setStartId(clean.nodes().keySet().iterator().next()); // start page was dropped
        }
        npc.setDialogue(clean);
        sp.displayClientMessage(Component.literal("Dialogue saved (" + clean.size() + " page" + (clean.size() == 1 ? "" : "s") + ").")
                .withStyle(ChatFormatting.GREEN), false);
    }

    public static void openActions(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        buf.writeNbt(npc.getActions().toNbt());
        Net.sendToClient(sp, NotchPackets.NPC_ACTIONS_DATA, buf);
    }

    public static void saveActions(ServerPlayer sp, NotchNpcEntity npc,
                                   net.minecraft.nbt.CompoundTag nbt) {
        if (!guard(sp, npc)) return;
        if (nbt == null) return;
        var actions = net.fugginbeenus.notchcurrency.npc.action.NpcActions.fromNbt(nbt);
        boolean stripped = false;
        boolean allowAdminActions = net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp);
        for (var trigger : net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.values()) {
            var kept = new java.util.ArrayList<>(actions.get(trigger));
            if (!allowAdminActions) {
                stripped |= kept.removeIf(a ->
                        net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.isAdminOnly(a.type()));
            }
            // The screen caps typing at 200, but the packet is whatever the client chose to send.
            for (var a : kept) {
                if (a.value().length() > 200) a.setValue(a.value().substring(0, 200));
                if (a.amount() < 0) a.setAmount(0);
            }
            actions.set(trigger, kept);
        }
        if (stripped) {
            sp.displayClientMessage(Component.literal("Actions that hand out coins, items or run commands were removed - those are admin-only.")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        npc.setActions(actions);
        sp.displayClientMessage(Component.literal("Reactions saved.").withStyle(ChatFormatting.GREEN), false);
    }

    public static void openSchedule(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        // Whether a schedule can run here at all. Sent with the data so the screen can explain itself
        // instead of the owner building a whole day that silently never advances.
        buf.writeBoolean(net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule
                .dimensionSupports(npc.level()));
        buf.writeNbt(npc.getSchedule().toNbt());
        Net.sendToClient(sp, NotchPackets.NPC_SCHEDULE_DATA, buf);
    }

    // Entry actions take the same admin gate as reactions: an entry can mint coins exactly like one.
    public static void saveSchedule(ServerPlayer sp, NotchNpcEntity npc,
                                    net.minecraft.nbt.CompoundTag nbt) {
        if (!guard(sp, npc)) return;
        if (nbt == null) return;
        var schedule = net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.fromNbt(nbt);
        boolean stripped = false;
        boolean allowAdminActions = net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp);

        var cleaned = new java.util.ArrayList<net.fugginbeenus.notchcurrency.npc.schedule.ScheduleEntry>();
        for (var entry : schedule.entries()) {
            var kept = new java.util.ArrayList<>(entry.onBegin());
            if (!allowAdminActions) {
                stripped |= kept.removeIf(a ->
                        net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.isAdminOnly(a.type()));
            }
            for (var a : kept) {
                if (a.value().length() > 200) a.setValue(a.value().substring(0, 200));
                if (a.amount() < 0) a.setAmount(0);
            }
            String closed = entry.closedLine();
            if (closed.length() > 200) closed = closed.substring(0, 200);
            cleaned.add(entry.withActions(kept).withClosedLine(closed));
        }
        schedule.setEntries(cleaned);

        if (stripped) {
            sp.displayClientMessage(Component.literal("Actions that hand out coins, items or run commands were removed - those are admin-only.")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        npc.setSchedule(schedule);
        int broken = schedule.brokenCount();
        if (broken > 0) {
            sp.displayClientMessage(Component.literal("Schedule saved. " + broken
                            + (broken == 1 ? " entry still needs a spot." : " entries still need a spot."))
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            sp.displayClientMessage(Component.literal("Schedule saved.").withStyle(ChatFormatting.GREEN), false);
        }
    }

    public static void giveScheduleTool(ServerPlayer sp, NotchNpcEntity npc, int entryIndex) {
        if (!guard(sp, npc)) return;
        var entry = npc.getSchedule().get(entryIndex);
        if (entry == null) {
            sp.displayClientMessage(Component.literal("That schedule entry is gone.").withStyle(ChatFormatting.RED), false);
            return;
        }
        // Clear any tool from a previous attempt first. Opening the picker, changing your mind and
        // opening it again is ordinary behaviour, and it must not leave an inventory full of
        // near-identical tools pointing at entries that may not exist any more.
        clearScheduleTools(sp);
        ItemStack tool = new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.ROUTE_PLANNER);
        StackData.putUuid(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_KEY, npc.getUUID());
        StackData.putString(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_NAME_KEY,
                net.fugginbeenus.notchcurrency.npc.NpcText.npcName(npc));
        StackData.putInt(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.ENTRY_KEY, entryIndex);
        if (!sp.getInventory().add(tool)) {
            sp.drop(tool, false);
        }
        boolean bed = entry.stance() == net.fugginbeenus.notchcurrency.npc.schedule.NpcStance.SLEEP;
        sp.displayClientMessage(Component.literal(bed
                        ? "Right-click the bed for the " + entry.clock() + " entry. Right-click the air to cancel."
                        : "Right-click the spot for the " + entry.clock() + " entry. Right-click the air to cancel.")
                .withStyle(ChatFormatting.GREEN), false);
    }

    public static void setScheduleAnchor(ServerPlayer sp, NotchNpcEntity npc, int entryIndex,
                                         net.minecraft.core.BlockPos pos, float facing) {
        if (!guard(sp, npc)) return;
        var schedule = npc.getSchedule();
        var entry = schedule.get(entryIndex);
        if (entry == null) {
            sp.displayClientMessage(Component.literal("That schedule entry is gone.").withStyle(ChatFormatting.RED), false);
            return;
        }
        var entries = new java.util.ArrayList<>(schedule.entries());
        entries.set(entryIndex, entry.withAnchor(pos, facing));
        schedule.setEntries(entries);
        npc.setSchedule(schedule);
        sp.displayClientMessage(Component.literal("Spot set for the " + entry.clock() + " entry.").withStyle(ChatFormatting.GREEN), false);
        openSchedule(sp, npc);
    }

    // Route tools are left alone. A route takes many clicks; a spot tool is spent in one, so a
    // leftover spot tool is always litter.
    public static void clearScheduleTools(ServerPlayer sp) {
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.getItem() instanceof net.fugginbeenus.notchcurrency.item.RoutePlannerItem
                    && StackData.has(st, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.ENTRY_KEY)) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        inv.setChanged();
        sp.containerMenu.broadcastChanges();
    }

    public static void reopenScheduleFor(ServerPlayer sp, ItemStack tool) {
        UUID npcId = StackData.getUuid(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_KEY);
        if (npcId == null) return;
        if (sp.serverLevel().getEntity(npcId) instanceof NotchNpcEntity npc) {
            openSchedule(sp, npc);
        }
    }

    public static void setFlavor(ServerPlayer sp, NotchNpcEntity npc,
                                 String subtitle, String voice, int voicePitch) {
        if (!guard(sp, npc)) return;
        npc.setSubtitle(subtitle);
        // Only real, registered sounds: an unknown id would be a silent NPC with no clue why.
        String cleaned = voice == null ? "" : voice.trim();
        if (!cleaned.isEmpty()) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(cleaned);
            if (id == null || !net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                sp.displayClientMessage(Component.literal("No sound called '" + cleaned + "'.").withStyle(ChatFormatting.RED), false);
                cleaned = npc.getVoice();
            }
        }
        npc.setVoice(cleaned);
        npc.setVoicePitchPercent(voicePitch);
        npc.playVoice(); // hear the change straight away rather than guessing at a number
    }

    // ---- edit actions (all owner/op-gated) ----

    public static void setRole(ServerPlayer sp, NotchNpcEntity npc, NpcRole role) {
        if (!guard(sp, npc)) return;
        NpcRole previous = npc.getRole();
        npc.setRole(role);
        if (role == NpcRole.SHOP) {
            ensureShopForNpc(sp.serverLevel(), npc, sp);
        } else if (previous == NpcRole.SHOP) {
            // Leaving the SHOP role: close & return the linked shop so nothing is orphaned.
            removeLinkedShop(sp, npc.getUUID());
        }
        seedRoleEntryChoice(sp, npc);
        sp.displayClientMessage(Component.literal("Role set to " + role.name() + ".").withStyle(ChatFormatting.GREEN), false);
    }

    private static net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice roleEntryChoice(NotchNpcEntity npc) {
        var entry = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice(
                net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch.entryLabel(npc.getRole()), "");
        entry.withAction(new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction(
                net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.Type.OPEN_ROLE, "", 0));
        return entry;
    }

    private static void seedRoleEntryChoice(ServerPlayer sp, NotchNpcEntity npc) {
        var role = npc.getRole();
        if (role == NpcRole.NONE || role == NpcRole.GREETER) return;
        var tree = npc.getDialogue();
        if (tree.isEmpty() || tree.isFlat()) return; // flat = Quick Lines; CHAT opens the role itself
        var start = tree.start();
        if (start == null || start.choices().size() >= 6) return;
        for (var node : tree.nodes().values()) {
            for (var choice : node.choices()) {
                for (var action : choice.actions()) {
                    var t = action.type();
                    if (t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.Type.OPEN_ROLE
                            || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.Type.OPEN_SCREEN) {
                        return; // the author already wired a path in
                    }
                }
            }
        }
        start.withChoice(roleEntryChoice(npc));
        sp.displayClientMessage(Component.literal("Added a \""
                + net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch.entryLabel(npc.getRole())
                + "\" choice to its dialogue: edit or remove it in the Studio.")
                .withStyle(ChatFormatting.YELLOW), false);
    }

    public static void setFarewell(ServerPlayer sp, NotchNpcEntity npc, String text) {
        if (!guard(sp, npc)) return;
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() > 150) trimmed = trimmed.substring(0, 150);
        npc.setFarewellText(trimmed);
        sp.displayClientMessage(Component.literal(trimmed.isEmpty() ? "Goodbye line cleared." : "Goodbye line saved.")
                .withStyle(ChatFormatting.GREEN), false);
    }

    public static void setName(ServerPlayer sp, NotchNpcEntity npc, String name) {
        if (!guard(sp, npc)) return;
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() > 48) trimmed = trimmed.substring(0, 48); // matches the editor field cap
        if (trimmed.isEmpty()) {
            npc.setCustomName(null);
            npc.setCustomNameVisible(false);
        } else {
            // The same '&' codes shop titles and dialogue already use, so "&6Carol" is a gold name.
            npc.setCustomName(Component.literal(NpcText.colorize(trimmed)));
            npc.setCustomNameVisible(true);
        }
        sp.displayClientMessage(Component.literal("NPC name updated.").withStyle(ChatFormatting.GREEN), false);
    }

    public static void pickUp(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        // Return & close any linked shop so it isn't orphaned when the entity is removed.
        removeLinkedShop(sp, npc.getUUID());

        ItemStack stack = new ItemStack(ModItems.NOTCH_NPC_ITEM);
        StackData.putCompound(stack, ITEM_TAG, npc.writeToItem());
        npc.discard();
        if (!sp.getInventory().add(stack)) {
            sp.drop(stack, false);
        }
        sp.displayClientMessage(Component.literal("Picked up the NPC - place the item to set it down again.").withStyle(ChatFormatting.GREEN), false);
    }

    public static void delete(ServerPlayer sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        removeLinkedShop(sp, npc.getUUID());
        npc.discard();
        sp.displayClientMessage(Component.literal("NPC deleted.").withStyle(ChatFormatting.GREEN), false);
    }

    // ---- shop linkage (SHOP role) ----

    public static void ensureShopForNpc(ServerLevel world, NotchNpcEntity npc, ServerPlayer fallbackOwner) {
        ShopState state = ShopState.get(world);
        if (state.getShopByNpc(npc.getUUID()) != null) return;
        UUID ownerId = npc.getOwner() != null ? npc.getOwner() : fallbackOwner.getUUID();
        String ownerName = npc.getOwnerName().isEmpty() ? fallbackOwner.getName().getString() : npc.getOwnerName();
        PlayerShop shop = new PlayerShop(ownerId, ownerName, ownerName + "'s Shop");
        shop.setLinkedNpcId(npc.getUUID());
        state.addShop(shop);
        state.markDirtyAndSave();
    }

    static void removeLinkedShop(ServerPlayer sp, UUID npcUuid) {
        ShopState state = ShopState.get(sp.serverLevel());
        PlayerShop shop = state.getShopByNpc(npcUuid);
        if (shop == null) return;
        PlayerShopManager.returnAllShopContents(sp.level().getServer(), shop, sp);
        state.removeShop(shop.getShopId());
        state.markDirtyAndSave();
    }

    static boolean guard(ServerPlayer sp, NotchNpcEntity npc) {
        if (npc.canEdit(sp)) return true;
        sp.displayClientMessage(Component.literal("Only the owner can edit this NPC.").withStyle(ChatFormatting.RED), false);
        return false;
    }
}
