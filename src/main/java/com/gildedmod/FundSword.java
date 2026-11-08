package com.gildedmod;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * 基金之剑（fund_sword）—— <b>纯数值</b>。
 *
 * <p>它<b>不是</b> {@link BlackGoldSword} 的子类，这是故意的：
 * <ul>
 *     <li>不继承天罚 / 范围抹除 / 左键绝对抹除（那些是"鎏"的能力）；</li>
 *     <li>也不套金液 Tooltip 面板 —— 它就是一把数值离谱的剑；</li>
 *     <li>但<b>星空外观仍然保留</b>：物品上的星空层是由
 *         {@code CosmicTextures#maskLocation} 按物品映射的，跟类继承无关。</li>
 * </ul>
 *
 * <p>数值：
 * <ul>
 *     <li>攻击伤害 <b>{@link Double#MAX_VALUE}</b> —— 实际生效时会被 float 上限截断成"必杀"，
 *         但不会变成 NaN（因为属性基础值是有限数，加法不会溢出到无穷）；</li>
 *     <li>攻击距离同"鎏"：{@code ENTITY_REACH +128}（约 131 格）、{@code BLOCK_REACH +16}。</li>
 * </ul>
 */
public class FundSword extends SwordItem {

    private static final UUID DAMAGE_UUID = UUID.fromString("c1a0b7d4-3e52-4f19-8a6b-2d7c5f0e9a13");
    private static final UUID REACH_UUID = UUID.fromString("8d0f2a53-6f2f-4a1b-9d5c-7a1f3e2b6c40");

    /** 纯数值：直接拉满。 */
    private static final double DAMAGE = Double.MAX_VALUE;
    private static final double ENTITY_REACH_BONUS = 128.0D;
    private static final double BLOCK_REACH_BONUS = 16.0D;

    public FundSword() {
        super(Tiers.NETHERITE, 0, -2.0F, new Item.Properties()
                .durability(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  这三条 override 是【必须】的，不是可选优化：
    //  构造器里 durability(1) 表示最大耐久 = 1，而 SwordItem.hurtEnemy 会调用
    //  stack.hurtAndBreak(1, ...) —— 1 >= maxDamage 直接触发销毁。
    //  所以之前：生存模式打一下怪（或挖一个方块，DiggerItem 扣 2），
    //  这把剑当场消失；而它是升级链的必经中间物（下界合金剑 → 基金之剑 → 鎏），
    //  丢了就是物品 + 进度双重损失。创造模式因为 abilities.instabuild 跳过耐久损耗，
    //  所以测试时完全看不出来。
    //
    //  BlackGoldSword 里本来就有这三条（第 77-88 行），FundSword 当时漏抄了。
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 拒绝任何耐久损耗
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> map =
                HashMultimap.create(super.getDefaultAttributeModifiers(slot));
        if (slot == EquipmentSlot.MAINHAND) {
            map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    DAMAGE_UUID, "fund_sword_damage", DAMAGE,
                    AttributeModifier.Operation.ADDITION));
            map.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                    REACH_UUID, "fund_sword_reach_entity", ENTITY_REACH_BONUS,
                    AttributeModifier.Operation.ADDITION));
            map.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(
                    REACH_UUID, "fund_sword_reach_block", BLOCK_REACH_BONUS,
                    AttributeModifier.Operation.ADDITION));
        }
        return map;
    }
}
