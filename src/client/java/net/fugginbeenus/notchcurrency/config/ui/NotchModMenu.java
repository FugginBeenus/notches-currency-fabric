package net.fugginbeenus.notchcurrency.config.ui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu entrypoint — wires the "Config" button in ModMenu to the code-drawn settings screen.
 * Declared as the "modmenu" entrypoint in fabric.mod.json; only loaded when ModMenu is present.
 */
public final class NotchModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NotchConfigScreen::create;
    }
}
