package net.fugginbeenus.notchcurrency.client.npc;

public record NpcSounds(String voice, int pitch, String hurt, String death, String step,
                        String angry, String ambient, int every, boolean random) {

    public static NpcSounds empty() {
        return new NpcSounds("", 100, "", "", "", "", "", 0, true);
    }

    public NpcSounds withVoice(String v) { return new NpcSounds(v, pitch, hurt, death, step, angry, ambient, every, random); }
    public NpcSounds withPitch(int v) { return new NpcSounds(voice, v, hurt, death, step, angry, ambient, every, random); }
    public NpcSounds withHurt(String v) { return new NpcSounds(voice, pitch, v, death, step, angry, ambient, every, random); }
    public NpcSounds withDeath(String v) { return new NpcSounds(voice, pitch, hurt, v, step, angry, ambient, every, random); }
    public NpcSounds withStep(String v) { return new NpcSounds(voice, pitch, hurt, death, v, angry, ambient, every, random); }
    public NpcSounds withAngry(String v) { return new NpcSounds(voice, pitch, hurt, death, step, v, ambient, every, random); }
    public NpcSounds withAmbient(String v) { return new NpcSounds(voice, pitch, hurt, death, step, angry, v, every, random); }
    public NpcSounds withEvery(int v) { return new NpcSounds(voice, pitch, hurt, death, step, angry, ambient, v, random); }
    public NpcSounds withRandom(boolean v) { return new NpcSounds(voice, pitch, hurt, death, step, angry, ambient, every, v); }
}
