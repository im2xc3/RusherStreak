package org.rusherstreak.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.internal.EventRusherHack;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.client.api.events.player.EventPlayerUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.event.stage.Stage;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NumberSetting;

import java.util.*;

public class AutoTnt extends ToggleableModule {

    private final BooleanSetting useTickDelay =
            new BooleanSetting("Use tick delay", false);

    private final NumberSetting<Integer> tickDelay =
            new NumberSetting<>("tickDelay", 1, 0, 30);

    private final NumberSetting<Double> reach =
            new NumberSetting<>("Reach", 4.0, 1.0, 6.0);

    public AutoTnt(ModuleCategory category) {
        super("AutoTnt", category);
        this.registerSettings(useTickDelay, tickDelay, reach);
    }

    private final Set<BlockPos> candidatePositions = new HashSet<>();
    private final Queue<IgnitionTask> ignitionQueue = new LinkedList<>();
    private BlockPos ignitePos;

    private boolean igniting = false;
    private int originalSlot = -1;
    private int flintSlot = -1;
    private record IgnitionTask(BlockPos pos, int ticksRemaining) {}

    @Override
    public void onEnable() {
        ignitePos = null;
        candidatePositions.clear();
        ignitionQueue.clear();
        igniting = false;
        originalSlot = -1;
        flintSlot = -1;
    }

    @Subscribe
    private void onPacket(EventPacket.Send event) {
        if (mc.player == null || mc.level == null || igniting) return;

        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet) {
            BlockHitResult hit = packet.getHitResult();
            BlockPos clicked = hit.getBlockPos();
            BlockPos offset = clicked.relative(hit.getDirection());

            boolean clickedIsTnt = mc.level.getBlockState(clicked).is(Blocks.TNT);
            boolean offsetIsTnt = mc.level.getBlockState(offset).is(Blocks.TNT);

            if (clickedIsTnt) candidatePositions.add(clicked);
            if (offsetIsTnt) candidatePositions.add(offset);
        }

        if (!candidatePositions.isEmpty() && ignitePos == null && !igniting) {
            Iterator<BlockPos> it = candidatePositions.iterator();
            while(it.hasNext()) {
                BlockPos pos = it.next();
                if (mc.level.getBlockState(pos).getBlock() == Blocks.TNT) {
                    if (ignitionQueue.stream().anyMatch(task -> task.pos().equals(pos))) {
                        it.remove();
                        continue;
                    }

                    it.remove();
                    if (useTickDelay.getValue()) {
                        ignitionQueue.add(new IgnitionTask(pos, tickDelay.getValue()));
                    } else {
                        ignitePos = pos;
                        candidatePositions.clear();
                        break;
                    }
                } else {
                    it.remove();
                }
            }
        }

        if (useTickDelay.getValue()) return;

        if (ignitePos != null && !igniting && flintSlot == -1) {
            if (originalSlot == -1) {
                originalSlot = mc.player.getInventory().getSelectedSlot();
            }

            flintSlot = InventoryUtils.findItemHotbar(Items.FLINT_AND_STEEL);
            if (flintSlot == -1 || flintSlot == 40) {
                ChatUtils.print("No flint and steel found in hotbar!");
                ignitePos = null;
                originalSlot = -1;
                flintSlot = -1;
                return;
            }

            mc.player.getInventory().setSelectedSlot(flintSlot);
            return;
        }

        if(ignitePos != null && !igniting && mc.player.getInventory().getSelectedSlot() == flintSlot) {
            igniting = true;
            Vec3 hitVec = Vec3.atCenterOf(ignitePos);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, ignitePos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            mc.player.swing(InteractionHand.MAIN_HAND);

            mc.player.getInventory().setSelectedSlot(originalSlot);
            ignitePos = null;
            flintSlot = -1;
            originalSlot = -1;
            igniting = false;
        }
    }

    @Subscribe(stage = Stage.POST)
    private void onTick(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;
        if (!useTickDelay.getValue()) return;
        if (igniting) return;

        if (ignitionQueue.isEmpty()) return;

        List<IgnitionTask> readyList = new ArrayList<>();
        Queue<IgnitionTask> updated = new LinkedList<>();

        while (!ignitionQueue.isEmpty()) {
            IgnitionTask task = ignitionQueue.poll();
            int newTicks = task.ticksRemaining() - 1;
            if (newTicks <= 0) {
                readyList.add(task);
            }else {
                updated.add(new IgnitionTask(task.pos(), newTicks));
            }
        }

        ignitionQueue.addAll(updated);

        int flintSlot = InventoryUtils.findItemHotbar(Items.FLINT_AND_STEEL);
        if (flintSlot == -1 || flintSlot == 40) {
            ChatUtils.print("No flint and steel found in hotbar!");
            readyList.clear();
            ignitionQueue.clear();
            return;
        }
        for (IgnitionTask task : readyList) igniteNow(task.pos(), flintSlot);
    }

    private void igniteNow(BlockPos pos, int flint) {
        if(mc.player.position().distanceTo(Vec3.atCenterOf(pos)) > reach.getValue()) return;
        igniting = true;
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(flint);

        Vec3 hitVec = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);

        mc.player.getInventory().setSelectedSlot(prevSlot);
        igniting = false;
    }
}
