package dev.skillascension.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component.Serializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.network.NetworkHelper"},
   remap = false
)
public abstract class PstChatComponentMixin {
   @Unique
   private static final String skillascension$MARKER = "\u0000skillascension:json\u0000";
   @Unique
   private static final int skillascension$MAX_LENGTH = 262144;

   @Inject(
      method = {"writeChatComponent"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private static void skillascension$writeAsJson(FriendlyByteBuf buf, MutableComponent component, CallbackInfo ci) {
      String json;
      try {
         json = Serializer.toJson(component);
      } catch (RuntimeException var6) {
         return;
      }

      ci.cancel();
      buf.writeUtf("\u0000skillascension:json\u0000" + json, 262144);
      Style style = component.getStyle();
      buf.writeBoolean(style.isBold());
      buf.writeBoolean(style.isItalic());
      buf.writeBoolean(style.isUnderlined());
      buf.writeBoolean(style.isStrikethrough());
      buf.writeBoolean(style.isObfuscated());
      TextColor color = style.getColor();
      buf.writeInt(color == null ? -1 : color.getValue());
   }

   @Inject(
      method = {"readChatComponent"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private static void skillascension$readJson(FriendlyByteBuf buf, CallbackInfoReturnable<MutableComponent> cir) {
      int start = buf.readerIndex();
      String text = buf.readUtf(262144);
      if (!text.startsWith("\u0000skillascension:json\u0000")) {
         buf.readerIndex(start);
      } else {
         MutableComponent component = null;

         try {
            component = Serializer.fromJson(text.substring("\u0000skillascension:json\u0000".length()));
         } catch (RuntimeException var6) {
         }

         buf.readBoolean();
         buf.readBoolean();
         buf.readBoolean();
         buf.readBoolean();
         buf.readBoolean();
         buf.readInt();
         cir.setReturnValue(component != null ? component : Component.literal(text.substring("\u0000skillascension:json\u0000".length())));
      }
   }
}
