package org.rusherstreak.commands;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

public class viewnbt extends Command {
    public viewnbt() {
        super("viewnbt", "get item nbt");
    }

    @CommandExecutor
    private String view_nbt() {
        assert mc.player != null;
        if (!mc.player.getMainHandItem().isEmpty()) {
            return mc.player.getMainHandItem().getComponents().toString();
        }
        return "No nbt data found";
    }
}
