package dev.dubhe.anvilcraft.event;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.TagIngredient;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ServerEventListener {
    @SubscribeEvent
    public void onServerStarted(@NotNull ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerEventListener.processRecipes(server);
    }

    @SubscribeEvent
    public void onDataPackReloaded(@NotNull ServerEndDataPackReloadEvent event) {
        MinecraftServer server = event.getServer();
        ServerEventListener.processRecipes(server);
    }

    public static void processRecipes(@NotNull MinecraftServer server) {
        Map<ResourceLocation, Recipe<?>> newRecipes = new HashMap<>();
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipeMap = new HashMap<>();
        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : server.getRecipeManager().recipes.entrySet()) {
            RecipeType<?> type = entry.getKey();
            Map<ResourceLocation, Recipe<?>> recipeMap = new HashMap<>();
            for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : entry.getValue().entrySet()) {
                ResourceLocation id = recipeEntry.getKey();
                Recipe<?> recipe = recipeEntry.getValue();
                recipeMap.put(id, recipe);
                if (type == RecipeType.CRAFTING) {
                    Pair<ResourceLocation, Recipe<?>> newRecipe = ServerEventListener.processRecipes(id, recipe);
                    if (newRecipe != null) newRecipes.put(newRecipe.getFirst(), newRecipe.getSecond());
                }
            }
            newRecipeMap.put(type, recipeMap);
        }
        for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : newRecipes.entrySet()) {
            ResourceLocation location = recipeEntry.getKey();
            Recipe<?> recipe = recipeEntry.getValue();
            RecipeType<?> type = recipe.getType();
            Map<ResourceLocation, Recipe<?>> recipeMap = newRecipeMap.getOrDefault(type, new HashMap<>());
            recipeMap.put(location, recipe);
            newRecipeMap.putIfAbsent(type, recipeMap);
        }
        newRecipeMap.replaceAll((type, resourceLocationRecipeMap) -> ImmutableMap.copyOf(resourceLocationRecipeMap));
        newRecipeMap = ImmutableMap.copyOf(newRecipeMap);
        server.getRecipeManager().recipes = newRecipeMap;
    }

    public static @Nullable Pair<ResourceLocation, Recipe<?>> processRecipes(ResourceLocation id, @NotNull Recipe<?> oldRecipe) {
        if (oldRecipe instanceof ShapelessRecipe recipe) {
            if (recipe.getIngredients().size() == 1) {
                ResourceLocation location = AnvilCraft.of("smash/" + id.getPath());
                Ingredient ingredient = recipe.getIngredients().get(0);
                TagIngredient ingredient1 = TagIngredient.of(ingredient);
                ItemStack result = recipe.getResultItem(new RegistryAccess.ImmutableRegistryAccess(List.of()));
                ItemAnvilRecipe recipe1 = new ItemAnvilRecipe(
                        location,
                        NonNullList.withSize(1, ingredient1),
                        ItemAnvilRecipe.Location.UP,
                        NonNullList.withSize(1, Component.of(Blocks.IRON_TRAPDOOR)),
                        List.of(result),
                        ItemAnvilRecipe.Location.UNDER,
                        false
                );
                return new Pair<>(location, recipe1);
            }
        }
        if (oldRecipe instanceof ShapedRecipe recipe) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (isIngredientsSame(ingredients)) {
                if (recipe.getHeight() != recipe.getWidth()) return null;
                if (recipe.getIngredients().size() != recipe.getWidth() * recipe.getHeight()) return null;
                ResourceLocation location = AnvilCraft.of("compress/" + id.getPath());
                Ingredient ingredient = recipe.getIngredients().get(0);
                TagIngredient ingredient1 = TagIngredient.of(ingredient);
                int ingredientCount = recipe.getIngredients().size();
                ItemStack result = recipe.getResultItem(new RegistryAccess.ImmutableRegistryAccess(List.of()));
                ItemAnvilRecipe recipe1 = new ItemAnvilRecipe(
                        location,
                        NonNullList.withSize(ingredientCount, ingredient1),
                        ItemAnvilRecipe.Location.IN,
                        NonNullList.withSize(1, Component.of(Blocks.IRON_TRAPDOOR)),
                        List.of(result),
                        ItemAnvilRecipe.Location.IN,
                        false
                );
                return new Pair<>(location, recipe1);
            }
        }
        return null;
    }

    public static boolean isIngredientsSame(@NotNull List<Ingredient> ingredients) {
        Ingredient ingredient = ingredients.get(0);
        for (Ingredient ingredient1 : ingredients) {
            if (ingredient1.equals(ingredient)) continue;
            return false;
        }
        return true;
    }
}