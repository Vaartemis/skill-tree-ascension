package dev.skillascension.client;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.client.screen.SkillTreeScreen;
import daripher.skilltree.config.ServerConfig;
import daripher.skilltree.exp.ExpHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.Render.Post;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class PstPointBadge {
   private static final String TAB_BUTTON = "sfiomn.legendarytabs.client.screens.TabButton";
   private static final long SOUND_COOLDOWN = 60000L;
   private static int free;
   private static boolean buy;
   private static boolean lastOn;
   private static boolean known;
   private static long lastSound;
   private static Field tabBaseField;
   private static Method getIdMethod;
   private static boolean reflectFailed;

   private PstPointBadge() {
   }

   @SubscribeEvent
   public static void onTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.level == null) {
            free = 0;
            buy = false;
            lastOn = false;
            known = false;
         } else if (mc.player.tickCount % 10 == 0) {
            try {
               if (!PlayerSkillsProvider.hasSkills(mc.player)) {
                  return;
               }

               IPlayerSkills skills = PlayerSkillsProvider.get(mc.player);
               free = skills.getSkillPoints();
               for (dev.skillascension.data.PointType type : dev.skillascension.data.PointTypes.extra()) {
                  free += PstClientPoints.synced(type.id);
               }
               int level = skills.getPlayerSkills().size() + skills.getSkillPoints();
               buy = ServerConfig.enable_exp_exchange
                  && level + PstClientRanks.levelDelta() < ServerConfig.max_skill_points
                  && ExpHelper.getPlayerExp(mc.player) >= (long)ServerConfig.getSkillPointCost(level);
            } catch (Throwable var5) {
               return;
            }

            boolean on = free > 0 || buy;
            if (!known) {
               known = mc.player.tickCount > 100;
               lastOn = on;
            } else {
               long now = System.currentTimeMillis();
               if (on && !lastOn && !(mc.screen instanceof SkillTreeScreen) && now - lastSound > 60000L) {
                  lastSound = now;
                  mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.9F));
               }

               lastOn = on;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRender(Post event) {
      if ((free > 0 || buy) && !reflectFailed) {
         for (GuiEventListener child : event.getScreen().children()) {
            if (child instanceof AbstractWidget w
               && "sfiomn.legendarytabs.client.screens.TabButton".equals(child.getClass().getName())
               && "passive_skill_tree".equals(tabId(child))) {
               draw(event.getGuiGraphics(), w.getX() + w.getWidth() - 7, w.getY() - 2);
               return;
            }
         }
      }
   }

   private static String tabId(Object button) {
      try {
         if (tabBaseField == null) {
            tabBaseField = button.getClass().getField("tabBase");
            getIdMethod = tabBaseField.getType().getMethod("getId");
         }

         Object base = tabBaseField.get(button);
         return base == null ? null : (String)getIdMethod.invoke(base);
      } catch (Throwable var2) {
         reflectFailed = true;
         return null;
      }
   }

   private static void draw(GuiGraphics g, int x, int y) {
      Minecraft mc = Minecraft.getInstance();
      String text = free > 0 ? (free > 9 ? "9+" : Integer.toString(free)) : "+";
      int tw = mc.font.width(text);
      int w = Math.max(9, tw + 4);
      int fill = free > 0 ? -1531113 : -12601008;
      float pulse = 0.5F + 0.5F * (float)Math.sin((double)System.currentTimeMillis() / 250.0);
      int edge = 0xFF000000 | (int)(64.0F + 191.0F * pulse) << 16 | (int)(64.0F + 191.0F * pulse) << 8 | (int)(64.0F + 191.0F * pulse);
      g.pose().pushPose();
      g.pose().translate(0.0F, 0.0F, 150.0F);
      g.fill(x - 1, y, x + w + 1, y + 9, edge);
      g.fill(x, y - 1, x + w, y + 10, edge);
      g.fill(x, y, x + w, y + 9, fill);
      g.drawString(mc.font, text, x + (w - tw) / 2 + 1, y + 1, -15066598, false);
      g.pose().popPose();
   }
}
