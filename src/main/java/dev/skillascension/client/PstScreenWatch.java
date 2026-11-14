package dev.skillascension.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PstScreenWatch {
   private static final Logger LOGGER = LoggerFactory.getLogger("SkillAscension");
   private static long windowStart;
   private static int inits;
   private static int skipped;
   private static int reports;
   private static boolean initPending;
   private static int coalesced;

   private PstScreenWatch() {
   }

   public static void init() {
      long var0 = System.currentTimeMillis();
      if (var0 - windowStart > 2000L) {
         windowStart = var0;
         inits = 0;
      }

      if (++inits == 4 && reports < 20) {
         reports++;
         LOGGER.debug("skill tree screen rebuilt 4 times within 2 s");
      }
   }

   public static void statsSkipped() {
      if (skipped++ % 50 == 0) {
         LOGGER.info("skill tree: repeated stats update ignored, no rebuild ({} so far)", skipped);
      }
   }

   public static void deferInit(Screen var0) {
      if (initPending) {
         if (coalesced++ % 100 == 0) {
            LOGGER.info("skill tree: skill syncs merged into one screen rebuild ({} so far)", coalesced);
         }
      } else {
         initPending = true;
         Minecraft var1 = Minecraft.getInstance();
         var1.tell(() -> {
            initPending = false;
            if (var1.screen == var0) {
               var0.init(var1, var0.width, var0.height);
            }
         });
      }
   }
}
