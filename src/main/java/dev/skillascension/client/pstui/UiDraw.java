package dev.skillascension.client.pstui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class UiDraw {
   public static final int FILL = -401206752;
   public static final int FILL_HI = -265937866;
   public static final int FILL_INSET = -804517614;
   public static final int BORDER_DIM = -12831408;
   public static final int GOLD = -2578358;
   public static final int GOLD_HI = -11654;
   public static final int TEXT = -1514768;
   public static final int MUTED = -6646866;
   public static final int GREEN = -8593270;
   public static final int VIOLET = -4944641;
   public static final int CYAN = -10497818;

   private UiDraw() {
   }

   public static void panel(GuiGraphics var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      if (var3 >= 4 && var4 >= 4) {
         var0.fill(var1 + 2, var2 + 1, var1 + var3 - 2, var2 + var4 - 1, var5);
         var0.fill(var1 + 1, var2 + 2, var1 + 2, var2 + var4 - 2, var5);
         var0.fill(var1 + var3 - 2, var2 + 2, var1 + var3 - 1, var2 + var4 - 2, var5);
         var0.fill(var1 + 2, var2 + 1, var1 + var3 - 2, var2 + 2, 419430399);
         var0.fill(var1 + 2, var2, var1 + var3 - 2, var2 + 1, var6);
         var0.fill(var1 + 2, var2 + var4 - 1, var1 + var3 - 2, var2 + var4, var6);
         var0.fill(var1, var2 + 2, var1 + 1, var2 + var4 - 2, var6);
         var0.fill(var1 + var3 - 1, var2 + 2, var1 + var3, var2 + var4 - 2, var6);
         var0.fill(var1 + 1, var2 + 1, var1 + 2, var2 + 2, var6);
         var0.fill(var1 + var3 - 2, var2 + 1, var1 + var3 - 1, var2 + 2, var6);
         var0.fill(var1 + 1, var2 + var4 - 2, var1 + 2, var2 + var4 - 1, var6);
         var0.fill(var1 + var3 - 2, var2 + var4 - 2, var1 + var3 - 1, var2 + var4 - 1, var6);
      }
   }

   public static void chip(GuiGraphics var0, int var1, int var2, int var3, int var4, int var5, int var6) {
      panel(var0, var1, var2, var3, var4, var5, var6);
      var0.fill(var1 + 1, var2 + 2, var1 + 2, var2 + 3, var6);
      var0.fill(var1 + 2, var2 + 1, var1 + 3, var2 + 2, var6);
      var0.fill(var1 + var3 - 2, var2 + 2, var1 + var3 - 1, var2 + 3, var6);
      var0.fill(var1 + var3 - 3, var2 + 1, var1 + var3 - 2, var2 + 2, var6);
      var0.fill(var1 + 1, var2 + var4 - 3, var1 + 2, var2 + var4 - 2, var6);
      var0.fill(var1 + 2, var2 + var4 - 2, var1 + 3, var2 + var4 - 1, var6);
      var0.fill(var1 + var3 - 2, var2 + var4 - 3, var1 + var3 - 1, var2 + var4 - 2, var6);
      var0.fill(var1 + var3 - 3, var2 + var4 - 2, var1 + var3 - 2, var2 + var4 - 1, var6);
   }

   public static int brighten(int var0, float var1) {
      int var2 = var0 >>> 24;
      int var3 = Math.min(255, (int)((float)(var0 >> 16 & 0xFF) * var1));
      int var4 = Math.min(255, (int)((float)(var0 >> 8 & 0xFF) * var1));
      int var5 = Math.min(255, (int)((float)(var0 & 0xFF) * var1));
      return var2 << 24 | var3 << 16 | var4 << 8 | var5;
   }

   public static Font font() {
      return Minecraft.getInstance().font;
   }

   public static void text(GuiGraphics var0, Component var1, int var2, int var3, int var4, int var5) {
      if (var5 <= 1) {
         var0.drawString(font(), var1, var2, var3, var4, true);
      } else {
         var0.pose().pushPose();
         var0.pose().translate((float)var2, (float)var3, 0.0F);
         var0.pose().scale((float)var5, (float)var5, 1.0F);
         var0.drawString(font(), var1, 0, 0, var4, true);
         var0.pose().popPose();
      }
   }

   public static boolean inside(double var0, double var2, int var4, int var5, int var6, int var7) {
      return var0 >= (double)var4 && var2 >= (double)var5 && var0 < (double)(var4 + var6) && var2 < (double)(var5 + var7);
   }
}
