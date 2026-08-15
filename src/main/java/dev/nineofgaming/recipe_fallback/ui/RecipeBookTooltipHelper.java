package dev.nineofgaming.recipe_fallback.ui;

import dev.nineofgaming.recipe_fallback.FallbackIndicator;
import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.mixins.ClientRecipeBookAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.2 {
import net.minecraft.locale.Language;
//?} else {
/*import net.minecraft.client.resources.language.I18n;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public final class RecipeBookTooltipHelper {
    private static final int maxAlternativeNames = 3;
    private static final Pattern tagNameSeparators = Pattern.compile("[/._-]+");
    private static final Pattern ingredientAliasKeySeparators = Pattern.compile("[^a-z0-9_.-]+");
    private static final Map<RecipeDisplayId, List<Component>> ingredientTooltipCache = new ConcurrentHashMap<>();
    private static final Map<Identifier, Set<Identifier>> tagContentsCache = new ConcurrentHashMap<>();
    private static final Map<Identifier, Set<Identifier>> explicitAliasContents = Map.of(
            vanillaId("iron_tools"),
            vanillaIds(
                    "iron_pickaxe",
                    "iron_shovel",
                    "iron_axe",
                    "iron_hoe",
                    "iron_sword",
                    "iron_spear",
                    "iron_helmet",
                    "iron_chestplate",
                    "iron_leggings",
                    "iron_boots",
                    "iron_horse_armor",
                    "iron_nautilus_armor",
                    "chainmail_helmet",
                    "chainmail_chestplate",
                    "chainmail_leggings",
                    "chainmail_boots"
            ),
            vanillaId("gold_tools"),
            vanillaIds(
                    "golden_pickaxe",
                    "golden_shovel",
                    "golden_axe",
                    "golden_hoe",
                    "golden_sword",
                    "golden_spear",
                    "golden_helmet",
                    "golden_chestplate",
                    "golden_leggings",
                    "golden_boots",
                    "golden_horse_armor",
                    "golden_nautilus_armor"
            ),
            vanillaId("copper_tools"),
            vanillaIds(
                    "copper_pickaxe",
                    "copper_shovel",
                    "copper_axe",
                    "copper_hoe",
                    "copper_sword",
                    "copper_spear",
                    "copper_helmet",
                    "copper_chestplate",
                    "copper_leggings",
                    "copper_boots",
                    "copper_horse_armor",
                    "copper_nautilus_armor"
            )
    );

    private RecipeBookTooltipHelper() {
    }

    public static void clear() {
        ingredientTooltipCache.clear();
        tagContentsCache.clear();
    }

    public static List<Component> appendRecipeDetails(Minecraft minecraft, RecipeDisplayId displayId, List<Component> tooltip) {
        List<Component> updatedTooltip = tooltip;
        if (RecipeFallbackConfig.shouldShowIngredientsInTooltip()) {
            List<Component> ingredientLines = ingredientTooltipLines(minecraft, displayId);
            if (!ingredientLines.isEmpty()) {
                updatedTooltip = new ArrayList<>(tooltip.size() + ingredientLines.size());
                updatedTooltip.addAll(tooltip);
                updatedTooltip.addAll(ingredientLines);
            }
        }

        return FallbackIndicator.appendTooltip(displayId, updatedTooltip);
    }

    public static List<Component> buildRecipeTooltip(Minecraft minecraft, RecipeDisplayId displayId) {
        RecipeDisplayEntry displayEntry = recipeEntry(minecraft, displayId);
        if (displayEntry == null) {
            return FallbackIndicator.appendTooltip(displayId, List.of());
        }

        ItemStack resultStack = resultStack(minecraft, displayEntry);
        List<Component> baseTooltip = resultStack.isEmpty()
                ? List.of()
                : Screen.getTooltipFromItem(minecraft, resultStack);
        return appendRecipeDetails(minecraft, displayId, baseTooltip);
    }

    private static List<Component> ingredientTooltipLines(Minecraft minecraft, RecipeDisplayId displayId) {
        if (displayId == null || minecraft.level == null) {
            return List.of();
        }

        return ingredientTooltipCache.computeIfAbsent(displayId, id -> buildIngredientTooltipLines(minecraft, id));
    }

    private static List<Component> buildIngredientTooltipLines(Minecraft minecraft, RecipeDisplayId displayId) {
        RecipeDisplayEntry displayEntry = recipeEntry(minecraft, displayId);
        if (displayEntry == null || minecraft.level == null) {
            return List.of();
        }

        ContextMap context = SlotDisplayContext.fromLevel(minecraft.level);
        Map<String, IngredientSummary> ingredients = new LinkedHashMap<>();
        for (SlotDisplay inputDisplay : inputDisplays(displayEntry.display())) {
            IngredientSummary summary = summarize(inputDisplay, context);
            if (summary == null) {
                continue;
            }

            ingredients.merge(
                    summary.key(),
                    summary,
                    (left, right) -> new IngredientSummary(left.key(), left.label(), left.amount() + right.amount())
            );
        }

        if (ingredients.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>(ingredients.size() + 1);
        lines.add(Component.translatable("recipe_fallback.tooltip.ingredients").withStyle(ChatFormatting.GRAY));
        for (IngredientSummary ingredient : ingredients.values()) {
            Component line = Component.literal("- " + ingredient.amount() + "x ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(debugDisplayLabel(ingredient));
            lines.add(line);
        }
        return List.copyOf(lines);
    }

    private static Component debugDisplayLabel(IngredientSummary ingredient) {
        var label = ingredient.label().copy().withStyle(ChatFormatting.GRAY);
        if (!RecipeFallbackConfig.shouldShowIngredientTooltipDebugKeys()) {
            return label;
        }

        String key = ingredient.key();
        if (key == null || key.isBlank()) {
            return label;
        }

        return Component.literal("[" + key + "] ").withStyle(ChatFormatting.DARK_GRAY)
                .append(label);
    }

    private static RecipeDisplayEntry recipeEntry(Minecraft minecraft, RecipeDisplayId displayId) {
        if (minecraft.player == null || displayId == null) {
            return null;
        }

        return ((ClientRecipeBookAccessor) minecraft.player.getRecipeBook()).recipe_fallback$getKnown().get(displayId);
    }

    private static ItemStack resultStack(Minecraft minecraft, RecipeDisplayEntry displayEntry) {
        if (minecraft.level == null) {
            return ItemStack.EMPTY;
        }

        ContextMap context = SlotDisplayContext.fromLevel(minecraft.level);
        List<ItemStack> resultItems = displayEntry.resultItems(context);
        if (!resultItems.isEmpty()) {
            return resultItems.getFirst();
        }

        return displayEntry.display().result().resolveForFirstStack(context);
    }

    private static List<SlotDisplay> inputDisplays(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.ingredients();
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.ingredients();
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            return List.of(furnace.ingredient(), furnace.fuel());
        }
        if (display instanceof StonecutterRecipeDisplay stonecutter) {
            return List.of(stonecutter.input());
        }
        if (display instanceof SmithingRecipeDisplay smithing) {
            return List.of(smithing.template(), smithing.base(), smithing.addition());
        }

        return List.of();
    }

    private static IngredientSummary summarize(SlotDisplay slotDisplay, ContextMap context) {
        switch (slotDisplay) {
            case null -> {
                return null;
            }
            case SlotDisplay.Empty empty -> {
                return null;
            }
            case SlotDisplay.WithRemainder withRemainder -> {
                return summarize(withRemainder.input(), context);
            }
            case SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate template) -> {
                ItemStack stack = template.create();
                if (stack.isEmpty()) {
                    return null;
                }
                return new IngredientSummary(stack.toString(), stack.getHoverName(), stack.getCount());
            }
            case SlotDisplay.ItemSlotDisplay(net.minecraft.core.Holder<net.minecraft.world.item.Item> item) -> {
                ItemStack stack = new ItemStack(item);
                return new IngredientSummary(item.unwrapKey().map(Object::toString).orElse(stack.toString()), stack.getHoverName(), 1);
            }
            case SlotDisplay.TagSlotDisplay(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) ->
                    summarizeTag(tag, slotDisplay, context);
            case SlotDisplay.AnyFuel anyFuel -> {
                return new IngredientSummary("any_fuel", Component.translatable("recipe_fallback.tooltip.any_fuel"), 1);
            }
            case SlotDisplay.Composite(List<SlotDisplay> contents) -> {
            }
            default -> {
            }
        }

        List<ItemStack> uniqueValues = uniqueResolvedStacks(slotDisplay, context);
        if (uniqueValues.isEmpty()) {
            return null;
        }

        String keyPrefix = slotDisplay instanceof SlotDisplay.Composite ? "composite" : "resolved";
        Set<Identifier> itemIds = ingredientItemIdsOrResolved(slotDisplay, uniqueValues);
        IngredientAliasMatch alias = matchIngredientAlias(slotDisplay, itemIds);
        if (alias != null) {
            return new IngredientSummary(alias.key(), alias.label(), 1);
        }

        if (uniqueValues.size() == 1) {
            ItemStack stack = uniqueValues.getFirst();
            return new IngredientSummary(stack.toString(), stack.getHoverName(), Math.max(stack.getCount(), 1));
        }

        List<String> names = uniqueValues.stream()
                .map(stack -> stack.getHoverName().getString())
                .toList();
        return new IngredientSummary(
                itemSetKey(keyPrefix, itemIds),
                Component.literal(joinAlternatives(names)),
                1
        );
    }

    private static IngredientSummary summarizeTag(
            net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag,
            SlotDisplay slotDisplay,
            ContextMap context
    ) {
        Identifier tagId = tag.location();
        String tagKey = "#" + tagId;
        if (hasGenericTagAlias(tagId)) {
            return new IngredientSummary(tagKey, tagLabel(tagId), 1);
        }

        List<ItemStack> uniqueValues = uniqueResolvedStacks(slotDisplay, context);
        if (!uniqueValues.isEmpty() && uniqueValues.size() <= maxAlternativeNames) {
            List<String> names = uniqueValues.stream()
                    .map(stack -> stack.getHoverName().getString())
                    .toList();
            return new IngredientSummary(tagKey, Component.literal(joinAlternatives(names)), 1);
        }

        Component label = tagLabel(tagId);
        if (!uniqueValues.isEmpty() && !hasGenericTagAlias(tagId)) {
            label = Component.translatable("recipe_fallback.tooltip.tag_with_option_count", label, uniqueValues.size());
        }

        return new IngredientSummary(tagKey, label, 1);
    }

    private static IngredientAliasMatch matchIngredientAlias(SlotDisplay slotDisplay, Set<Identifier> itemIds) {
        if (itemIds.isEmpty()) {
            return null;
        }

        IngredientAliasMatch explicitAlias = matchExplicitAlias(itemIds);
        if (explicitAlias != null) {
            return explicitAlias;
        }

        Set<Identifier> commonTags = commonAliasTags(itemIds);
        if (commonTags != null && !commonTags.isEmpty()) {
            List<Identifier> orderedTags = commonTags.stream()
                    .sorted((left, right) -> left.toString().compareTo(right.toString()))
                    .toList();
            for (Identifier tagId : orderedTags) {
                if (matchesTagContents(itemIds, tagContents(tagId))) {
                    return new IngredientAliasMatch("#" + tagId, tagLabel(tagId));
                }
            }
        }

        if (slotDisplay instanceof SlotDisplay.Composite) {
            String compositeKey = itemSetKey("composite", itemIds);
            Component compositeAlias = ingredientAliasLabel(compositeKey);
            if (compositeAlias != null) {
                return new IngredientAliasMatch(compositeKey, compositeAlias);
            }
        }

        String resolvedKey = itemSetKey("resolved", itemIds);
        Component directAlias = ingredientAliasLabel(resolvedKey);
        return directAlias != null ? new IngredientAliasMatch(resolvedKey, directAlias) : null;
    }

    private static IngredientAliasMatch matchExplicitAlias(Set<Identifier> resolvedItemIds) {
        for (Map.Entry<Identifier, Set<Identifier>> entry : explicitAliasContents.entrySet()) {
            if (!resolvedItemIds.equals(entry.getValue())) {
                continue;
            }

            return new IngredientAliasMatch("set:" + entry.getKey(), tagLabel(entry.getKey()));
        }

        return null;
    }

    private static boolean matchesTagContents(Set<Identifier> resolvedItemIds, Set<Identifier> tagItemIds) {
        return resolvedItemIds.equals(tagItemIds)
                || (tagItemIds.size() == resolvedItemIds.size() + 1 && tagItemIds.containsAll(resolvedItemIds));
    }

    private static Set<Identifier> commonAliasTags(Set<Identifier> itemIds) {
        Set<Identifier> commonTags = null;
        for (Identifier itemId : itemIds) {
            net.minecraft.world.item.Item item = itemById(itemId);
            if (item == null) {
                return Set.of();
            }

            Set<Identifier> stackTags = item.builtInRegistryHolder().tags()
                    .map(TagKey::location)
                    .filter(thisTagId -> translationExists(tagAliasTranslationKey(thisTagId)))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (commonTags == null) {
                commonTags = stackTags;
            } else {
                commonTags.retainAll(stackTags);
            }

            if (commonTags.isEmpty()) {
                return Set.of();
            }
        }

        return commonTags == null ? Set.of() : commonTags;
    }

    private static Set<Identifier> tagContents(Identifier tagId) {
        return tagContentsCache.computeIfAbsent(tagId, id -> {
            Set<Identifier> itemIds = new LinkedHashSet<>();
            for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                boolean inTag = stack.typeHolder().tags().anyMatch(tag -> tag.location().equals(id));
                if (inTag) {
                    itemIds.add(BuiltInRegistries.ITEM.getKey(item));
                }
            }

            return Set.copyOf(itemIds);
        });
    }

    private static String itemSetKey(String prefix, List<ItemStack> uniqueValues) {
        return itemSetKey(
                prefix,
                uniqueValues.stream()
                        .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    private static String itemSetKey(String prefix, Set<Identifier> itemIds) {
        return prefix + ":" + itemIds.stream()
                .map(Identifier::toString)
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private static List<ItemStack> uniqueResolvedStacks(SlotDisplay slotDisplay, ContextMap context) {
        List<ItemStack> resolvedStacks = slotDisplay.resolveForStacks(context).stream()
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (resolvedStacks.isEmpty()) {
            return List.of();
        }

        Map<String, ItemStack> uniqueStacks = new LinkedHashMap<>();
        for (ItemStack stack : resolvedStacks) {
            uniqueStacks.putIfAbsent(itemKey(stack), stack);
        }

        return new ArrayList<>(uniqueStacks.values());
    }

    private static Set<Identifier> ingredientItemIdsOrResolved(SlotDisplay slotDisplay, List<ItemStack> uniqueValues) {
        Set<Identifier> itemIds = ingredientItemIds(slotDisplay);
        if (!itemIds.isEmpty()) {
            return itemIds;
        }

        return uniqueValues.stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<Identifier> ingredientItemIds(SlotDisplay slotDisplay) {
        return switch (slotDisplay) {
            case null -> Set.of();
            case SlotDisplay.Empty ignored -> Set.of();
            case SlotDisplay.AnyFuel ignored -> Set.of();
            case SlotDisplay.WithRemainder withRemainder -> ingredientItemIds(withRemainder.input());
            case SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate template) -> {
                ItemStack stack = template.create();
                yield stack.isEmpty() ? Set.of() : Set.of(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            case SlotDisplay.ItemSlotDisplay(net.minecraft.core.Holder<net.minecraft.world.item.Item> item) -> {
                ItemStack stack = new ItemStack(item);
                yield stack.isEmpty() ? Set.of() : Set.of(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
            case SlotDisplay.TagSlotDisplay(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) ->
                    tagContents(tag.location());
            case SlotDisplay.Composite(List<SlotDisplay> contents) -> {
                Set<Identifier> itemIds = new LinkedHashSet<>();
                for (SlotDisplay content : contents) {
                    itemIds.addAll(ingredientItemIds(content));
                }
                yield itemIds;
            }
            default -> Set.of();
        };
    }

    private static Component tagLabel(Identifier tagId) {
        String aliasTranslationKey = tagAliasTranslationKey(tagId);
        if (translationExists(aliasTranslationKey)) {
            return Component.translatable(aliasTranslationKey);
        }

        String translationKey = tagTranslationKey(tagId);
        if (translationExists(translationKey)) {
            return Component.translatable(translationKey);
        }

        return Component.literal(humanizeTagName(tagId));
    }

    private static boolean hasGenericTagAlias(Identifier tagId) {
        return translationExists(tagAliasTranslationKey(tagId));
    }

    private static String tagAliasTranslationKey(Identifier tagId) {
        return "recipe_fallback.tooltip.tag_alias." + tagId.getNamespace() + "." + tagId.getPath().replace('/', '.');
    }

    private static String tagTranslationKey(Identifier tagId) {
        return "recipe_fallback.tag." + tagId.getNamespace() + "." + tagId.getPath().replace('/', '.');
    }

    private static Component ingredientAliasLabel(String ingredientKey) {
        String translationKey = ingredientAliasTranslationKey(ingredientKey);
        return translationExists(translationKey) ? Component.translatable(translationKey) : null;
    }

    private static boolean translationExists(String translationKey) {
        //? if >=26.2 {
        return Language.getInstance().has(translationKey);
        //?} else {
        /*return I18n.exists(translationKey);
        *///?}
    }

    private static String ingredientAliasTranslationKey(String ingredientKey) {
        String normalizedKey = ingredientAliasKeySeparators.matcher(ingredientKey.toLowerCase()).replaceAll(".");
        normalizedKey = normalizedKey.replaceAll("\\.+", ".");
        normalizedKey = normalizedKey.replaceAll("^\\.|\\.$", "");
        return "recipe_fallback.tooltip.ingredient_alias." + normalizedKey;
    }

    private static String humanizeTagName(Identifier tagId) {
        String pathName = humanizeTagPath(tagId.getPath());
        if ("minecraft".equals(tagId.getNamespace())) {
            return pathName;
        }

        return pathName + " (" + tagId.getNamespace() + ")";
    }

    private static String humanizeTagPath(String path) {
        List<String> words = new ArrayList<>();
        for (String word : tagNameSeparators.split(path)) {
            if (word.isEmpty()) {
                continue;
            }

            words.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }

        return words.isEmpty() ? path : String.join(" ", words);
    }

    private static String joinAlternatives(List<String> names) {
        int visibleCount = Math.min(names.size(), maxAlternativeNames);
        List<String> visibleNames = new ArrayList<>(names.subList(0, visibleCount));
        if (names.size() > visibleCount) {
            visibleNames.add(Component.translatable("recipe_fallback.tooltip.more_alternatives", names.size() - visibleCount).getString());
        }
        return String.join(" / ", visibleNames);
    }

    private static String itemKey(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null ? id.toString() : stack.getHoverName().getString();
    }

    private static net.minecraft.world.item.Item itemById(Identifier itemId) {
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            if (itemId.equals(BuiltInRegistries.ITEM.getKey(item))) {
                return item;
            }
        }

        return null;
    }

    private static Set<Identifier> vanillaIds(String... itemIds) {
        return Arrays.stream(itemIds)
                .map(RecipeBookTooltipHelper::vanillaId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Identifier vanillaId(String path) {
        return Objects.requireNonNull(Identifier.tryParse("minecraft:" + path));
    }

    private record IngredientSummary(String key, Component label, int amount) {
    }

    private record IngredientAliasMatch(String key, Component label) {
    }
}
