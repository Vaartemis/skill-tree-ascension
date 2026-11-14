package dev.skillascension.client;

import daripher.skilltree.client.widget.SkillTreeWidgets;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The skill tree screen that is open right now, and its queued (not yet confirmed) skills. */
public final class PstRankQueue {
    public static SkillTreeWidgets widgets;
    public static List<ResourceLocation> learned;
    public static ResourceLocation selected;

    private PstRankQueue() {
    }

    public static int queued(ResourceLocation id) {
        SkillTreeWidgets w = widgets;
        if (w == null) return 0;
        int n = 0;
        for (ResourceLocation queued : w.newlyLearnedSkills) if (queued.equals(id)) n++;
        return n;
    }

    /** Learned before this screen was opened (so queueing it again buys a level). */
    public static boolean learnedOnScreen(ResourceLocation id) {
        List<ResourceLocation> l = learned;
        return l != null && l.contains(id);
    }
}
