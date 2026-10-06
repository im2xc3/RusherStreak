package org.rusherstreak.commands;

import net.minecraft.core.BlockPos;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;


public class center extends Command {
    public center() {
        super("center", "puts you into the block center");
    }

    @CommandExecutor
    private void Center() {
        if (mc.player==null) return;
        mc.player.setDeltaMovement(0,0,0);

        BlockPos blockpos = mc.player.blockPosition();
        mc.player.setPos(blockpos.getX()+0.5, mc.player.getY(), blockpos.getZ()+0.5);
    }
}
