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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Server-side operations for the Notch NPC: opening the owner editor, dispatching the NPC's role on
 * interact, and the edit actions (set role/name, pick up, delete). All mutating actions are gated on
 * {@link NotchNpcEntity#canEdit} (owner or op).
 */
public final class NotchNpcManager {

    /** Item NBT tag holding a packed NPC config (owner/role/name). */
    public static final String ITEM_TAG = "NotchNpc";

    private NotchNpcManager() {}

    // ---- interaction ----

    /** Interacting with the NPC (non-editing) runs its role. */
    public static void dispatchRole(ServerPlayerEntity sp, NotchNpcEntity npc) {
        NpcRoleDispatch.open(sp, npc.getRole(), npc.getRoleTarget(), npc);
    }

    /** Open the editor for an owner/op (sends the current state to the client). */
    public static void openEditor(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!npc.canEdit(sp)) {
            sp.sendMessage(Text.literal("Only the owner can edit this NPC.").formatted(Formatting.RED), false);
            return;
        }
        String name = (npc.hasCustomName() && npc.getCustomName() != null) ? npc.getCustomName().getString() : "";
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npc.getUuid());
        buf.writeVarInt(npc.getRole().ordinal());
        buf.writeString(name);
        buf.writeString(npc.getOwnerName());
        buf.writeBoolean(true);
        buf.writeString(npc.getModelId());
        buf.writeString(npc.getSkinType());
        buf.writeString(npc.getSkinValue());
        buf.writeBoolean(npc.isSlim());
        buf.writeFloat(npc.getScale());
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
        buf.writeVarInt((int) Math.round(npc.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH)));
        buf.writeVarInt((int) Math.round(npc.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED) * 100));
        buf.writeVarInt(npc.getRegen());
        buf.writeString(npc.getFollowPlayerName());
        buf.writeVarInt((npc.avoidsMonsters() ? 1 : 0) | (npc.watchesPlayers() ? 2 : 0));
        buf.writeString(npc.getFarewellText());
        Net.sendToClient(sp, NotchPackets.NPC_EDITOR_OPEN, buf);
    }

    public static void setDialogueMode(ServerPlayerEntity sp, NotchNpcEntity npc, int modeOrdinal) {
        if (!guard(sp, npc)) return;
        NotchNpcEntity.DialogueMode[] modes = NotchNpcEntity.DialogueMode.values();
        npc.setDialogueMode((modeOrdinal >= 0 && modeOrdinal < modes.length)
                ? modes[modeOrdinal] : NotchNpcEntity.DialogueMode.WINDOW);
    }

    /** Pack the stat toggles into a bitmask (1=protected 2=silent 4=glowing 8=nameplate 16=no gravity
     *  32=opens doors 64=leashable 128=invisible; bits 8-9 = visibility rule 0/1/2 always/day/night). */
    public static int statsBits(NotchNpcEntity npc) {
        int bits = 0;
        if (npc.isProtectedNpc()) bits |= 1;
        if (npc.isSilent()) bits |= 2;
        if (npc.isGlowing()) bits |= 4;
        if (npc.isCustomNameVisible()) bits |= 8;
        if (npc.hasNoGravity()) bits |= 16;
        if (npc.opensDoors()) bits |= 32;
        if (npc.isLeashable()) bits |= 64;
        if (npc.isManualInvisible()) bits |= 128;
        bits |= (npc.getVisibility() & 3) << 8; // bits 8-9 reserved for the visibility rule
        if (npc.isNpcPushable()) bits |= 1024;
        if (npc.isHostileToPlayers()) bits |= 2048;
        if (npc.fightsBack()) bits |= 4096;
        return bits;
    }

    public static void setStats(ServerPlayerEntity sp, NotchNpcEntity npc, int bits) {
        if (!guard(sp, npc)) return;
        npc.setProtectedNpc((bits & 1) != 0);
        npc.setSilent((bits & 2) != 0);
        npc.setGlowing((bits & 4) != 0);
        npc.setCustomNameVisible((bits & 8) != 0);
        npc.setNoGravity((bits & 16) != 0);
        npc.setOpensDoors((bits & 32) != 0);
        npc.setLeashable((bits & 64) != 0);
        npc.setManualInvisible((bits & 128) != 0);
        npc.setVisibility((bits >> 8) & 3);
        npc.setNpcPushable((bits & 1024) != 0);
        npc.setHostileToPlayers((bits & 2048) != 0);
        npc.setFightsBack((bits & 4096) != 0);
        // Apply the effective invisibility now rather than waiting for the next tick window.
        npc.setInvisible(npc.isManualInvisible() || npc.isRuleHidden());
    }

    /** Slider attributes from the stats screen: max health, walk speed, regen. */
    public static void setAttrs(ServerPlayerEntity sp, NotchNpcEntity npc, int maxHealth, int speedPct, int regen) {
        if (!guard(sp, npc)) return;
        npc.setBaseStats(maxHealth, speedPct);
        npc.setRegen(regen);
    }

    // ---- equipment ----

    /** Open the NPC equipment screen (armor + hands backed live by the entity). */
    public static void openEquipScreen(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(sp, Text.literal("NPC Equipment"),
                (syncId, inv, p) -> new NpcEquipScreenHandler(syncId, inv, new NpcEquipmentInventory(npc), npc),
                buf -> {
                    buf.writeBoolean(true);
                    buf.writeUuid(npc.getUuid());
                });
    }

    public static void setAppearance(ServerPlayerEntity sp, NotchNpcEntity npc,
                                     String model, String skinType, String skinValue, boolean slim, float scale) {
        if (!guard(sp, npc)) return;
        // URL skins are fetched by every client that sees the NPC — only allow real web URLs.
        if (NotchNpcEntity.SKIN_URL.equals(skinType) && !skinValue.isBlank()) {
            String lower = skinValue.trim().toLowerCase();
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                sp.sendMessage(Text.literal("Skin URLs must start with http:// or https://.")
                        .formatted(Formatting.RED), false);
                return;
            }
        }
        npc.setAppearance(model, skinType, skinValue, slim, scale);
    }

    public static void setPose(ServerPlayerEntity sp, NotchNpcEntity npc, int pose) {
        if (!guard(sp, npc)) return;
        npc.setNpcPose(pose);
    }

    /** Pose-editor slider edit: set one part's rotation (or reset all with part = -1) and make sure
     *  the custom pose is what's showing. */
    public static void setPosePart(ServerPlayerEntity sp, NotchNpcEntity npc, int part, int x, int y, int z) {
        if (!guard(sp, npc)) return;
        npc.setCustomPosePart(part, x, y, z);
        if (npc.getNpcPose() != NotchNpcEntity.POSE_CUSTOM) {
            npc.setNpcPose(NotchNpcEntity.POSE_CUSTOM);
        }
    }

    /** Set the idle animation layered on the pose (statue/breathe/sway/lively). */
    public static void setPoseAnim(ServerPlayerEntity sp, NotchNpcEntity npc, int anim) {
        if (!guard(sp, npc)) return;
        npc.setPoseAnim(anim);
    }

    /** Move (delta, clamped) and/or rotate (absolute yaw) the whole NPC — the move screen's live
     *  control. The home leash follows so movement behaviors don't drag it back. */
    public static void transform(ServerPlayerEntity sp, NotchNpcEntity npc, double dx, double dy, double dz,
                                 float yawDeg, boolean applyYaw) {
        if (!guard(sp, npc)) return;
        dx = net.minecraft.util.math.MathHelper.clamp(dx, -16.0, 16.0);
        dy = net.minecraft.util.math.MathHelper.clamp(dy, -16.0, 16.0);
        dz = net.minecraft.util.math.MathHelper.clamp(dz, -16.0, 16.0);
        double x = npc.getX() + dx;
        double y = Math.max(npc.getWorld().getBottomY(), npc.getY() + dy);
        double z = npc.getZ() + dz;
        float yaw = applyYaw ? net.minecraft.util.math.MathHelper.wrapDegrees(yawDeg) : npc.getYaw();
        npc.refreshPositionAndAngles(x, y, z, yaw, npc.getPitch());
        npc.setHeadYaw(yaw);
        npc.bodyYaw = yaw;
        npc.getNavigation().stop();
        npc.setHome(npc.getBlockPos());
    }

    public static void setBehavior(ServerPlayerEntity sp, NotchNpcEntity npc, int modeOrdinal, int radius,
                                   String followName, int movesBits) {
        if (!guard(sp, npc)) return;
        NotchNpcEntity.Behavior[] all = NotchNpcEntity.Behavior.values();
        NotchNpcEntity.Behavior mode = (modeOrdinal >= 0 && modeOrdinal < all.length)
                ? all[modeOrdinal] : NotchNpcEntity.Behavior.STATIONARY;
        npc.setWanderRadius(radius);
        npc.setFollowPlayerName(followName);
        npc.setAvoidMonsters((movesBits & 1) != 0);
        npc.setWatchPlayers((movesBits & 2) != 0);
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
        sp.sendMessage(Text.literal("Behavior set: " + desc + ".").formatted(Formatting.GREEN), false);

        // Follow with an unresolvable target does nothing — say why. (Common in dev, where each
        // launch gets a fresh random username/UUID.)
        if (mode == NotchNpcEntity.Behavior.FOLLOW_OWNER && npc.resolveFollowTarget() == null) {
            String who = npc.getFollowPlayerName().isEmpty()
                    ? "its owner (" + (npc.getOwnerName().isEmpty() ? "unknown" : npc.getOwnerName()) + ")"
                    : npc.getFollowPlayerName();
            sp.sendMessage(Text.literal("Note: " + who + " isn't online, so it has no one to follow.")
                    .formatted(Formatting.YELLOW), false);
        }
        if (mode == NotchNpcEntity.Behavior.PATROL && npc.getWaypoints().isEmpty()) {
            sp.sendMessage(Text.literal("Add waypoints (stand somewhere and click 'Add waypoint here') so it has a route.")
                    .formatted(Formatting.YELLOW), false);
        }
    }

    /** Patrol speed presets (index shared with the editor's Speed cycle). */
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

    /** Waypoint dwell-time presets in ticks (index shared with the editor's Wait cycle). */
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

    /** Patrol edits: 0 = hand out a bound route tool, 1 = clear the route,
     *  2 = finalize (take the route tools back), 3 = set speed ({@code value} = preset index),
     *  4 = set waypoint dwell time ({@code value} = preset index). */
    public static void patrolAction(ServerPlayerEntity sp, NotchNpcEntity npc, int action, int value) {
        if (!guard(sp, npc)) return;
        switch (action) {
            case 0 -> {
                ItemStack tool = new ItemStack(ModItems.ROUTE_PLANNER);
                String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                        ? npc.getCustomName().getString() : "NPC";
                StackData.putUuid(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_KEY, npc.getUuid());
                StackData.putString(tool, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_NAME_KEY, npcName);
                if (!sp.getInventory().insertStack(tool)) {
                    sp.dropItem(tool, false);
                }
                sp.sendMessage(Text.literal("Route tool added — walk the route and right-click the ground to drop waypoints.")
                        .formatted(Formatting.GREEN), false);
            }
            case 1 -> {
                npc.clearWaypoints();
                sp.sendMessage(Text.literal("Waypoints cleared.").formatted(Formatting.GREEN), false);
            }
            case 2 -> confirmRoute(sp, npc);
            case 3 -> {
                int idx = Math.max(0, Math.min(PATROL_SPEEDS.length - 1, value));
                npc.setPatrolSpeed(PATROL_SPEEDS[idx]);
                sp.sendMessage(Text.literal("Patrol speed: " + PATROL_SPEED_NAMES[idx] + ".").formatted(Formatting.GREEN), false);
            }
            case 4 -> {
                int idx = Math.max(0, Math.min(PATROL_WAITS.length - 1, value));
                npc.setPatrolWaitTicks(PATROL_WAITS[idx]);
                sp.sendMessage(Text.literal("Waypoint wait: " + PATROL_WAIT_NAMES[idx] + ".").formatted(Formatting.GREEN), false);
            }
            default -> { }
        }
    }

    /** Route-tool click: add a waypoint at the clicked spot. Actionbar feedback (the HUD overlay
     *  shows the running count) so the chat doesn't fill up while walking a long route. */
    public static void addWaypointAt(ServerPlayerEntity sp, NotchNpcEntity npc, net.minecraft.util.math.BlockPos pos) {
        if (!guard(sp, npc)) return;
        if (npc.addWaypoint(pos)) {
            sp.sendMessage(Text.literal("Waypoint " + npc.getWaypoints().size() + " added.")
                    .formatted(Formatting.GREEN), true);
        } else {
            sp.sendMessage(Text.literal("Route is full (16 waypoints).").formatted(Formatting.RED), true);
        }
    }

    /** Route-tool sneak-click: undo the last waypoint. */
    public static void removeLastWaypoint(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        if (npc.removeLastWaypoint()) {
            sp.sendMessage(Text.literal("Removed last waypoint (" + npc.getWaypoints().size() + " left).")
                    .formatted(Formatting.YELLOW), true);
        } else {
            sp.sendMessage(Text.literal("The route is already empty.").formatted(Formatting.YELLOW), true);
        }
    }

    /** Confirm the route: the tool disappears, and the NPC starts walking it. Needs 2+ waypoints
     *  (with fewer, the tool stays so the player can keep planning). */
    public static void confirmRoute(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        if (npc.getWaypoints().size() < 2) {
            sp.sendMessage(Text.literal("Add at least 2 waypoints first (right-click the ground).")
                    .formatted(Formatting.YELLOW), false);
            return;
        }
        int removed = 0;
        for (int i = 0; i < sp.getInventory().size(); i++) {
            ItemStack st = sp.getInventory().getStack(i);
            if (st.getItem() instanceof net.fugginbeenus.notchcurrency.item.RoutePlannerItem
                    && npc.getUuid().equals(StackData.getUuid(st, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.NPC_KEY))) {
                sp.getInventory().setStack(i, ItemStack.EMPTY);
                removed++;
            }
        }
        npc.setBehavior(NotchNpcEntity.Behavior.PATROL);
        sp.sendMessage(Text.literal("Route confirmed — " + npc.getWaypoints().size()
                + " waypoints, patrol started." + (removed > 0 ? " The route tool vanished." : ""))
                .formatted(Formatting.GREEN), false);
    }

    // ---- dialogue setup ----

    /** Build the friendly starter conversation used to seed new dialogues. */
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

    /** Fill the NPC with the starter conversation the owner can build on. */
    public static void createDialogueTemplate(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        npc.setDialogue(buildStarterTree(npc));
        sp.sendMessage(Text.literal("Starter dialogue created — talk to the NPC to try it.").formatted(Formatting.GREEN), false);
    }

    public static void clearDialogue(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        npc.getDialogue().clear();
        sp.sendMessage(Text.literal("Dialogue cleared.").formatted(Formatting.GREEN), false);
    }

    /** Send the full dialogue tree to the owner's client for editing in the studio. An NPC with no
     *  dialogue gets the starter template pre-loaded (nothing is saved until the studio saves). */
    public static void openStudio(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        var tree = npc.getDialogue().isEmpty() ? buildStarterTree(npc) : npc.getDialogue();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npc.getUuid());
        buf.writeNbt(tree.toNbt());
        Net.sendToClient(sp, NotchPackets.NPC_STUDIO_DATA, buf);
    }

    /** Replace the NPC's dialogue with a studio-edited tree (owner/op re-validated). The client
     *  can't be trusted with sizes: clamp text to 500 chars, choices to 6 per page, drop blank ids. */
    public static void saveDialogue(ServerPlayerEntity sp, NotchNpcEntity npc,
                                    net.minecraft.nbt.NbtCompound treeNbt) {
        if (!guard(sp, npc)) return;
        if (treeNbt == null) return;
        var tree = net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(treeNbt);
        if (tree.size() > 64) {
            sp.sendMessage(Text.literal("That dialogue is too large (max 64 pages).").formatted(Formatting.RED), false);
            return;
        }
        var clean = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree();
        boolean allowCommands = sp.hasPermissionLevel(2);
        boolean strippedCommands = false;
        for (var node : tree.nodes().values()) {
            if (node.id().isBlank() || node.id().length() > 32) continue; // ids are <=24 in the studio
            if (node.text().length() > 500) node.setText(node.text().substring(0, 500));
            while (node.choices().size() > 6) node.choices().remove(node.choices().size() - 1);
            if (!allowCommands) {
                for (var choice : node.choices()) {
                    strippedCommands |= choice.actions().removeIf(a ->
                            a.type() == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.Type.RUN_COMMAND
                            || a.type() == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.Type.RUN_COMMAND_AS_PLAYER);
                }
            }
            clean.put(node);
        }
        if (strippedCommands) {
            sp.sendMessage(Text.literal("Command actions were removed — those are admin-only.")
                    .formatted(Formatting.YELLOW), false);
        }
        clean.setStartId(tree.startId());
        if (clean.get(clean.startId()) == null && clean.size() > 0) {
            clean.setStartId(clean.nodes().keySet().iterator().next()); // start page was dropped
        }
        npc.setDialogue(clean);
        sp.sendMessage(Text.literal("Dialogue saved (" + clean.size() + " page" + (clean.size() == 1 ? "" : "s") + ").")
                .formatted(Formatting.GREEN), false);
    }

    // ---- edit actions (all owner/op-gated) ----

    public static void setRole(ServerPlayerEntity sp, NotchNpcEntity npc, NpcRole role) {
        if (!guard(sp, npc)) return;
        NpcRole previous = npc.getRole();
        npc.setRole(role);
        if (role == NpcRole.SHOP) {
            ensureShopForNpc(sp.getServerWorld(), npc, sp);
        } else if (previous == NpcRole.SHOP) {
            // Leaving the SHOP role: close & return the linked shop so nothing is orphaned.
            removeLinkedShop(sp, npc.getUuid());
        }
        seedRoleEntryChoice(sp, npc);
        sp.sendMessage(Text.literal("Role set to " + role.name() + ".").formatted(Formatting.GREEN), false);
    }

    /** The default "Browse the shop"-style choice: a NORMAL choice with an OPEN_ROLE action, so
     *  the author can rename or delete it in the Studio (it is never forced back). */
    private static net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice roleEntryChoice(NotchNpcEntity npc) {
        var entry = new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice(
                net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch.entryLabel(npc.getRole()), "");
        entry.withAction(new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction(
                net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction.Type.OPEN_ROLE, "", 0));
        return entry;
    }

    /** When an NPC with branching dialogue gains a screen role and no choice reaches it yet, add
     *  the default entry choice to the start page (once — removing it in the Studio sticks). */
    private static void seedRoleEntryChoice(ServerPlayerEntity sp, NotchNpcEntity npc) {
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
        sp.sendMessage(Text.literal("Added a \""
                + net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch.entryLabel(npc.getRole())
                + "\" choice to its dialogue — edit or remove it in the Studio.")
                .formatted(Formatting.YELLOW), false);
    }

    public static void setFarewell(ServerPlayerEntity sp, NotchNpcEntity npc, String text) {
        if (!guard(sp, npc)) return;
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() > 150) trimmed = trimmed.substring(0, 150);
        npc.setFarewellText(trimmed);
        sp.sendMessage(Text.literal(trimmed.isEmpty() ? "Goodbye line cleared." : "Goodbye line saved.")
                .formatted(Formatting.GREEN), false);
    }

    public static void setName(ServerPlayerEntity sp, NotchNpcEntity npc, String name) {
        if (!guard(sp, npc)) return;
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() > 48) trimmed = trimmed.substring(0, 48); // matches the editor field cap
        if (trimmed.isEmpty()) {
            npc.setCustomName(null);
            npc.setCustomNameVisible(false);
        } else {
            npc.setCustomName(Text.literal(trimmed));
            npc.setCustomNameVisible(true);
        }
        sp.sendMessage(Text.literal("NPC name updated.").formatted(Formatting.GREEN), false);
    }

    /** Pack the NPC back into an item (config preserved) and hand it to the player. */
    public static void pickUp(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        // Return & close any linked shop so it isn't orphaned when the entity is removed.
        removeLinkedShop(sp, npc.getUuid());

        ItemStack stack = new ItemStack(ModItems.NOTCH_NPC_ITEM);
        StackData.putCompound(stack, ITEM_TAG, npc.writeToItem());
        npc.discard();
        if (!sp.getInventory().insertStack(stack)) {
            sp.dropItem(stack, false);
        }
        sp.sendMessage(Text.literal("Picked up the NPC — place the item to set it down again.").formatted(Formatting.GREEN), false);
    }

    public static void delete(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!guard(sp, npc)) return;
        removeLinkedShop(sp, npc.getUuid());
        npc.discard();
        sp.sendMessage(Text.literal("NPC deleted.").formatted(Formatting.GREEN), false);
    }

    // ---- shop linkage (SHOP role) ----

    /** Ensure a {@link PlayerShop} exists and is linked to this NPC's UUID (creates one if missing). */
    public static void ensureShopForNpc(ServerWorld world, NotchNpcEntity npc, ServerPlayerEntity fallbackOwner) {
        ShopState state = ShopState.get(world);
        if (state.getShopByNpc(npc.getUuid()) != null) return;
        UUID ownerId = npc.getOwner() != null ? npc.getOwner() : fallbackOwner.getUuid();
        String ownerName = npc.getOwnerName().isEmpty() ? fallbackOwner.getName().getString() : npc.getOwnerName();
        PlayerShop shop = new PlayerShop(ownerId, ownerName, ownerName + "'s Shop");
        shop.setLinkedNpcId(npc.getUuid());
        state.addShop(shop);
        state.markDirtyAndSave();
    }

    static void removeLinkedShop(ServerPlayerEntity sp, UUID npcUuid) {
        ShopState state = ShopState.get(sp.getServerWorld());
        PlayerShop shop = state.getShopByNpc(npcUuid);
        if (shop == null) return;
        PlayerShopManager.returnAllShopContents(sp.getServer(), shop, sp);
        state.removeShop(shop.getShopId());
        state.markDirtyAndSave();
    }

    static boolean guard(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (npc.canEdit(sp)) return true;
        sp.sendMessage(Text.literal("Only the owner can edit this NPC.").formatted(Formatting.RED), false);
        return false;
    }
}
