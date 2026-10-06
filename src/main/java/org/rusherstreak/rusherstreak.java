package org.rusherstreak;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.plugin.Plugin;
import org.rusherstreak.commands.center;
import org.rusherstreak.commands.viewnbt;
import org.rusherstreak.modules.AutoStaircase;
import org.rusherstreak.modules.AutoTnt;

public class rusherstreak extends Plugin {
	
	@Override
	public void onLoad() {
		this.getLogger().info("loading rusher-streak plugin");

		ModuleCategory rushercategory = ModuleCategory.getOrRegister("RusherStreak");

		RusherHackAPI.getModuleManager().registerFeature(new AutoStaircase(rushercategory));
		RusherHackAPI.getModuleManager().registerFeature(new AutoTnt(rushercategory));

		RusherHackAPI.getCommandManager().registerFeature(new viewnbt());
		RusherHackAPI.getCommandManager().registerFeature(new center());
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("RusherStreak plugin unloaded!");
	}
	
}