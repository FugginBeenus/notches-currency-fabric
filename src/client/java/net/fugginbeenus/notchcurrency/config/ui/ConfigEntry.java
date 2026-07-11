package net.fugginbeenus.notchcurrency.config.ui;

import java.util.function.Consumer;

/**
 * One setting row in the Jade-style config screen. Entries hold a WORKING value seeded from the
 * config, so edits are local until the screen's Save commits every entry back — Cancel/Esc
 * discards everything, and a per-row reset restores the shipped default.
 */
public abstract class ConfigEntry {

    public final String category;
    public final String label;
    public final String[] tooltip;

    protected ConfigEntry(String category, String label, String... tooltip) {
        this.category = category;
        this.label = label;
        this.tooltip = tooltip;
    }

    /** Write the working value back into the live config object (called on Save). */
    public abstract void commit();

    /** True while the working value equals the shipped default. */
    public abstract boolean isDefault();

    /** Restore the shipped default (the row's reset arrow). */
    public abstract void reset();

    public boolean matches(String query) {
        return label.toLowerCase().contains(query) || category.toLowerCase().contains(query);
    }

    /* ---------------------------------- types ---------------------------------- */

    /** ON/OFF pill toggle. */
    public static final class BoolEntry extends ConfigEntry {
        public boolean value;
        private final boolean def;
        private final Consumer<Boolean> setter;

        public BoolEntry(String category, String label, boolean current, boolean def,
                         Consumer<Boolean> setter, String... tooltip) {
            super(category, label, tooltip);
            this.value = current;
            this.def = def;
            this.setter = setter;
        }

        @Override public void commit() { setter.accept(value); }
        @Override public boolean isDefault() { return value == def; }
        @Override public void reset() { value = def; }
    }

    /** Click-to-edit number (backs both int and long config fields; clamped to [min, max]). */
    public static final class NumberEntry extends ConfigEntry {
        public long value;
        public final long min, max;
        private final long def;
        private final Consumer<Long> setter;

        public NumberEntry(String category, String label, long current, long def, long min, long max,
                           Consumer<Long> setter, String... tooltip) {
            super(category, label, tooltip);
            this.value = current;
            this.def = def;
            this.min = min;
            this.max = max;
            this.setter = setter;
        }

        public void set(long v) { value = Math.max(min, Math.min(max, v)); }
        @Override public void commit() { setter.accept(value); }
        @Override public boolean isDefault() { return value == def; }
        @Override public void reset() { value = def; }
    }

    /** Draggable slider for bounded ints, with a suffix readout ("85%"). */
    public static final class SliderEntry extends ConfigEntry {
        public int value;
        public final int min, max;
        public final String suffix;
        private final int def;
        private final Consumer<Integer> setter;

        public SliderEntry(String category, String label, int current, int def, int min, int max,
                           String suffix, Consumer<Integer> setter, String... tooltip) {
            super(category, label, tooltip);
            this.value = current;
            this.def = def;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.setter = setter;
        }

        public void setFromFraction(double f) {
            value = (int) Math.round(min + Math.max(0, Math.min(1, f)) * (max - min));
        }
        public double fraction() { return (value - min) / (double) (max - min); }
        @Override public void commit() { setter.accept(value); }
        @Override public boolean isDefault() { return value == def; }
        @Override public void reset() { value = def; }
    }

    /** Cycle button over a fixed set of options (e.g. the HUD anchor). */
    public static final class SelectEntry extends ConfigEntry {
        public final String[] options;
        public int index;
        private final int defIndex;
        private final Consumer<String> setter;

        public SelectEntry(String category, String label, String[] options, String current,
                           String def, Consumer<String> setter, String... tooltip) {
            super(category, label, tooltip);
            this.options = options;
            this.index = Math.max(0, indexOf(options, current));
            this.defIndex = Math.max(0, indexOf(options, def));
            this.setter = setter;
        }

        private static int indexOf(String[] arr, String v) {
            for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
            return -1;
        }

        public void cycle(int dir) { index = Math.floorMod(index + dir, options.length); }
        public String value() { return options[index]; }
        @Override public void commit() { setter.accept(options[index]); }
        @Override public boolean isDefault() { return index == defIndex; }
        @Override public void reset() { index = defIndex; }
    }

    /** Click-to-edit free text (coin name, webhook URL). */
    public static final class StringEntry extends ConfigEntry {
        public String value;
        public final int maxLength;
        private final String def;
        private final Consumer<String> setter;

        public StringEntry(String category, String label, String current, String def, int maxLength,
                           Consumer<String> setter, String... tooltip) {
            super(category, label, tooltip);
            this.value = current;
            this.def = def;
            this.maxLength = maxLength;
            this.setter = setter;
        }

        @Override public void commit() { setter.accept(value); }
        @Override public boolean isDefault() { return value.equals(def); }
        @Override public void reset() { value = def; }
    }
}
