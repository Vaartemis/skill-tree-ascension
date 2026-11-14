package dev.skillascension.client.pstui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.skillascension.client.PstClientPoints;
import dev.skillascension.AscensionConfig;
import dev.skillascension.client.PstClientRanks;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.client.screen.SkillTreeScreen;
import daripher.skilltree.client.widget.Label;
import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.client.widget.TextField;
import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.client.widget.skill.SkillButtons;
import daripher.skilltree.config.ServerConfig;
import daripher.skilltree.data.reloader.SkillTreesReloader;
import daripher.skilltree.exp.ExpHelper;
import daripher.skilltree.skill.PassiveSkillTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AscensionUi {
   private static final Logger LOG = LogManager.getLogger("skillascension/pst_ui");
   private static final int PAD = 8;
   private static final int TOP = 8;
   private static final int RAIL_ITEM = 22;
   private static boolean loggedActive;
   private static boolean loggedError;
   private static SkillButton pinned;
   private static ResourceLocation pinnedTree;
   private static int cardScroll;
   private static int cardMaxScroll;
   private static int railStart;
   private static Runnable buyAction;
   private static int headerW;
   private static int[] rail = new int[4];
   private static int[] card = new int[4];
   private static int[] bar = new int[4];
   private static int[] ready = new int[4];
   private static final List<int[]> chipZones = new ArrayList<>();
   private static final List<Runnable> chipActions = new ArrayList<>();
   private static final List<ResourceLocation> railTrees = new ArrayList<>();
   private static int railVisible;

   private AscensionUi() {
   }

   public static boolean hubEnabled() {
      try {
         return enabled() && AscensionConfig.HUB_ENABLED.get();
      } catch (IllegalStateException e) {
         return true;
      }
   }

   public static boolean enabled() {
      try {
         return AscensionConfig.UI_ENABLED.get();
      } catch (IllegalStateException e) {
         return true;
      }
   }

   private static void fail(String var0, Throwable var1) {
      if (!loggedError) {
         loggedError = true;
         LOG.error("PST UI: {} failed, the screen keeps working but part of the Skill layout is missing", var0, var1);
      }
   }

   public static void layout(
      SkillTreeWidgets var0, PassiveSkillTree var1, AbstractWidget var2, AbstractWidget var3, AbstractWidget var4, AbstractWidget var5, Runnable var6
   ) {
      try {
         buyAction = var6;
         if (pinnedTree == null || !pinnedTree.equals(var1.getId())) {
            pinned = null;
            cardScroll = 0;
            pinnedTree = var1.getId();
         }

         int var7 = var0.getWidth();
         int var8 = var0.getHeight();
         String var9 = Component.translatable("widget.confirm_button").getString();
         String var10 = Component.translatable("widget.cancel_button").getString();
         String var11 = Component.translatable("widget.show_stats").getString();
         int var12 = headerWidth(var1);
         int var13 = 8 + var12 + 6;

         for (AbstractWidget var15 : new ArrayList<AbstractWidget>(var0.getWidgets())) {
            if (var15 == var2 || var15 == var3 || var15 == var4) {
               var15.visible = false;
            } else if (var15 == var5) {
               var15.setY(38);
            } else {
               String var16 = var15.getMessage().getString();
               if (var15 instanceof TextField) {
                  var15.setX(var13);
                  var15.setY(13);
                  var15.setWidth(90);
               } else if (var16.equals(var11)) {
                  var15.setX(var13 + 94);
                  var15.setY(13);
               } else if (var16.equals(var9)) {
                  var15.setX(var7 / 2 - var15.getWidth() - 4);
                  var15.setY(var8 - 50);
               } else if (var16.equals(var10)) {
                  var15.setX(var7 / 2 + 4);
                  var15.setY(var8 - 50);
               } else if (var15 instanceof Label && var15.getX() == 4 && var15.getY() == var8 - 18) {
                  var15.visible = false;
               } else if (var15.getY() == var8 - 40) {
                  var15.setY(var8 - 68);
               }
            }
         }

         buildRail();
         if (!loggedActive) {
            loggedActive = true;
            LOG.info("PST UI: Skill layout active ({} class trees on the rail)", railTrees.size());
         }
      } catch (Throwable var17) {
         fail("layout", var17);
      }
   }

   private static void buildRail() {
      railTrees.clear();
      ArrayList<PassiveSkillTree> var0 = new ArrayList<>(SkillTreesReloader.getSkillTrees().values());
      var0.removeIf(var0x -> var0x.getSkillIds().isEmpty() || isSpecial(var0x.getId()));
      var0.sort(Comparator.<PassiveSkillTree>comparingInt(var0x -> AscensionConfig.order(var0x.getId())).thenComparing(var0x -> var0x.getId().toString()));

      for (PassiveSkillTree var2 : var0) {
         railTrees.add(var2.getId());
      }
   }

   /** Trees opened from a point type button, or hidden by config, stay off the rail. */
   static boolean isSpecial(ResourceLocation tree) {
      return pointTypeOf(tree) != null || AscensionConfig.hiddenFromRail(tree);
   }

   /** The point type whose button opens this tree, or null. */
   static PointType pointTypeOf(ResourceLocation tree) {
      for (PointType type : PointTypes.all()) {
         if (tree.equals(type.openTree)) {
            return type;
         }
      }

      return null;
   }

   private static int headerWidth(PassiveSkillTree var0) {
      int var1 = UiDraw.font().width(treeName(var0.getId()));
      return Math.max(120, 32 + var1 * 2 + 12);
   }

   private static Component treeName(ResourceLocation var0) {
      return Component.translatable(var0.toString());
   }

   public static void render(Screen var0, GuiGraphics var1, int var2, int var3, SkillButtons var4, SkillTreeWidgets var5, PassiveSkillTree var6) {
      int var7 = var0.width;
      int var8 = var0.height;
      chipZones.clear();
      chipActions.clear();
      var1.pose().pushPose();
      var1.pose().translate(0.0F, 0.0F, 400.0F);
      RenderSystem.enableBlend();

      try {
         if (!overUi((double)var2, (double)var3)) {
            SkillButton var9 = (SkillButton)var4.getWidgetAt((double)var2, (double)var3);
            if (var9 != null && var9 != pinned) {
               pinned = var9;
               cardScroll = 0;
            }
         }

         drawHeader(var1, var6);
         drawRail(var1, var2, var3, var6, var8);
         drawChips(var1, var2, var3, var7, var5);
         drawBar(var1, var2, var3, var7, var8);
         drawCard(var1, var7, var8, var6);
      } catch (Throwable var10) {
         fail("render", var10);
      }

      RenderSystem.disableBlend();
      var1.pose().popPose();
   }

   private static void drawHeader(GuiGraphics var0, PassiveSkillTree var1) {
      headerW = headerWidth(var1);
      PointType headerType = pointTypeOf(var1.getId());
      int var2 = headerType != null ? headerType.color : -2578358;
      UiDraw.panel(var0, 8, 8, headerW, 24, -401206752, var2);
      treeIcon(var0, var1.getId(), 12, 11, 0);
      UiDraw.text(var0, treeName(var1.getId()), 36, 13, -11654, 2);
   }

   private static void treeIcon(GuiGraphics var0, ResourceLocation var1, int var2, int var3, int var4) {
      ResourceLocation var5 = var1.withPrefix("textures/icons/skill_tree/").withSuffix(".png");
      var0.blit(var5, var2, var3, 0.0F, (float)(var4 * 19), 19, 19, 19, 57);
   }

   private static void drawRail(GuiGraphics var0, int var1, int var2, PassiveSkillTree var3, int var4) {
      int var5 = 40;
      int var6 = var4 - var5 - 60;
      railVisible = Math.max(1, Math.min(railTrees.size(), (var6 - 16) / 22));
      railStart = Math.max(0, Math.min(railStart, railTrees.size() - railVisible));
      boolean var7 = railStart > 0;
      int var8 = railTrees.size() - railStart - railVisible;
      int var9 = railVisible * 22 + 8 + (var7 ? 8 : 0) + (var8 > 0 ? 16 : 0);
      rail = new int[]{8, var5, 27, var9};
      UiDraw.panel(var0, 8, var5, 27, var9, -401206752, -12831408);
      int var10 = var5 + 4;
      if (var7) {
         arrow(var0, 21, var10 + 1, true);
         var10 += 8;
      }

      Component var11 = null;
      int var12 = 0;

      for (int var13 = railStart; var13 < railStart + railVisible; var13++) {
         ResourceLocation var14 = railTrees.get(var13);
         boolean var15 = var14.equals(var3.getId());
         boolean var16 = UiDraw.inside((double)var1, (double)var2, 10, var10, 23, 22);
         if (var15 || var16) {
            UiDraw.panel(var0, 10, var10, 23, 21, var15 ? -265937866 : -401206752, var15 ? -2578358 : -6646866);
         }

         treeIcon(var0, var14, 12, var10 + 1, var16 ? 1 : 0);
         if (var16) {
            var11 = treeName(var14);
            var12 = var10 + 5;
         }

         var10 += 22;
      }

      if (var8 > 0) {
         arrow(var0, 21, var10 + 1, false);
         MutableComponent var17 = Component.literal("+" + var8);
         var0.drawString(UiDraw.font(), var17, 22 - UiDraw.font().width(var17) / 2, var10 + 6, -6646866, false);
      }

      if (var11 != null) {
         int var18 = UiDraw.font().width(var11) + 10;
         UiDraw.panel(var0, 40, var12 - 3, var18, 15, -401206752, -12831408);
         var0.drawString(UiDraw.font(), var11, 45, var12 + 1, -1514768, false);
      }
   }

   private static void arrow(GuiGraphics var0, int var1, int var2, boolean var3) {
      for (int var4 = 0; var4 < 4; var4++) {
         int var5 = var3 ? var4 : 3 - var4;
         var0.fill(var1 - var5 - 1, var2 + var4, var1 + var5 + 1, var2 + var4 + 1, -6646866);
      }
   }

   private static void drawChips(GuiGraphics var0, int var1, int var2, int var3, SkillTreeWidgets var4) {
      int x = var3 - 8;
      List<PointType> types = new ArrayList<>(PointTypes.all());
      java.util.Collections.reverse(types);

      for (PointType type : types) {
         if (!type.showInHeader) {
            continue;
         }

         int amount = type.nativePoints ? var4.skillPoints : PstClientPoints.left(var4, type.id);
         ResourceLocation icon = type.iconTree != null ? type.iconTree : type.openTree;
         ResourceLocation target = type.openTree;
         Runnable action = target == null || dev.skillascension.common.LockedTrees.isLocked(target) ? null : () -> openTree(target);
         x = chip(var0, var1, var2, x, icon, type.name, free(amount), type.color, action);
      }
   }

   private static String free(int var0) {
      return Component.translatable("skillascension.pst_ui.free", new Object[]{var0}).getString();
   }

   private static int chip(GuiGraphics var0, int var1, int var2, int var3, ResourceLocation var4, Component var5, String var6, int var7, Runnable var8) {
      Font var9 = UiDraw.font();
      MutableComponent var10 = var6 == null ? null : Component.literal(var6);
      int var11 = var9.width(var5) + (var10 == null ? 0 : var9.width(var10) + 5);
      int var12 = var11 + (var4 != null ? 26 : 25);
      int var13 = var3 - var12;
      boolean var14 = var8 != null && UiDraw.inside((double)var1, (double)var2, var13, 12, var12, 16);
      UiDraw.chip(var0, var13, 12, var12, 16, var14 ? -265937866 : -401206752, var14 ? UiDraw.brighten(var7, 1.25F) : var7);
      int var15 = var13 + 8;
      if (var4 != null) {
         var0.pose().pushPose();
         var0.pose().translate((float)(var13 + 5), 14.0F, 0.0F);
         var0.pose().scale(0.6315789F, 0.6315789F, 1.0F);
         treeIcon(var0, var4, 0, 0, 0);
         var0.pose().popPose();
         var15 = var13 + 19;
      } else {
         var0.drawString(var9, Component.literal("✦"), var15 - 1, 16, var7, false);
         var15 += 9;
      }

      var0.drawString(var9, var5, var15, 16, -1514768, true);
      if (var10 != null) {
         var0.drawString(var9, var10, var15 + var9.width(var5) + 5, 16, var7, true);
      }

      chipZones.add(new int[]{var13, 12, var12, 16});
      chipActions.add(var8);
      return var13 - 6;
   }

   private static void openTree(ResourceLocation tree) {
      if (SkillTreesReloader.getSkillTrees().containsKey(tree)) {
         click();
         Minecraft.getInstance().setScreen(new SkillTreeScreen(tree));
      }
   }

   private static void click() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private static int level(Player var0) {
      IPlayerSkills var1 = PlayerSkillsProvider.get(var0);
      return var1.getPlayerSkills().size() + var1.getSkillPoints();
   }

   private static void drawBar(GuiGraphics var0, int var1, int var2, int var3, int var4) {
      LocalPlayer var5 = Minecraft.getInstance().player;
      if (var5 != null && ServerConfig.enable_exp_exchange) {
         Font var6 = UiDraw.font();
         int var7 = level(var5);
         boolean var8 = var7 + PstClientRanks.levelDelta() >= ServerConfig.max_skill_points;
         long var9 = ExpHelper.getPlayerExp(var5);
         int var11 = var8 ? 1 : ServerConfig.getSkillPointCost(var7);
         float var12 = var8 ? 1.0F : Math.min(1.0F, (float)var9 / (float)var11);
         int var13 = Math.min(360, var3 - 16 - 90);
         int var14 = (var3 - var13) / 2;
         int var15 = var4 - 15;
         int var16 = 9;
         bar = new int[]{var14, var15 - 2, var13, var16 + 4};
         UiDraw.panel(var0, var14 - 2, var15 - 2, var13 + 4, var16 + 4, -804517614, -12831408);
         int var17 = (int)((float)var13 * var12);
         if (var17 > 0) {
            var0.fillGradient(var14, var15, var14 + var17, var15 + var16, -9444790, -13722078);
            long var18 = System.currentTimeMillis() / 90L;

            for (int var20 = 0; var20 + 2 <= var17; var20 += 6) {
               int var21 = (int)(((long)(var20 * 7) + var18) % (long)(var16 - 2));
               var0.fill(var14 + var20, var15 + 1 + var21, var14 + var20 + 2, var15 + 2 + var21, 1358954495);
            }

            var0.fill(var14, var15, var14 + var17, var15 + 1, 1627389951);
            var0.fill(var14 + var17 - 1, var15, var14 + var17, var15 + var16, -1507368);
         }

         String var32 = (int)(var12 * 100.0F) + "%";
         int var19 = var6.width(var32);
         int var33 = Math.max(var14 + 2, Math.min(var14 + var13 - var19 - 2, var14 + var17 - var19 - 4));
         int var34 = var15 + 1;

         for (int[] var25 : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            var0.drawString(var6, var32, var33 + var25[0], var34 + var25[1], -3148352, false);
         }

         var0.drawString(var6, var32, var33, var34, -15848950, false);
         int var35 = (var8 ? var7 - 1 : var7) + PstClientRanks.levelDelta();
         MutableComponent var36 = Component.translatable("skillascension.pst_ui.level", new Object[]{var35});
         MutableComponent var37 = Component.translatable("skillascension.pst_ui.level", new Object[]{var35 + 1});
         int var38 = var6.width(var36) + 12;
         UiDraw.chip(var0, var14 - var38 - 8, var15 - 3, var38, 15, -401206752, -2578358);
         var0.drawString(var6, var36, var14 - var38 - 2, var15 + 1, -11654, true);
         int var26 = var6.width(var37) + 12;
         UiDraw.chip(var0, var14 + var13 + 8, var15 - 3, var26, 15, -401206752, -6646866);
         var0.drawString(var6, var37, var14 + var13 + 14, var15 + 1, -1514768, true);
         ready = new int[4];
         int var27 = var15 - 15;
         if (var8) {
            MutableComponent var28 = Component.translatable("skillascension.pst_ui.max_level");
            var0.drawString(var6, var28, var3 / 2 - var6.width(var28) / 2, var27 + 3, -11654, true);
         } else if (var9 >= (long)var11) {
            MutableComponent var39 = Component.translatable("skillascension.pst_ui.point_ready");
            int var29 = var6.width(var39) + 14;
            boolean var30 = UiDraw.inside((double)var1, (double)var2, var14, var27, var29, 13);
            float var31 = 0.75F + 0.25F * (float)Math.sin((double)System.currentTimeMillis() / 300.0);
            UiDraw.chip(var0, var14, var27, var29, 13, var30 ? -265937866 : -401206752, UiDraw.brighten(-11654, var30 ? 1.0F : var31));
            var0.drawString(var6, var39, var14 + 7, var27 + 3, -11654, true);
            ready = new int[]{var14, var27, var29, 13};
         } else {
            MutableComponent var40 = Component.translatable(
               "skillascension.pst_ui.xp_left", new Object[]{Component.literal(Long.toString((long)var11 - var9)).withStyle(ChatFormatting.GOLD)}
            );
            var0.drawString(var6, var40, var3 / 2 - var6.width(var40) / 2, var27 + 3, -1514768, true);
         }

         if (UiDraw.inside((double)var1, (double)var2, bar[0], bar[1], bar[2], bar[3])) {
            MutableComponent var41 = Component.translatable("skillascension.pst_ui.xp_exact", new Object[]{var9, var8 ? "-" : Integer.toString(var11)});
            var0.renderTooltip(var6, var41, var1, var2);
         }
      } else {
         bar = new int[4];
         ready = new int[4];
      }
   }

   private static int typeColor(SkillButton var0) {
      String var1 = var0.skill.getTitleColor();
      if (var1 != null && var1.length() == 6) {
         try {
            return 0xFF000000 | Integer.parseInt(var1, 16);
         } catch (NumberFormatException var4) {
         }
      }

      String var2 = type(var0);
      switch (var2) {
         case "keystone":
            return -2065825;
         case "notable":
            return -2578358;
         case "gateway":
            return -11549735;
         default:
            return -7562316;
      }
   }

   private static String type(SkillButton var0) {
      String var1 = var0.skill.getTooltipFrameTexture().getPath();
      int var2 = var1.lastIndexOf(47);
      String var3 = var1.substring(var2 + 1).replace(".png", "");
      var3 = var3.replaceAll("_(green|lightblue|magenta|orange|purple|red|teal|yellow)$", "");
      return var3.isEmpty() ? "lesser" : var3;
   }

   private static void drawCard(GuiGraphics var0, int var1, int var2, PassiveSkillTree var3) {
      card = new int[4];
      SkillButton var4 = pinned;
      if (var4 != null) {
         Font var5 = UiDraw.font();
         int var6 = Math.max(150, Math.min(210, (int)((float)var1 * 0.3F)));
         int var7 = var1 - 8 - var6;
         int var8 = 36;
         int var9 = var2 - var8 - 60;
         List var10 = var4.getSkillTooltip(var3);
         if (!var10.isEmpty()) {
            MutableComponent var11 = (MutableComponent)var10.get(0);
            ArrayList<FormattedCharSequence> var12 = new ArrayList<>();
            ArrayList<Boolean> var13 = new ArrayList<>();

            for (int var14 = 1; var14 < var10.size(); var14++) {
               MutableComponent var15 = (MutableComponent)var10.get(var14);
               if (var15.getString().isBlank()) {
                  var12.add(FormattedCharSequence.EMPTY);
                  var13.add(true);
               } else {
                  for (FormattedCharSequence var17 : var5.split(var15, var6 - 16)) {
                     var12.add(var17);
                     var13.add(false);
                  }
               }
            }

            while (!var12.isEmpty() && var13.get(0)) {
               var12.remove(0);
               var13.remove(0);
            }

            int var34 = 0;

            for (boolean var37 : var13) {
               var34 += var37 ? 5 : 10;
            }

            String var36 = var4.skillLearned ? "learned" : (var4.canLearn ? "can_learn" : "locked");
            int var38 = 44;
            int var39 = var38 + var34 + 22;
            int var18 = Math.min(var9, var39);
            cardMaxScroll = Math.max(0, var39 - var18);
            cardScroll = Math.max(0, Math.min(cardScroll, cardMaxScroll));
            int var19 = typeColor(var4);
            card = new int[]{var7, var8, var6, var18};
            UiDraw.panel(var0, var7, var8, var6, var18, -401206752, var19);
            int var20 = 32;
            int var21 = var7 + 7;
            int var22 = var8 + 6;
            UiDraw.panel(var0, var21 - 1, var22 - 1, var20 + 2, var20 + 2, -804517614, UiDraw.brighten(var19, 0.6F));
            int var23 = var4.skill.getSkillSize();
            ResourceLocation var24 = var4.skill.getFrameTexture();
            int var25 = var20 / 2;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            var0.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            var0.blit(var24, var21, var22, var20, var20, 0.0F, 0.0F, var23, var23, var23 * 3, var23);
            var0.blit(
               var4.skill.getIconTexture(), var21 + (var20 - var25) / 2, var22 + (var20 - var25) / 2, var25, var25, 0.0F, 0.0F, var23, var23, var23, var23
            );
            var0.blit(var24, var21, var22, var20, var20, (float)(var23 * 2), 0.0F, var23, var23, var23 * 3, var23);
            RenderSystem.disableBlend();
            int var26 = var7 + 46;
            int var27 = var5.width(var11) * 2 <= var6 - 54 ? 2 : 1;
            UiDraw.text(var0, var11, var26, var8 + (var27 == 2 ? 14 : 18), -1, var27);
            var0.fill(var7 + 7, var8 + var38 - 3, var7 + var6 - 7, var8 + var38 - 2, UiDraw.brighten(var19, 0.45F));
            int var28 = var8 + var38;
            int var29 = var8 + var18 - 20;
            var0.enableScissor(var7 + 1, var28, var7 + var6 - 1, var29);
            int var30 = var28 + 2 - cardScroll;

            for (int var31 = 0; var31 < var12.size(); var31++) {
               if ((Boolean)var13.get(var31)) {
                  var30 += 5;
               } else {
                  if (var30 > var28 - 10 && var30 < var29) {
                     var0.drawString(var5, (FormattedCharSequence)var12.get(var31), var7 + 8, var30, -1514768, true);
                  }

                  var30 += 10;
               }
            }

            var0.disableScissor();
            if (cardMaxScroll > 0) {
               int var40 = var29 - var28 - 4;
               int var32 = Math.max(8, var40 * (var29 - var28) / (var34 + 4));
               int var33 = var28 + 2 + (var40 - var32) * cardScroll / cardMaxScroll;
               var0.fill(var7 + var6 - 4, var28 + 2, var7 + var6 - 3, var28 + 2 + var40, 1090519039);
               var0.fill(var7 + var6 - 5, var33, var7 + var6 - 2, var33 + var32, UiDraw.brighten(var19, 0.8F));
            }

            var0.fill(var7 + 7, var8 + var18 - 19, var7 + var6 - 7, var8 + var18 - 18, UiDraw.brighten(var19, 0.45F));
            MutableComponent var41 = Component.translatable("skillascension.pst_ui.state." + var36);
            int var42 = var4.skillLearned ? -11654 : (var4.canLearn ? -8593270 : -6646866);
            var0.drawString(var5, var41, var7 + var6 / 2 - var5.width(var41) / 2, var8 + var18 - 13, var42, true);
         }
      }
   }

   public static boolean overUi(double var0, double var2) {
      if (!UiDraw.inside(var0, var2, 8, 8, headerW, 24)
         && !UiDraw.inside(var0, var2, rail[0], rail[1], rail[2], rail[3])
         && !UiDraw.inside(var0, var2, card[0], card[1], card[2], card[3])
         && !UiDraw.inside(var0, var2, bar[0], bar[1], bar[2], bar[3])
         && !UiDraw.inside(var0, var2, ready[0], ready[1], ready[2], ready[3])) {
         for (int[] var5 : chipZones) {
            if (UiDraw.inside(var0, var2, var5[0], var5[1], var5[2], var5[3])) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean click(Screen var0, double var1, double var3, int var5, PassiveSkillTree var6) {
      try {
         if (var5 != 0) {
            return overUi(var1, var3);
         } else if (UiDraw.inside(var1, var3, ready[0], ready[1], ready[2], ready[3]) && buyAction != null) {
            int var12 = Screen.hasShiftDown() ? 10 : 1;

            for (int var14 = 0; var14 < var12; var14++) {
               buyAction.run();
            }

            click();
            return true;
         } else {
            for (int var7 = 0; var7 < chipZones.size(); var7++) {
               int[] var8 = chipZones.get(var7);
               if (UiDraw.inside(var1, var3, var8[0], var8[1], var8[2], var8[3])) {
                  if (chipActions.get(var7) != null) {
                     chipActions.get(var7).run();
                  }

                  return true;
               }
            }

            if (UiDraw.inside(var1, var3, rail[0], rail[1], rail[2], rail[3])) {
               int var11 = rail[1] + 4;
               if (railStart > 0) {
                  if (var3 < (double)(var11 + 8)) {
                     railStart--;
                     return true;
                  }

                  var11 += 8;
               }

               int var13 = (int)((var3 - (double)var11) / 22.0);
               if (var13 >= railVisible) {
                  railStart++;
                  return true;
               } else {
                  if (var13 >= 0 && railStart + var13 < railTrees.size()) {
                     ResourceLocation var9 = railTrees.get(railStart + var13);
                     if (!var9.equals(var6.getId())) {
                        click();
                        Minecraft.getInstance().setScreen(new SkillTreeScreen(var9));
                     }
                  }

                  return true;
               }
            } else {
               return overUi(var1, var3);
            }
         }
      } catch (Throwable var10) {
         fail("click", var10);
         return false;
      }
   }

   public static boolean scroll(double var0, double var2, double var4) {
      if (UiDraw.inside(var0, var2, card[0], card[1], card[2], card[3])) {
         cardScroll = Math.max(0, Math.min(cardMaxScroll, cardScroll - (int)(var4 * 12.0)));
         return true;
      } else if (UiDraw.inside(var0, var2, rail[0], rail[1], rail[2], rail[3])) {
         railStart = Math.max(0, railStart - (int)Math.signum(var4));
         return true;
      } else {
         return overUi(var0, var2);
      }
   }
}
