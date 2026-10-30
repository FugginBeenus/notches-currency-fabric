package net.fugginbeenus.notchcurrency.core;

import net.minecraft.world.inventory.ContainerData;

public final class WideData implements ContainerData {

    private final int[] values;

    public WideData(int count) {
        this.values = new int[count];
    }

    @Override
    public int get(int index) {
        return values[index];
    }

    @Override
    public void set(int index, int value) {
        values[index] = value;
    }

    @Override
    public int getCount() {
        return values.length;
    }

    public ContainerData halves() {
        return new ContainerData() {
            @Override
            public int get(int slot) {
                int v = values[slot >> 1];
                return (slot & 1) == 0 ? v & 0xFFFF : v >>> 16;
            }

            @Override
            public void set(int slot, int half) {
                int i = slot >> 1, h = half & 0xFFFF;
                values[i] = (slot & 1) == 0 ? (values[i] & 0xFFFF0000) | h : (values[i] & 0xFFFF) | (h << 16);
            }

            @Override
            public int getCount() {
                return values.length * 2;
            }
        };
    }
}
