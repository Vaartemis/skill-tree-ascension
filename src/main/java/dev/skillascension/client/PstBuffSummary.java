package dev.skillascension.client;

import dev.skillascension.pst.RepeatableSkills;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.bonus.SkillBonus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class PstBuffSummary {
   private static final int WRAP = 230;
   private static final Pattern NUMBER = Pattern.compile("(?<!§)([+-]?)(\\d+(?:[.,]\\d+)?)");

   private PstBuffSummary() {
   }

   public static List<Component> build(List<PassiveSkill> learned) {
      Map<String, Integer> effects = new LinkedHashMap<>();
      Map<String, double[]> stats = new LinkedHashMap<>();
      Map<String, String[]> statParts = new LinkedHashMap<>();
      List<SkillBonus<?>> loose = new ArrayList<>();
      Set<String> unlocks = new LinkedHashSet<>();

      for (PassiveSkill skill : learned) {
         int copies = 1 + (RepeatableSkills.isRepeatable(skill) ? (int)Math.round(RepeatableSkills.power(skill, PstClientRanks.rank(skill.getId()))) : 0);
         List<MutableComponent> desc = skill.getDescription();
         if (desc != null && !desc.isEmpty()) {
            for (MutableComponent line : desc) {
               String raw = line.getString();
               String plain = ChatFormatting.stripFormatting(raw);
               plain = plain == null ? "" : plain.trim();
               if (!plain.isEmpty() && !skipLine(raw, plain)) {
                  if (plain.startsWith("▸")) {
                     unlocks.add(plain.substring(1).trim());
                  } else {
                     Matcher m = amount(raw);
                     if (m != null && !raw.contains("§d")) {
                        String head = raw.substring(0, m.start(2));
                        String tail = raw.substring(m.end(2));
                        String key = head + "#" + tail;
                        double v = parse(m.group(2));
                        double[] sum = stats.computeIfAbsent(key, k -> new double[2]);
                        sum[0] += v * (double)copies;
                        sum[1] = Math.max(sum[1], (double)decimals(m.group(2)));
                        statParts.putIfAbsent(key, new String[]{head, tail, m.group(2).contains(",") ? "," : "."});
                     } else {
                        effects.merge(raw, copies, Integer::sum);
                     }
                  }
               }
            }
         } else {
            for (SkillBonus<?> b : skill.getBonuses()) {
               merge(loose, copies > 1 ? b.copy().multiply((double)copies) : b);
            }
         }
      }

      List<Component> out = new ArrayList<>();
      out.add(
         Component.translatable("skillascension.skill_summary.title", new Object[]{learned.size()})
            .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})
      );
      if (!unlocks.isEmpty()) {
         out.add(Component.empty());
         out.add(Component.translatable("skillascension.skill_summary.unlocks").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.UNDERLINE}));
         addWrapped(out, "§f" + String.join("§7, §f", unlocks));
      }

      if (!effects.isEmpty()) {
         out.add(Component.empty());
         out.add(
            Component.translatable("skillascension.skill_summary.effects").withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE})
         );
         effects.forEach((rawx, n) -> addWrapped(out, n > 1 ? rawx + " §8x" + n : rawx));
      }

      if (!stats.isEmpty() || !loose.isEmpty()) {
         out.add(Component.empty());
         out.add(Component.translatable("skillascension.skill_summary.stats").withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.UNDERLINE}));
         List<String> lines = new ArrayList<>();
         stats.forEach((keyx, sumx) -> {
            String[] p = statParts.get(keyx);
            lines.add(p[0] + format(sumx[0], (int)sumx[1], p[2]) + p[1]);
         });
         lines.sort((a, bx) -> sortKey(a).compareTo(sortKey(bx)));

         for (String l : lines) {
            addWrapped(out, l);
         }

         loose.sort(null);

         for (SkillBonus<?> b : loose) {
            out.add(b.getSimpleTooltip());
         }
      }

      if (learned.isEmpty()) {
         out.add(Component.translatable("skillascension.skill_summary.empty").withStyle(ChatFormatting.GRAY));
      }

      return out;
   }

   private static boolean skipLine(String raw, String plain) {
      if (raw.startsWith("§7§o") || raw.startsWith("§8")) {
         return true;
      } else {
         return raw.startsWith("§6") && !plain.startsWith("•") ? true : plain.startsWith("▶") || plain.startsWith("✦");
      }
   }

   private static Matcher amount(String raw) {
      Matcher m = NUMBER.matcher(raw);
      Matcher first = null;
      int firstStart = -1;

      while (m.find()) {
         if (!m.group(1).isEmpty()) {
            Matcher r = NUMBER.matcher(raw);
            r.find(m.start());
            return r;
         }

         if (firstStart < 0) {
            firstStart = m.start();
         }
      }

      if (firstStart < 0) {
         return null;
      } else {
         first = NUMBER.matcher(raw);
         first.find(firstStart);
         return first;
      }
   }

   private static void merge(List<SkillBonus<?>> list, SkillBonus<?> bonus) {
      for (int i = 0; i < list.size(); i++) {
         if (list.get(i).canMerge(bonus)) {
            list.set(i, list.get(i).copy().merge(bonus));
            return;
         }
      }

      list.add(bonus);
   }

   private static double parse(String s) {
      try {
         return Double.parseDouble(s.replace(',', '.'));
      } catch (NumberFormatException var2) {
         return 0.0;
      }
   }

   private static int decimals(String s) {
      int i = Math.max(s.indexOf(46), s.indexOf(44));
      return i < 0 ? 0 : s.length() - i - 1;
   }

   private static String format(double v, int dec, String sep) {
      String s = dec <= 0 && v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%." + Math.max(1, Math.min(2, dec)) + "f", v);
      if (s.contains(".")) {
         s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
      }

      return s.replace(".", sep);
   }

   private static String sortKey(String raw) {
      String plain = ChatFormatting.stripFormatting(raw);
      if (plain == null) {
         plain = raw;
      }

      plain = plain.replaceAll("^[^\\p{L}]*", "").replaceAll("^(de|do|da|of) ", "");
      return plain.toLowerCase(Locale.ROOT);
   }

   private static MutableComponent styled(String raw) {
      MutableComponent root = Component.empty();
      Style style = Style.EMPTY;
      StringBuilder buf = new StringBuilder();

      for (int i = 0; i < raw.length(); i++) {
         char c = raw.charAt(i);
         if (c == 167 && i + 1 < raw.length()) {
            ChatFormatting f = ChatFormatting.getByCode(raw.charAt(i + 1));
            if (f != null) {
               if (buf.length() > 0) {
                  root.append(Component.literal(buf.toString()).withStyle(style));
                  buf.setLength(0);
               }

               style = f == ChatFormatting.RESET ? Style.EMPTY : (f.isColor() ? Style.EMPTY.applyFormat(f) : style.applyFormat(f));
               i++;
               continue;
            }
         }

         buf.append(c);
      }

      if (buf.length() > 0) {
         root.append(Component.literal(buf.toString()).withStyle(style));
      }

      return root;
   }

   private static void addWrapped(List<Component> out, String raw) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.font.width(raw) <= 230) {
         out.add(styled(raw));
      } else {
         StringBuilder line = new StringBuilder();

         for (String word : raw.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && mc.font.width(next) > 230) {
               String done = line.toString();
               out.add(styled(done));
               line = new StringBuilder("   " + activeCodes(done) + word);
            } else {
               line = new StringBuilder(next);
            }
         }

         if (line.length() > 0) {
            out.add(styled(line.toString()));
         }
      }
   }

   private static String activeCodes(String raw) {
      String codes = "";

      for (int i = 0; i + 1 < raw.length(); i++) {
         if (raw.charAt(i) == 167) {
            ChatFormatting f = ChatFormatting.getByCode(raw.charAt(i + 1));
            if (f != null) {
               if (f == ChatFormatting.RESET) {
                  codes = "";
               } else if (f.isColor()) {
                  codes = "§" + raw.charAt(i + 1);
               } else {
                  codes = codes + "§" + raw.charAt(i + 1);
               }

               i++;
            }
         }
      }

      return codes;
   }
}
