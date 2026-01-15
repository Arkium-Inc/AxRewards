package com.artillexstudios.axrewards.guis.data;

import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.libs.boostedyaml.block.implementation.Section;
import com.artillexstudios.axrewards.AxRewards;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RewardsManager {
    private static final Set<Reward> rewards = ConcurrentHashMap.newKeySet();

    public static void reload() {
        final File file = new File(AxRewards.getInstance().getDataFolder(), "rewards.yml");
        if (!file.exists()) return;

        final Config config = new Config(file);

        for (String route : config.getBackingDocument().getRoutesAsStrings(false)) {
            Section s = config.getSection(route);
            if (s == null) continue;

            long cd = s.getLong("cooldown");
            Reward reward = new Reward(
                    route,
                    s.getInt("slot"),
                    cd == -1 ? -1 : s.getLong("cooldown") * 1_000L,
                    s.getStringList("claim-commands"),
                    s.getMapList("claim-items"),
                    s.getString("permission", null)
            );
            rewards.add(reward);
        }
    }

    public static int getClaimable(Player player) {
        int am = 0;
        for (Reward reward : rewards) {
            long lastClaim = AxRewards.getDatabase().getLastClaim(player, reward);
            boolean canClaim = canClaimReward(reward, lastClaim);

            String permission = reward.claimPermission();
            boolean hasPermission = permission == null || player.hasPermission(permission);

            if (canClaim && hasPermission) am++;
        }
        return am;
    }

    public static boolean canClaimReward(Reward reward, long lastClaim) {
        boolean onCooldown = lastClaim + reward.cooldown() > System.currentTimeMillis();
        boolean claimFailed = lastClaim == -1;
        boolean oneTimeClaimed = lastClaim > 0 && reward.cooldown() == -1;
        return !onCooldown && !claimFailed && !oneTimeClaimed;
    }

    public static Set<Reward> getRewards() {
        return rewards;
    }
}
