package org.rusherstreak.modules;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.rusherhack.client.api.IRusherHack;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.NumberSetting;

public class AutoStaircase extends ToggleableModule {

    private final NumberSetting<Double> view =
            new NumberSetting<>("ViewAngle", 1.0, 0.1, 30.0);

    private final NumberSetting<Integer> limit =
            new NumberSetting<>("Build Limit", 319, -64, 319);

    public AutoStaircase(ModuleCategory category) {
        super("Auto Staircase", category);
        this.registerSettings(view, limit);
    }

    @Override
    public void onEnable() {
        if (mc.player==null || mc.level == null || mc.gameMode == null) return;
        mc.player.setDeltaMovement(0,0,0);
        centerPlayer();
        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
        BlockPos pos = mc.player.blockPosition().offset(0,-1,0);
        if(mc.level.getBlockState(pos).canBeReplaced()) {
            mc.gameMode.useItemOn(mc.player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atLowerCornerOf(pos),
                            Direction.DOWN, pos, false)
            );
            mc.player.swing(InteractionHand.MAIN_HAND);
            // TODO: check for flight and other modules, disable them
        }
    }

    @Override
    public void onDisable() {
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
    }

    @Subscribe(stage = Stage.PRE)
    private void onPreTick(EventPlayerUpdate event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.getMainHandItem().isEmpty()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyJump.setDown(false);
            centerPlayer();
        }
        if (mc.options.keyRight.isDown())
            mc.options.keyRight.setDown(false);
        if (mc.options.keyLeft.isDown())
            mc.options.keyLeft.setDown(false);

        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;

        Vec3 lookAtTarget = getLookAtTarget();
        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, lookAtTarget);

        if (!mc.player.onGround()) return;
        if (mc.options.keyDown.isDown()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.player.setDeltaMovement(0,0,0);
        }
        if(mc.player.getY() >= limit.getValue()){
            mc.options.keyUp.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyJump.setDown(false);
            centerPlayer();
            this.toggle();
        }
    }

    private @NonNull Vec3 getLookAtTarget() {
        assert mc.player != null;
        Vec3 lookAtTarget = switch (mc.player.getMotionDirection()) {
            case NORTH -> new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ() - view.getValue());
            case EAST -> new Vec3(mc.player.getX() + view.getValue(), mc.player.getY(), mc.player.getZ());
            case SOUTH -> new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ() + view.getValue());
            case WEST -> new Vec3(mc.player.getX() - view.getValue(), mc.player.getY(), mc.player.getZ());
            default -> null;
        };
        assert lookAtTarget != null;
        return lookAtTarget;
    }

    @Subscribe
    public void onPlayerMove(EventPlayerUpdate playerUpdate) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!mc.player.onGround() || !(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
        BlockPos pos = mc.player.blockPosition().relative(mc.player.getMotionDirection());
        if (mc.level.getBlockState(pos).canBeReplaced()) {
            mc.options.keyUp.setDown(false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.DOWN, pos, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        if (!mc.level.getBlockState(pos).canBeReplaced()) {
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(true);
            centerPlayer();
        }
        if (mc.player.getMainHandItem().isEmpty()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
        }
    }

    public void centerPlayer() {
        if (mc.player == null) return;
        BlockPos pos = mc.player.blockPosition();
        mc.player.setPos(pos.getX() + 0.5, mc.player.getY(), pos.getZ() + 0.5);
    }
}