package dev.skillascension.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Post;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

public final class PstCooldownHud {
   private static final Map<String, String> ICONS = Map.of(
      "last_page", "minecraft:enchanted_book", "war_cry", "minecraft:goat_horn", "stormcaller", "minecraft:lightning_rod"
   );
   private static final Map<String, long[]> ACTIVE = new LinkedHashMap<>();

   private PstCooldownHud() {
   }

   public static void start(String kind, int ticks) {
      if (ticks > 0) {
         long now = System.currentTimeMillis();
         ACTIVE.put(kind, new long[]{now, now + (long)ticks * 50L});
      }
   }

   @SubscribeEvent
   public static void onOverlay(Post event) {
      if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() && !ACTIVE.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && !mc.options.hideGui) {
            GuiGraphics g = event.getGuiGraphics();
            int x = event.getWindow().getGuiScaledWidth() / 2 - 91 - 22;
            int y = event.getWindow().getGuiScaledHeight() - 19;
            long now = System.currentTimeMillis();
            Iterator<Entry<String, long[]>> it = ACTIVE.entrySet().iterator();

            while (it.hasNext()) {
               Entry<String, long[]> e = it.next();
               long[] t = e.getValue();
               if (now >= t[1]) {
                  it.remove();
               } else {
                  int ix = x - (mc.player.getOffhandItem().isEmpty() ? 0 : 29);
                  Item item = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(ICONS.getOrDefault(e.getKey(), "minecraft:clock")));
                  g.renderItem(new ItemStack(item == null ? Items.CLOCK : item), ix, y);
                  float left = (float)(t[1] - now) / (float)(t[1] - t[0]);
                  int h = Math.round(16.0F * left);
                  g.fill(ix, y + 16 - h, ix + 16, y + 16, -1879048192);
                  String secs = Long.toString((t[1] - now + 999L) / 1000L);
                  g.pose().pushPose();
                  g.pose().translate(0.0F, 0.0F, 200.0F);
                  g.drawString(mc.font, secs, ix + 17 - mc.font.width(secs), y + 9, -1, true);
                  g.pose().popPose();
                  x -= 20;
               }
            }
         } else {
            ACTIVE.clear();
         }
      }
   }
}
