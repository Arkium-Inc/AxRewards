package com.artillexstudios.axrewards.guis.data;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record Reward(String name, int slot, long cooldown, List<String> claimCommands,
                     List<Map<?, ?>> claimItems,
                     @Nullable String claimPermission) {

    @Override
    public String toString() {
        return "Reward{" +
                "name='" + name + '\'' +
                ", slot=" + slot +
                ", cooldown=" + cooldown +
                ", claimCommands=" + claimCommands +
                ", claimItems=" + claimItems +
                ", claimPermission='" + claimPermission + '\'' +
                '}';
    }
}
