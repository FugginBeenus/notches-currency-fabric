package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Checks that an NPC share code written by one Minecraft version still means the same thing when it
 * is read by another.
 *
 * <p>The code itself is gzipped NBT in Base64 and that container has not changed, so what is
 * actually at risk is the contents: a key one version writes and another does not, a compat helper
 * that defaults differently, or one of the nested shapes drifting. This builds a deliberately
 * awkward NPC, writes its code out, then reads back every other version's code and reports each key
 * that went missing or came back different.
 *
 * <p>Not wired into normal play. Run with {@code -Dnotchcurrency.shareTest=export|import},
 * {@code -Dnotchcurrency.shareDir=<dir>} and {@code -Dnotchcurrency.mcver=<version>}. The version is
 * passed in rather than read from the game so this file needs no per-version branches of its own.
 */
public final class ShareCodeHarness {

    private ShareCodeHarness() {}

    private static final String LOG = "NotchCurrency-ShareTest";
    private static final java.util.concurrent.atomic.AtomicBoolean ran =
            new java.util.concurrent.atomic.AtomicBoolean();

    public static void init() {
        String mode = System.getProperty("notchcurrency.shareTest");
        if (mode == null) return;
        String dir = System.getProperty("notchcurrency.shareDir");
        String version = System.getProperty("notchcurrency.mcver");
        if (dir == null || version == null) {
            log("shareDir and mcver are both required");
            return;
        }

        // On join rather than server-started: equipment is written through the item codec, which
        // needs the registry manager, and that is not published until a world is actually in play.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ran.compareAndSet(false, true)) return;
            try {
                Path base = Path.of(dir);
                Files.createDirectories(base);
                NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, server.overworld());
                if ("export".equals(mode)) {
                    export(base, version, npc);
                } else {
                    doImport(base, version, server);
                }
            } catch (Exception e) {
                log("harness failed: " + e);
                e.printStackTrace();
            }
        });
    }

    // ---- export ----

    private static void export(Path base, String version, NotchNpcEntity npc) throws IOException {
        configure(npc);
        CompoundTag tag = npc.writeToItem();
        String code = NpcShareCodec.encode(tag);
        Files.writeString(base.resolve(version + ".code"), code);
        Files.writeString(base.resolve(version + ".dump"), dump(tag));
        log("exported " + code.length() + " chars, " + flatten(tag).size() + " keys, to " + version + ".code");
    }

    // ---- import ----

    private static void doImport(Path base, String version, net.minecraft.server.MinecraftServer server)
            throws IOException {
        List<Path> codes = new ArrayList<>();
        try (var stream = Files.list(base)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".code")).forEach(codes::add);
        }
        codes.sort(null);
        if (codes.isEmpty()) {
            log("no .code files in " + base + " - run export on every version first");
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("Reading share codes on ").append(version).append("\n\n");
        int bad = 0;

        for (Path codeFile : codes) {
            String from = codeFile.getFileName().toString().replace(".code", "");
            String code = Files.readString(codeFile).strip();
            report.append("=== from ").append(from).append(" ===\n");

            CompoundTag roundTripped;
            String reEncoded;
            try {
                CompoundTag decoded = NpcShareCodec.decode(code);
                NotchNpcEntity fresh = new NotchNpcEntity(ModEntities.NOTCH_NPC, server.overworld());
                fresh.readFromItem(decoded);
                roundTripped = fresh.writeToItem();
                reEncoded = NpcShareCodec.encode(roundTripped);
            } catch (Exception e) {
                report.append("  DECODE FAILED: ").append(e).append("\n\n");
                bad++;
                continue;
            }

            // The strongest check there is: read the code, rebuild the NPC, write a new code, and see
            // whether it comes out the same. Equal codes mean the tag survived exactly.
            if (reEncoded.equals(code)) {
                report.append("  IDENTICAL round trip (re-encoded code matches byte for byte)\n\n");
                continue;
            }
            report.append("  re-encoded code differs, comparing key by key\n");

            Path sourceDump = base.resolve(from + ".dump");
            if (!Files.exists(sourceDump)) {
                report.append("  no ").append(from).append(".dump to compare against\n\n");
                continue;
            }

            Map<String, String> want = parseDump(Files.readString(sourceDump));
            Map<String, String> got = flatten(roundTripped);

            List<String> lost = new ArrayList<>();
            List<String> changed = new ArrayList<>();
            for (Map.Entry<String, String> e : want.entrySet()) {
                String actual = got.get(e.getKey());
                if (actual == null) {
                    lost.add("    LOST    " + e.getKey() + "  (was " + e.getValue() + ")");
                } else if (!actual.equals(e.getValue())) {
                    changed.add("    CHANGED " + e.getKey() + "  " + e.getValue() + " -> " + actual);
                }
            }
            List<String> added = new ArrayList<>();
            for (String key : got.keySet()) {
                if (!want.containsKey(key)) added.add("    added   " + key + " = " + got.get(key));
            }

            if (lost.isEmpty() && changed.isEmpty()) {
                report.append("  OK, ").append(want.size()).append(" keys match");
                if (!added.isEmpty()) {
                    report.append(" (").append(added.size()).append(" defaulted in by this version)");
                }
                report.append("\n");
            } else {
                bad++;
                report.append("  ").append(lost.size() + changed.size()).append(" PROBLEM(S)\n");
                lost.forEach(l -> report.append(l).append("\n"));
                changed.forEach(l -> report.append(l).append("\n"));
            }
            added.forEach(l -> report.append(l).append("\n"));
            report.append("\n");
        }

        Path out = base.resolve("report-" + version + ".txt");
        Files.writeString(out, report.toString());
        log((bad == 0 ? "ALL CLEAN" : bad + " CODE(S) WITH PROBLEMS") + " - wrote " + out.getFileName());
    }

    // ---- the awkward NPC ----

    /** Every flat field set away from its default, plus one of each nested shape. */
    private static void configure(NotchNpcEntity npc) {
        npc.setCustomName(Component.literal("Shäre Tést §6NPC"));
        npc.setCustomNameVisible(true);
        npc.setModelId("entity:minecraft:zombie");
        npc.setSkinType(NotchNpcEntity.SKIN_VARIANT);
        npc.setSkinValue("banker");
        npc.setSlim(true);
        npc.setScale(1.4f);
        npc.setScaleY(0.8f);
        npc.setScaleZ(1.2f);
        npc.setNameOffset(0.35f);
        npc.setBillboard("&aLine one\n%player% second\n&6Third %balance%");
        npc.setSubtitle("&bBlacksmith");
        npc.setVoice("minecraft:entity.villager.ambient");
        npc.setVoicePitchPercent(120);
        npc.setNpcPose(NotchNpcEntity.POSE_SITTING);
        npc.setPoseAnim(NotchNpcEntity.ANIM_STATUE);
        npc.setCustomPosePart(0, 15, -20, 5);
        npc.setCustomPosePart(1, -30, 10, 0);
        npc.setBehavior(NotchNpcEntity.Behavior.PATROL);
        npc.setWanderRadius(12);
        npc.setPatrolSpeed(0.9f);
        npc.setPatrolWaitTicks(40);
        npc.setHome(new net.minecraft.core.BlockPos(11, 64, -22));
        npc.setFarewellText("&cSafe travels");
        npc.setProtectedNpc(true);
        npc.setOpensDoors(true);
        npc.setLeashable(true);
        npc.setNpcPushable(false);
        npc.setHostileToPlayers(true);
        npc.setFightsBack(true);
        npc.setFollowPlayerName("Player789");
        npc.setActionSweepVersion(3);
        npc.setFactionId("merchants");
        npc.setProtectOwner(true);
        npc.setFightRivalFactions(true);
        npc.setAttackMonsters(true);

        // Dialogue: two nodes, one with a choice carrying an action and a condition.
        CompoundTag choice = new CompoundTag();
        choice.putString("Label", "&eWho are you?");
        choice.putString("Next", "about");
        choice.putBoolean("Hide", true);
        choice.put("Actions", new ListTag());
        choice.put("Conditions", new ListTag());
        ListTag choices = new ListTag();
        choices.add(choice);

        CompoundTag root = new CompoundTag();
        root.putString("Id", "root");
        root.putString("Text", "Hello, %player%!");
        root.put("Choices", choices);

        CompoundTag about = new CompoundTag();
        about.putString("Id", "about");
        about.putString("Text", "&7I mind the shop.");
        about.put("Choices", new ListTag());

        ListTag nodes = new ListTag();
        nodes.add(root);
        nodes.add(about);
        CompoundTag tree = new CompoundTag();
        tree.putString("Start", "root");
        tree.put("Nodes", nodes);
        npc.setDialogue(net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(tree));
        npc.setDialogueMode(NotchNpcEntity.DialogueMode.CHAT);

        CompoundTag actions = new CompoundTag();
        actions.putInt("ProximityRadius", 7);
        npc.setActions(net.fugginbeenus.notchcurrency.npc.action.NpcActions.fromNbt(actions));

        // Equipment. This is the one part of the tag that is a serialised ItemStack, and stacks moved
        // from tags to components at 1.20.5, so it is the field most likely not to cross versions.
        npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3));

        CompoundTag entry = new CompoundTag();
        entry.putInt("Time", 6000);
        entry.putString("Stance", "STAND");
        entry.putInt("X", 4);
        entry.putInt("Y", 65);
        entry.putInt("Z", 9);
        entry.putInt("Radius", 5);
        entry.putFloat("Facing", 90f);
        entry.putBoolean("RoleOpen", true);
        entry.putString("Label", "Morning");
        ListTag entries = new ListTag();
        entries.add(entry);
        CompoundTag schedule = new CompoundTag();
        schedule.putBoolean("Enabled", true);
        schedule.putBoolean("EnforceHours", true);
        schedule.put("Entries", entries);
        npc.setSchedule(net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.fromNbt(schedule));
    }

    // ---- comparing ----

    /**
     * A tag as sorted {@code path=TYPE:value} lines. Written by hand rather than leaning on
     * toString, whose formatting has changed more than once across these versions.
     */
    private static String dump(CompoundTag tag) {
        StringBuilder sb = new StringBuilder();
        flatten(tag).forEach((k, v) -> sb.append(k).append('\t').append(v).append('\n'));
        return sb.toString();
    }

    private static Map<String, String> parseDump(String text) {
        Map<String, String> map = new TreeMap<>();
        for (String line : text.split("\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) map.put(line.substring(0, tab), line.substring(tab + 1));
        }
        return map;
    }

    private static Map<String, String> flatten(CompoundTag tag) {
        Map<String, String> out = new TreeMap<>();
        walk("", tag, out);
        return out;
    }

    private static void walk(String prefix, Tag tag, Map<String, String> out) {
        if (tag instanceof CompoundTag compound) {
            for (String key : net.fugginbeenus.notchcurrency.compat.Nbt.keys(compound)) {
                Tag child = compound.get(key);
                if (child != null) walk(prefix.isEmpty() ? key : prefix + "." + key, child, out);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                walk(prefix + "[" + i + "]", list.get(i), out);
            }
        } else if (tag instanceof net.minecraft.nbt.StringTag) {
            // Not toString: it quotes and escapes differently from 1.21.11 on, which would read as a
            // difference where there is none, and a newline in a value would break the line format.
            out.put(prefix, "str:" + escape(stringValue(tag)));
        } else {
            out.put(prefix, tag.getId() + ":" + tag);
        }
    }

    private static String stringValue(Tag tag) {
        //? if >=1.21.11 {
        /*return tag.asString().orElse("");
        *///?} else {
        return tag.getAsString();
        //?}
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }

    private static void log(String message) {
        org.slf4j.LoggerFactory.getLogger(LOG).info(message);
    }
}
