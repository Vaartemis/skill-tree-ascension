package dev.skillascension.client.pstui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.skillascension.client.PstClientPoints;
import dev.skillascension.AscensionConfig;
import dev.skillascension.client.PstClientRanks;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.client.screen.SkillTreeScreen;
import daripher.skilltree.data.reloader.SkillTreesReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

public final class AscensionHub {
   private static final Logger LOG = LogManager.getLogger("skillascension/pst_ui");
   /** Used for trees without a color in the treeColors config, in rail order. */
   private static final int[] PALETTE = new int[]{-2065825, -5709728, -2580384, -10497818, -1003424, -8593270, -2065712, -4944641, -5195576};
   private static final double GAP = Math.toRadians(1.6);
   private static final List<PassiveSkillTree> trees = new ArrayList<>();
   private static final List<PointType> buttonTypes = new ArrayList<>();
   private static int hovered = -1;
   private static final float[] pop = new float[32];
   private static final List<int[]> buttons = new ArrayList<>();
   private static boolean loggedActive;
   private static boolean loggedError;

   private AscensionHub() {
   }

   private static void collect() {
      trees.clear();
      buttonTypes.clear();

      for (PassiveSkillTree tree : SkillTreesReloader.getSkillTrees().values()) {
         if (!tree.getSkillIds().isEmpty() && !AscensionUi.isSpecial(tree.getId())) {
            trees.add(tree);
         }
      }

      for (PointType type : PointTypes.all()) {
         if (type.openTree != null && SkillTreesReloader.getSkillTrees().containsKey(type.openTree) && !dev.skillascension.common.LockedTrees.isLocked(type.openTree)) {
            buttonTypes.add(type);
         }
      }

      trees.sort(Comparator.<PassiveSkillTree>comparingInt(t -> AscensionConfig.order(t.getId())).thenComparing(t -> t.getId().toString()));
      if (!loggedActive) {
         loggedActive = true;
         LOG.info("Radial tree hub active ({} trees, {} point type buttons)", trees.size(), buttonTypes.size());
      }
   }

   private static int color(PassiveSkillTree tree) {
      for (String entry : AscensionConfig.TREE_COLORS.get()) {
         int eq = entry.indexOf('=');
         if (eq > 0) {
            String key = entry.substring(0, eq).trim();
            if (key.equals(tree.getId().toString()) || key.equals(tree.getId().getPath())) {
               String hex = entry.substring(eq + 1).trim().replace("#", "");
               try {
                  return 0xFF000000 | Integer.parseInt(hex, 16);
               } catch (NumberFormatException ignored) {
               }
            }
         }
      }

      int index = trees.indexOf(tree);
      return PALETTE[Math.floorMod(index < 0 ? tree.getId().hashCode() : index, PALETTE.length)];
   }

   private static float cx(Screen var0) {
      return (float)var0.width / 2.0F;
   }

   private static float cy(Screen var0) {
      return (float)var0.height / 2.0F - 6.0F;
   }

   private static float outer(Screen var0) {
      return Math.max(60.0F, Math.min((float)var0.width * 0.32F, (float)(var0.height - 84) / 2.0F));
   }

   private static float inner(Screen var0) {
      return outer(var0) * 0.42F;
   }

   private static double sliceAngle() {
      return (Math.PI * 2) / (double)Math.max(1, trees.size());
   }

   private static double start(int var0) {
      return (-Math.PI / 2) - sliceAngle() / 2.0 + (double)var0 * sliceAngle();
   }

   private static int sliceAt(Screen var0, double var1, double var3) {
      double var5 = var1 - (double)cx(var0);
      double var7 = var3 - (double)cy(var0);
      double var9 = Math.sqrt(var5 * var5 + var7 * var7);
      if (!(var9 < (double)inner(var0)) && !(var9 > (double)(outer(var0) + 6.0F)) && !trees.isEmpty()) {
         double var11 = Math.atan2(var7, var5) - start(0);
         var11 = (var11 % (Math.PI * 2) + (Math.PI * 2)) % (Math.PI * 2);
         return Math.min(trees.size() - 1, (int)(var11 / sliceAngle()));
      } else {
         return -1;
      }
   }

   private static void sector(GuiGraphics var0, float var1, float var2, float var3, float var4, double var5, double var7, int var9) {
      Matrix4f var10 = var0.pose().last().pose();
      int var11 = var9 >>> 24;
      int var12 = var9 >> 16 & 0xFF;
      int var13 = var9 >> 8 & 0xFF;
      int var14 = var9 & 0xFF;
      int var15 = Math.max(2, (int)Math.ceil(Math.abs(var7 - var5) / Math.toRadians(3.0)));
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder var16 = Tesselator.getInstance().getBuilder();
      var16.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int var17 = 0; var17 <= var15; var17++) {
         double var18 = var5 + (var7 - var5) * (double)var17 / (double)var15;
         float var20 = (float)Math.cos(var18);
         float var21 = (float)Math.sin(var18);
         var16.vertex(var10, var1 + var20 * var4, var2 + var21 * var4, 0.0F).color(var12, var13, var14, var11).endVertex();
         var16.vertex(var10, var1 + var20 * var3, var2 + var21 * var3, 0.0F).color(var12, var13, var14, var11).endVertex();
      }

      BufferUploader.drawWithShader(var16.end());
   }

   private static Set<ResourceLocation> learned(Player var0) {
      HashSet var1 = new HashSet();

      try {
         IPlayerSkills var2 = PlayerSkillsProvider.get(var0);

         for (PassiveSkill var4 : var2.getPlayerSkills()) {
            var1.add(var4.getId());
         }
      } catch (Throwable var5) {
      }

      return var1;
   }

   private static int count(PassiveSkillTree var0, Set<ResourceLocation> var1) {
      int var2 = 0;

      for (ResourceLocation var4 : var0.getSkillIds()) {
         if (var1.contains(var4)) {
            var2++;
         }
      }

      return var2;
   }

   public static void render(Screen var0, GuiGraphics var1, int var2, int var3, float var4) {
      try {
         collect();
         LocalPlayer var5 = Minecraft.getInstance().player;
         if (var5 == null) {
            return;
         }

         Font var6 = UiDraw.font();
         Set var7 = learned(var5);
         var1.fill(0, 0, var0.width, var0.height, -1879048192);
         float var8 = cx(var0);
         float var9 = cy(var0);
         float var10 = outer(var0);
         float var11 = inner(var0);
         hovered = sliceAt(var0, (double)var2, (double)var3);
         MutableComponent var12 = Component.translatable("skillascension.pst_ui.hub.title");
         UiDraw.text(var1, var12, (int)var8 - var6.width(var12), 10, -11654, 2);

         for (int var13 = 0; var13 < trees.size(); var13++) {
            PassiveSkillTree var14 = trees.get(var13);
            boolean var15 = var13 == hovered;
            pop[var13 % pop.length] = pop[var13 % pop.length] + ((var15 ? 1.0F : 0.0F) - pop[var13 % pop.length]) * Math.min(1.0F, var4 * 0.5F + 0.15F);
            float var16 = pop[var13 % pop.length] * 5.0F;
            double var17 = start(var13) + GAP;
            double var19 = start(var13 + 1) - GAP;
            int var21 = color(var14);
            int var22 = count(var14, var7);
            sector(var1, var8, var9, var11 + 2.0F, var10 + var16, var17, var19, var15 ? -265937866 : -401206752);
            int var23 = var15 ? UiDraw.brighten(var21, 1.2F) : (var22 > 0 ? var21 : var21 & 16777215 | -1879048192);
            sector(var1, var8, var9, var10 + var16 - 2.0F, var10 + var16, var17, var19, var23);
            sector(var1, var8, var9, var11 + 2.0F, var11 + 3.0F, var17, var19, var21 & 16777215 | 1610612736);
            double var24 = (var17 + var19) / 2.0;
            float var26 = (var11 + var10) / 2.0F + var16 / 2.0F;
            int var27 = (int)((double)var8 + Math.cos(var24) * (double)var26);
            int var28 = (int)((double)var9 + Math.sin(var24) * (double)var26);
            int var29 = var10 - var11 >= 56.0F ? 2 : 1;
            int var30 = 19 * var29;
            var1.pose().pushPose();
            var1.pose().translate((float)(var27 - var30 / 2), (float)(var28 - var30 / 2 - 5), 0.0F);
            var1.pose().scale((float)var29, (float)var29, 1.0F);
            ResourceLocation var31 = var14.getId().withPrefix("textures/icons/skill_tree/").withSuffix(".png");
            var1.blit(var31, 0, 0, 0.0F, var15 ? 19.0F : 0.0F, 19, 19, 19, 57);
            var1.pose().popPose();
            MutableComponent var32 = Component.translatable(var14.getId().toString());
            var1.drawString(var6, var32, var27 - var6.width(var32) / 2, var28 + var30 / 2 - 3, var15 ? -11654 : -1514768, true);
            if (var22 > 0) {
               String var33 = Integer.toString(var22);
               var1.drawString(var6, var33, var27 - var6.width(var33) / 2, var28 + var30 / 2 + 7, var21, true);
            }
         }

         sector(var1, var8, var9, 0.0F, var11 - 4.0F, 0.0, Math.PI * 2, -401206752);
         sector(var1, var8, var9, var11 - 6.0F, var11 - 4.0F, 0.0, Math.PI * 2, -2578358);
         if (hovered >= 0) {
            PassiveSkillTree var35 = trees.get(hovered);
            MutableComponent var38 = Component.translatable(var35.getId().toString());
            int var41 = (float)(var6.width(var38) * 2) <= (var11 - 8.0F) * 2.0F ? 2 : 1;
            UiDraw.text(var1, var38, (int)var8 - var6.width(var38) * var41 / 2, (int)var9 - 10 - (var41 - 1) * 4, color(var35), var41);
            MutableComponent var44 = Component.translatable("skillascension.pst_ui.hub.nodes", new Object[]{count(var35, var7), var35.getSkillIds().size()});
            var1.drawString(var6, var44, (int)var8 - var6.width(var44) / 2, (int)var9 + 8, -6646866, true);
         } else {
            IPlayerSkills var36 = PlayerSkillsProvider.get(var5);
            int var39 = var36.getPlayerSkills().size() + var36.getSkillPoints() + PstClientRanks.levelDelta();
            MutableComponent var42 = Component.translatable("skillascension.pst_ui.level", new Object[]{var39});
            UiDraw.text(var1, var42, (int)var8 - var6.width(var42), (int)var9 - 12, -11654, 2);
            MutableComponent var45 = Component.translatable("skillascension.pst_ui.hub.points", new Object[]{var36.getSkillPoints()});
            var1.drawString(var6, var45, (int)var8 - var6.width(var45) / 2, (int)var9 + 8, var36.getSkillPoints() > 0 ? -8593270 : -6646866, true);
         }

         int var37 = (int)(var9 + var10 + 12.0F);
         buttons.clear();
         List<MutableComponent> labels = new ArrayList<>();
         int total = 0;

         for (PointType type : buttonTypes) {
            int amount = type.nativePoints ? PlayerSkillsProvider.get(var5).getSkillPoints() : PstClientPoints.synced(type.id);
            MutableComponent label = type.name.copy();
            if (amount > 0) {
               label.append("  " + amount);
            }

            labels.add(label);
            total += var6.width(label) + 34 + 8;
         }

         int bx = (int)var8 - (total - 8) / 2;

         for (int i = 0; i < buttonTypes.size(); i++) {
            PointType type = buttonTypes.get(i);
            int w = var6.width(labels.get(i)) + 34;
            ResourceLocation icon = type.iconTree != null ? type.iconTree : type.openTree;
            buttons.add(button(var1, var2, var3, bx, var37, w, labels.get(i), icon, type.color));
            bx += w + 8;
         }

         if (hovered >= 0) {
            MutableComponent var20 = Component.translatable("skillascension.pst_ui.hub.open");
            var1.drawString(var6, var20, (int)var8 - var6.width(var20) / 2, var0.height - 14, -6646866, true);
         }
      } catch (Throwable var34) {
         if (!loggedError) {
            loggedError = true;
            LOG.error("radial hub render failed", var34);
         }
      }
   }

   private static int[] button(GuiGraphics var0, int var1, int var2, int var3, int var4, int var5, Component var6, ResourceLocation var7, int var8) {
      boolean var9 = UiDraw.inside((double)var1, (double)var2, var3, var4, var5, 20);
      UiDraw.chip(var0, var3, var4, var5, 20, var9 ? -265937866 : -401206752, var9 ? UiDraw.brighten(var8, 1.25F) : var8);
      var0.pose().pushPose();
      var0.pose().translate((float)(var3 + 6), (float)(var4 + 3), 0.0F);
      var0.pose().scale(0.7368421F, 0.7368421F, 1.0F);
      var0.blit(var7.withPrefix("textures/icons/skill_tree/").withSuffix(".png"), 0, 0, 0.0F, var9 ? 19.0F : 0.0F, 19, 19, 19, 57);
      var0.pose().popPose();
      var0.drawString(UiDraw.font(), var6, var3 + 24, var4 + 6, var9 ? -11654 : -1514768, true);
      return new int[]{var3, var4, var5, 20};
   }

   public static boolean click(Screen var0, double var1, double var3, int var5) {
      if (var5 != 0) {
         return false;
      } else {
         try {
            PassiveSkillTree var6 = null;
            int var7 = sliceAt(var0, var1, var3);
            if (var7 >= 0) {
               var6 = trees.get(var7);
            } else {
               for (int i = 0; i < buttons.size() && i < buttonTypes.size(); i++) {
                  int[] b = buttons.get(i);
                  if (UiDraw.inside(var1, var3, b[0], b[1], b[2], b[3])) {
                     var6 = SkillTreesReloader.getSkillTreeById(buttonTypes.get(i).openTree);
                  }
               }
            }

            if (var6 == null) {
               return false;
            } else {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               Minecraft.getInstance().setScreen(new SkillTreeScreen(var6.getId()));
               return true;
            }
         } catch (Throwable var8) {
            LOG.error("radial hub click failed", var8);
            return false;
         }
      }
   }
}
