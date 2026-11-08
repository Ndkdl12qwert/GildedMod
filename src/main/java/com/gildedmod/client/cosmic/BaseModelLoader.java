package com.gildedmod.client.cosmic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseModelLoader<T extends IUnbakedGeometry<T>> implements IGeometryLoader<T> {

    public JsonObject clear(JsonObject modelContents, String... types) {
        final JsonObject clean = modelContents.deepCopy();
        clean.remove("loader");
        for (String type : types) {
            clean.remove(type);
        }
        return clean;
    }

    public List<ResourceLocation> getMasks(JsonObject modelContents, String type) {
        final JsonObject cosmic = modelContents.getAsJsonObject(type);
        if (cosmic == null) {
            throw new IllegalStateException("Missing " + type + " object.");
        }
        List<ResourceLocation> maskTexture = new ArrayList<>();
        if (cosmic.has("mask") && cosmic.get("mask").isJsonArray()) {
            JsonArray masks = cosmic.getAsJsonArray("mask");
            for (JsonElement mask : masks) {
                maskTexture.add(new ResourceLocation(mask.getAsString()));
            }
        } else {
            maskTexture.add(new ResourceLocation(GsonHelper.getAsString(cosmic, "mask")));
        }
        return maskTexture;
    }
}