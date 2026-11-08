/*
 * GildedMod
 * Copyright (C) 2026 Onicox
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gildedmod;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class BlackGoldSword extends SwordItem {

    /**
     * 天罚闪电的标记名。
     * <p>为什么要用"自定义名"当标记：渲染发生在<b>客户端</b>，而 {@code LightningBolt} 的
     * {@code cause} / {@code visualOnly} / {@code damage} 全是<b>纯服务端字段</b>
     * （反编译确认 {@code getCause()} 就是一句 {@code getfield cause}，类里没有任何
     * SynchedEntityData）→ 客户端读到的一律是 null ✗。
     * 而实体自定义名走的是 {@code DATA_CUSTOM_NAME}，是<b>会同步</b>的 ✓，
     * 配合 {@code setCustomNameVisible(false)} 又完全不会显示出来 ✓。
     */
    public static final String GILDED_LIGHTNING_TAG = "gildedmod:cosmic_lightning";

    public BlackGoldSword() {
        super(
                Tiers.NETHERITE,
                0,
                -2.0F,
                new Item.Properties()
                        .durability(1)
                        .rarity(Rarity.EPIC)
                        .fireResistant()
        );
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {}

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            String entityName = target.getName().getString();
            attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.0F, 0.5F);

            if (attacker instanceof Player player) {
                player.sendSystemMessage(Component.literal(
                        "§c☠ §6GILDED §c☠ §8» §7" + entityName + " §8已被§c§k context§r§7鎏金神力§c§k context-two§r粉碎"
                ));
            }
        }
        return true;
    }

    private static boolean isHasAnyBlackGoldWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return (mainHand.getItem() instanceof BlackGoldSword) ||
                (offHand.getItem() instanceof BlackGoldSword) ||
                (mainHand.getItem() instanceof BlackGoldPickaxe) ||
                (offHand.getItem() instanceof BlackGoldPickaxe) ||
                (mainHand.getItem() instanceof BlackGoldAxe) ||
                (offHand.getItem() instanceof BlackGoldAxe);
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level world, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity miner) {
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // Shift + 右键：范围抹除（鎏金闪电 + nativeAbsoluteKill 绝对抹除，无掉落）
            annihilate(world, player);
        } else {
            // 右键：天罚（正常死亡流程，保留掉落）
            castLightning(world, player);
        }
        return InteractionResultHolder.success(stack);
    }

    /** 天罚 / 范围抹除共用的作用范围（相对玩家 ±128 / ±64）。想改范围只动这里。 */
    private static AABB strikeBox(Player player) {
        return new AABB(
                player.getX() - 128, player.getY() - 64, player.getZ() - 128,
                player.getX() + 128, player.getY() + 64, player.getZ() + 128
        );
    }

    /**
     * 是否生成"鎏金闪电"实体。<b>true（当前）：保留闪电视觉。</b>
     * <p>想关掉就改 false（渲染侧 {@code LightningBoltRendererMixin} 会跟着一起失效，
     * 因为没有带标记的闪电实体了）。
     */
    private static final boolean SPAWN_LIGHTNING = true;

    /**
     * 闪电落点是否产生爆炸。<b>false（当前）：不爆炸。</b>
     * <p>爆炸是"击杀延迟"的主要来源：每个实体一次 {@code world.explode}（粒子 + 范围计算 +
     * 伤害结算），几十个实体就会把服务端那一 tick 拉长，而实体生成包/音效要等 tick 结束才发，
     * 表现为"击杀和闪电延迟"。
     */
    private static final boolean EXPLODE_ON_STRIKE = false;

    /**
     * 超长攻击距离：触及属性（Forge 的 ENTITY_REACH / BLOCK_REACH）。
     * <p>两个数值都是<b>在基础值上叠加</b>：
     * <ul>
     *     <li>攻击距离：原版基础 3.0 + 128 → <b>约 131 格</b>（准星能锁到多远就能打多远）</li>
     *     <li>挖/放距离：原版基础 4.5 + 16 → 约 20.5 格（不做太夸张，否则容易误挖）</li>
     * </ul>
     * <p>注意：实际能打到的上限还受<b>客户端实体同步范围</b>限制 —— 没同步到客户端的实体，
     * 准星根本选不中，再远也打不到。所以"极长"在开阔地很爽，但别指望打穿到别的区块。
     */
    private static final UUID REACH_UUID = UUID.fromString("8d0f2a53-6f2f-4a1b-9d5c-7a1f3e2b6c40");
    private static final double ENTITY_REACH_BONUS = 128.0D;   // 3.0 → 约 131 格
    private static final double BLOCK_REACH_BONUS = 16.0D;     // 4.5 → 约 20.5 格

    /**
     * 给主手加上"超长触及"。
     * <p>Forge 的两条属性 {@link ForgeMod#ENTITY_REACH} / {@link ForgeMod#BLOCK_REACH} 会同时影响
     * <b>客户端的准星选取</b>和<b>服务端的距离校验</b>，所以加在这里才是真正生效的攻击距离，
     * 而不是只在客户端视觉上"够到"。
     * <p>注意 {@code super.getDefaultAttributeModifiers(slot)} 返回的是<b>不可变</b>集合，
     * 直接 {@code put} 会抛异常，所以先复制成 {@link HashMultimap} 再追加。
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> map =
                HashMultimap.create(super.getDefaultAttributeModifiers(slot));
        if (slot == EquipmentSlot.MAINHAND) {
            map.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                    REACH_UUID, "gildedmod_reach_entity", ENTITY_REACH_BONUS,
                    AttributeModifier.Operation.ADDITION));
            map.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(
                    REACH_UUID, "gildedmod_reach_block", BLOCK_REACH_BONUS,
                    AttributeModifier.Operation.ADDITION));
        }
        return map;
    }

    /**
     * 闪电落地时的爆炸半径（TNT 是 4.0，这里取 3.0）。
     * <p>爆炸类型是 {@link Level.ExplosionInteraction#NONE} —— 有爆炸的视觉、音效、击退与伤害，
     * 但<b>不破坏任何方块</b>（地形安全）。
     */
    private static final float EXPLOSION_RADIUS = 3.0F;

    /**
     * 生成一道鎏金闪电（带标记 → 渲染侧走星空皮肤；visualOnly → 不留火）。
     * <p>{@code public} 是为了让左键命中的 {@code BlackGoldSwordMixin} 也能调用 ✓。
     */
    public static void spawnGildedLightning(Level world, Player player, Entity at) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning == null) {
            System.out.println("[GILDED] LIGHTNING_BOLT.create 返回 null ✗（闪电没能生成）");
            return;
        }
        lightning.moveTo(at.position());
        lightning.setVisualOnly(true);
        // 标记用会同步到客户端的自定义名（cause 是纯服务端字段，客户端读不到 ✗）
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            lightning.setCause(serverPlayer);
        }
        lightning.setCustomName(Component.literal(GILDED_LIGHTNING_TAG));
        lightning.setCustomNameVisible(false);
        world.addFreshEntity(lightning);
        System.out.println("[GILDED] 鎏金闪电已生成 @ " + String.format("%.1f, %.1f, %.1f",
                at.getX(), at.getY(), at.getZ()) + "  客户端=" + world.isClientSide);
    }

    /**
     * 在某个位置炸一下（有视觉/音效/击退/伤害，但<b>不破坏方块</b>）。
     * <p>和闪电解耦了：即使 {@link #SPAWN_LIGHTNING} 关掉，爆炸依然保留。
     */
    private static void explodeAt(Level world, Player player, Entity at) {
        //   fire = false              → 不引燃火焰
        //   ExplosionInteraction.NONE → 不破坏方块（地形安全）
        // 只在服务端发生：Level.explode 在客户端会直接返回 null，无需额外判断。
        world.explode(player, at.getX(), at.getY(), at.getZ(),
                EXPLOSION_RADIUS, false, Level.ExplosionInteraction.NONE);
    }

    /**
     * Shift + 右键：<b>范围绝对抹除</b>。
     * <p>和天罚（{@link #castLightning}）的区别：
     * <ul>
     *     <li>天罚走 {@code setHealth(0) + die} → 有死亡动画、<b>有掉落</b>；</li>
     *     <li>这里直接 {@code LiuCore.nativeAbsoluteKill} → JNI 字段直写
     *         （{@code removalReason} / {@code dead} + {@code setRemoved}），
     *         不经过任何可被 mixin 拦截的方法，<b>没有掉落</b>，是真正的"抹除"。</li>
     * </ul>
     * <p>注意：native 调用<b>必须只在服务端跑</b>（客户端跑不仅无效，还会造成不同步；
     * 库里没有导出符号时更会直接抛 UnsatisfiedLinkError），所以下面第一行就是客户端早退。
     */
    private void annihilate(Level world, Player player) {
        if (world.isClientSide) {
            return;
        }

        List<Entity> entities = world.getEntities(player, strikeBox(player),
                entity -> entity != player && entity.isAlive());

        // 播报必须在"重活"之前发出去 ✗→✓
        // sendSystemMessage 只是入队，真正的网络包要等这一 tick 结束才 flush；
        // 而下面的循环里有 nativeAbsoluteKill + 每个实体一次 world.explode（破坏方块、
        // 伤害、光照更新），几十个实体能让这个 tick 拉长到肉眼可见 → 播报就"延后"了。
        // 先播报（数量直接取列表大小，反正全都会被杀），再干活，反馈就是即时的。
        broadcast(world, player,
                "§e⚡ §6GODSWORD §e⚡ §8» §7" + player.getName().getString() +
                        " §8发动了 §c鎏金·湮灭 §8» §7消逝了 §c" + entities.size() + " §7个低等生物体");

        for (Entity entity : entities) {
            if (SPAWN_LIGHTNING) {
                spawnGildedLightning(world, player, entity);
            }
            if (EXPLODE_ON_STRIKE) {
                explodeAt(world, player, entity);
            }

            if (entity instanceof LivingEntity living) {
                // 绝对抹除：字段直写，绕开 hurt/setHealth/die/remove 全部守卫
                LiuCore.nativeAbsoluteKill(living);
            } else {
                // 非生物实体（掉落物、展示框等）没有 dead 字段，native 只能写 removalReason，
                // 这里再补一次 remove 让 levelCallback 正常跑完
                entity.remove(Entity.RemovalReason.KILLED);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 0.5F);
    }

    /** 给所有玩家发一条播报。 */
    private static void broadcast(Level world, Player actor, String rawText) {
        Component message = Component.literal(rawText);
        for (Player p : world.players()) {
            p.sendSystemMessage(message);
        }
    }

    private void castLightning(Level world, Player player) {
        if (!world.isClientSide) {
            AABB box = strikeBox(player);

            List<Entity> entities = world.getEntities(player, box,
                    entity -> entity != player && entity.isAlive());

            // 播报先发（理由同 annihilate：循环里有爆炸 + 死亡流程，等它跑完这一 tick 就太长了）
            broadcast(world, player,
                    "§e⚡ §6GODSWORD §e⚡ §8» §7" + player.getName().getString() +
                            " §8发动了 §e鎏金·神力 §8» §7寂湮了 §c" + entities.size() + " §7个低等生物体");

            // 天罚：无差别抹除（只要是实体就杀）——三层保险，越往后越强硬
            //   第 1 层：打开 LiuBypass，让自家 LiuArmorMixin 的 hurt/setHealth/die/remove
            //           守卫在本次循环内让路（否则穿满黑金甲的目标会被自己的守卫保下来）
            //   第 2 层：正常死亡流程 setHealth(0) + die(...) —— 保留死亡动画与掉落物
            //   第 3 层：若目标依然没死（被其它 mod 的方法级拦截挡住），
            //           用 nativeAbsoluteKill 走 JNI 字段直写（removalReason/dead + setRemoved），
            //           那是不经过任何方法、拦不到的层面
            LiuBypass.setBypass(true);
            try {
                for (Entity entity : entities) {
                    // 闪电保留；爆炸默认关闭（见 EXPLODE_ON_STRIKE）
                    if (SPAWN_LIGHTNING) {
                        spawnGildedLightning(world, player, entity);
                    }
                    if (EXPLODE_ON_STRIKE) {
                        explodeAt(world, player, entity);
                    }

                    // 第 2 层：正常死亡流程 —— 保留死亡动画与掉落物
                    // （LiuBypass 已打开，自家的 hurt/setHealth/die/remove 守卫本次不会拦）
                    if (entity instanceof LivingEntity living) {
                        living.setHealth(0);
                        living.die(world.damageSources().magic());

                        // 第 3 层：只要它还留在世界里（说明被别的方法级拦截挡住了，
                        // 例如 ASM 那套守卫只看系统属性、不吃 LiuBypass），
                        // 就改用 JNI 字段直写绝对抹除 —— removalReason/dead + setRemoved，
                        // 全程不经过任何可被 mixin 拦截的方法。
                        if (!living.isRemoved()) {
                            LiuCore.nativeAbsoluteKill(living);
                        }
                    } else {
                        entity.remove(Entity.RemovalReason.KILLED);
                    }
                }
            } finally {
                LiuBypass.setBypass(false);
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 1.0F);

        } else {
            for (int i = 0; i < 300; i++) {
                world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + (world.random.nextDouble() - 0.5) * 100,
                        player.getY() + world.random.nextDouble() * 20,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 100,
                        0, 0, 0);
            }
            player.displayClientMessage(Component.literal("§e⚡ §l神权·天罚 §e⚡"), true);
            player.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 1.0F);
        }
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClientSide) {
            stack.getOrCreateTag().putLong("AnimTick", System.currentTimeMillis());
        }
        if (world.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        if (selected) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                // 排除穿满黑金甲：否则穿满甲 + 剑在背包时，这里会每 tick 把飞行踢掉
                if (!isHasAnyBlackGoldWeapon(player)
                        && !GildedMod.hasFullBlackGoldArmor(player)
                        && player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }

        if (selected && player.tickCount % 40 == 0) {
            player.displayClientMessage(
                    Component.literal("§6§lGODSWORD §8| §e§l天罚 §8| §7右键释放"),
                    true
            );
        }
    }

    private static Component rainbowSmooth(String text, long offsetMs, float hueStart, float hueSpan) {
        MutableComponent root = Component.empty();
        int len = text.length();
        if (len == 0) return root;

        float timeShift = (offsetMs / 1000.0f) % 1.0f;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                root.append(Component.literal("\n"));
                continue;
            }

            float t = len == 1 ? 0 : (float) i / (len - 1);
            float hue = (hueStart + t * hueSpan + timeShift) % 1.0f;
            if (hue < 0) hue += 1.0f;

            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;

            MutableComponent ch = Component.literal(String.valueOf(c))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
            root.append(ch);
        }
        return root;
    }


    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.clear();

        long t = System.currentTimeMillis();

        // 顶部装饰线（暗金渐变）
        tooltip.add(rainbowSmooth("                                        ", t, 0.8f, 0.6f));

        // 标题：GildedEX 彩虹 + 副标题
        tooltip.add(Component.empty()
                .append(Component.literal("  "))
                .append(rainbowSmooth("GildedEX", t, 1.0f, 1.0f))
                .append(Component.literal("  §7·  §7§l「§e神 陨§7§l」")));

        // 底部装饰线
        tooltip.add(rainbowSmooth("                                        ", t, 0.8f, 0.6f));
        tooltip.add(Component.literal(""));

        // 属性列表
        tooltip.add(Component.literal("  §e✦ 无限耐久"));
        tooltip.add(Component.literal("  §e✦ 免疫一切伤害"));
        tooltip.add(Component.literal("  §e✦ 手持自动飞行"));
        tooltip.add(Component.literal("  §e✦ 左键直接湮灭"));
        tooltip.add(Component.literal(""));

        // 当前神权（彩虹文字）
        tooltip.add(Component.empty()
                .append(Component.literal(""))
                .append(rainbowSmooth("雷 · 罚", t, 1.0f, 1.0f)));

        tooltip.add(Component.literal("  §e✦ §6释放方式    §7»  §7右键"));
        tooltip.add(Component.literal(""));

        tooltip.add(Component.literal("  §7§o「吾持手中之太阿，至以无穷，鎏！」"));
        tooltip.add(Component.literal(""));

        tooltip.add(rainbowSmooth("                                        ", t, 0.8f, 0.6f));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}