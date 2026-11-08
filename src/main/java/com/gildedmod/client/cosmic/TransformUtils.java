package com.gildedmod.client.cosmic;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class TransformUtils {

    public static final PerspectiveModelState IDENTITY = PerspectiveModelState.IDENTITY;
    public static final PerspectiveModelState DEFAULT_BLOCK;
    public static final PerspectiveModelState DEFAULT_ITEM;
    public static final PerspectiveModelState DEFAULT_TOOL;
    public static final PerspectiveModelState DEFAULT_BOW;
    public static final PerspectiveModelState DEFAULT_HANDHELD_ROD;
    public static final PerspectiveModelState DEFAULT_TRIDENT;
    public static final PerspectiveModelState DEFAULT_THROWING_TRIDENT;

    private static final float TORAD = (float) Math.PI / 180.0f;
    private static final Transformation flipX = new Transformation(null, null, new Vector3f(-1, 1, 1), null);

    static {
        Map<ItemDisplayContext, Transformation> map;
        Transformation thirdPerson;
        Transformation firstPerson;

        // DEFAULT_BLOCK
        map = new HashMap<>();
        thirdPerson = create(0F, 2.5F, 0F, 75F, 45F, 0F, 0.375F);
        map.put(ItemDisplayContext.GUI, create(0F, 0F, 0F, 30F, 225F, 0F, 0.625F));
        map.put(ItemDisplayContext.GROUND, create(0F, 3F, 0F, 0F, 0F, 0F, 0.25F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, thirdPerson);
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, flipLeft(thirdPerson));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(0F, 0F, 0F, 0F, 45F, 0F, 0.4F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(0F, 0F, 0F, 0F, 225F, 0F, 0.4F));
        DEFAULT_BLOCK = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // DEFAULT_ITEM
        map = new HashMap<>();
        thirdPerson = create(0F, 3F, 1F, 0F, 0F, 0F, 0.55F);
        firstPerson = create(1.13F, 3.2F, 1.13F, 0F, -90F, 25F, 0.68F);
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.HEAD, create(0F, 13F, 7F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, thirdPerson);
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, flipLeft(thirdPerson));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, firstPerson);
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, flipLeft(firstPerson));
        DEFAULT_ITEM = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // DEFAULT_TOOL
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(0F, 4F, 0.5F, 0F, -90F, 55, 0.85F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(0F, 4F, 0.5F, 0F, 90F, -55, 0.85F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(1.13F, 3.2F, 1.13F, 0F, -90F, 25, 0.68F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(1.13F, 3.2F, 1.13F, 0F, 90F, -25, 0.68F));
        DEFAULT_TOOL = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // DEFAULT_BOW
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(-1F, -2F, 2.5F, -80F, 260F, -40F, 0.9F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(-1F, -2F, 2.5F, -80F, -280F, 40F, 0.9F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(1.13F, 3.2F, 1.13F, 0F, -90F, 25F, 0.68F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(1.13F, 3.2F, 1.13F, 0F, 90F, -25F, 0.68F));
        DEFAULT_BOW = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // DEFAULT_HANDHELD_ROD
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(0F, 2F, 0F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(0F, 4F, 2.5F, 0F, 90F, 55F, 0.85F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(0F, 4F, 2.5F, 0F, -90F, -55F, 0.85F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(0F, 1.6F, 0.8F, 0F, 90F, 25F, 0.68F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(0F, 1.6F, 0.8F, 0F, -90F, -25F, 0.68F));
        DEFAULT_HANDHELD_ROD = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // DEFAULT_TRIDENT
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(4F, 4F, 2F, 0F, 0F, 0F, 0.5F));
        map.put(ItemDisplayContext.FIXED, create(0F, 0F, 0F, 0F, 180F, 0F, 1F));
        map.put(ItemDisplayContext.GUI, create(0F, 0F, 0F, 0F, 0F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(11F, 17F, -2F, 0F, 60F, 0F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(-11F, 17F, 4F, 0F, 60F, 0F, 1F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(-3F, 17F, 1F, 0F, -90F, 25F, 1F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(-15F, 17F, 1F, 0F, 90F, -25F, 1F));
        DEFAULT_TRIDENT = new PerspectiveModelState(ImmutableMap.copyOf(map));

        // DEFAULT_THROWING_TRIDENT
        map = new HashMap<>();
        map.put(ItemDisplayContext.GROUND, create(4F, 4F, 2F, 0F, 0F, 0F, 0.25F));
        map.put(ItemDisplayContext.FIXED, create(-2F, 4F, -5F, 0F, 180F, 0F, 0.5F));
        map.put(ItemDisplayContext.GUI, create(2F, 3F, 0F, 15F, -25F, -5F, 0.65F));
        map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, create(8F, -17F, 9F, 0F, 90F, 180F, 1F));
        map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, create(-8F, -17F, -7F, 0F, 90F, 180F, 1F));
        map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, create(-3F, 17F, 1F, 0F, -90F, 25F, 1F));
        map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, create(-13F, 17F, 1F, 0F, 90F, -25F, 1F));
        DEFAULT_THROWING_TRIDENT = new PerspectiveModelState(ImmutableMap.copyOf(map));
    }

    public static Transformation create(float tx, float ty, float tz, float rx, float ry, float rz, float s) {
        return create(new Vector3f(tx / 16, ty / 16, tz / 16), new Vector3f(rx, ry, rz), new Vector3f(s, s, s));
    }

    public static Transformation create(Vector3f transform, Vector3f rotation, Vector3f scale) {
        return new Transformation(
                transform,
                new Quaternionf().rotationXYZ(rotation.x() * TORAD, rotation.y() * TORAD, rotation.z() * TORAD),
                scale,
                null
        );
    }

    public static Transformation create(ItemTransform transform) {
        if (ItemTransform.NO_TRANSFORM.equals(transform)) return Transformation.identity();
        return create(transform.translation, transform.rotation, transform.scale);
    }

    public static Transformation flipLeft(Transformation transform) {
        return flipX.compose(transform).compose(flipX);
    }

    public static PerspectiveModelState stateFromItemTransforms(ItemTransforms itemTransforms) {
        if (itemTransforms == ItemTransforms.NO_TRANSFORMS) return IDENTITY;
        ImmutableMap.Builder<ItemDisplayContext, Transformation> map = ImmutableMap.builder();
        for (ItemDisplayContext value : ItemDisplayContext.values()) {
            map.put(value, create(itemTransforms.getTransform(value)));
        }
        return new PerspectiveModelState(map.build());
    }

    public static void applyLeftyFlip(PoseStack pStack) {
        if (!pStack.clear()) {
            Matrix4f tMat = pStack.last().pose();
            Matrix3f nMat = pStack.last().normal();
            tMat.mulLocal(flipX.getMatrix());
            tMat.mul(flipX.getMatrix());
            nMat.mulLocal(flipX.getNormalMatrix());
            nMat.mul(flipX.getNormalMatrix());
            pStack.last().pose().mul(tMat);
            pStack.last().normal().mul(nMat);
        }
    }
}