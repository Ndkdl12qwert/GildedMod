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
package com.example.examplemod;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlackGoldBlastingRecipeSerializer implements RecipeSerializer<BlackGoldBlastingRecipe> {

    @Override
    public @NotNull BlackGoldBlastingRecipe fromJson(@NotNull ResourceLocation recipeId, JsonObject json) {
        // 从 JSON 中读取 ingredient, result, experience, cookingTime
        Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
        ItemStack result = new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(json.get("result").getAsString()))));
        float experience = json.get("experience").getAsFloat();
        int cookingTime = json.get("cookingtime").getAsInt();

        // 这里 category 可以固定为 MISC 或从 JSON 里读
        CookingBookCategory category = CookingBookCategory.MISC;

        return new BlackGoldBlastingRecipe(recipeId, "", category, ingredient, result, experience, cookingTime);
    }

    @Override
    public BlackGoldBlastingRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        ItemStack result = buffer.readItem();
        float experience = buffer.readFloat();
        int cookingTime = buffer.readInt();
        CookingBookCategory category = CookingBookCategory.MISC;
        return new BlackGoldBlastingRecipe(recipeId, "", category, ingredient, result, experience, cookingTime);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, BlackGoldBlastingRecipe recipe) {
        recipe.getIngredients().get(0).toNetwork(buffer);
        buffer.writeItem(recipe.getResultItem(null));
        buffer.writeFloat(recipe.getExperience());
        buffer.writeInt(recipe.getCookingTime());
    }
}