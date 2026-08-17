package dev.nineofgaming.recipe_fallback.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.nineofgaming.recipe_fallback.AlwaysVisibleRecipeSync;
import dev.nineofgaming.recipe_fallback.RecipeFallback;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookTooltipHelper;
import net.minecraft.client.ClientRecipeBook;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RecipeFallbackConfig {
    private static final Logger LOGGER = RecipeFallback.createLogger("Config");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(RecipeFallback.MOD_ID + ".json");

    private static ConfigData config = loadInternal();

    private RecipeFallbackConfig() {
    }

    public static ConfigData get() {
        return config;
    }

    public static void load() {
        config = loadInternal();
    }

    public static void save() {
        config.sanitize();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
            RecipeBookTooltipHelper.clear();
            refreshRecipeBook();
        } catch (IOException exception) {
            LOGGER.error("Failed to save config to {}", CONFIG_PATH, exception);
        }
    }

    public static Screen createScreen(Screen parent) {
        ConfigData defaults = ConfigData.defaults();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("recipe_fallback.config.title"))
                .category(buildFallbackCategory(defaults))
                .category(buildRecipeBookCategory(defaults))
                .category(buildOverridesCategory(defaults))
                .category(buildCompatibilityCategory(defaults))
                .category(buildAdvancedCategory(defaults))
                .save(RecipeFallbackConfig::save)
                .build()
                .generateScreen(parent);
    }

    public static boolean shouldApplyFallbackForCurrentServer(Minecraft minecraft) {
        return config.isFallbackAllowedForCurrentServer(minecraft);
    }

    public static boolean shouldForceShowAllRecipes(Minecraft minecraft) {
        return config.enabled
                && config.forceShowAllRecipes
                && shouldApplyFallbackForCurrentServer(minecraft);
    }

    public static boolean shouldForceShowAllServerRecipes(Minecraft minecraft) {
        return config.enabled
                && config.serverOnlyShowAllRecipes
                && shouldApplyFallbackForCurrentServer(minecraft);
    }

    public static boolean shouldHideVanillaRecipeBook() {
        return config.hideVanillaRecipeBook;
    }

    public static boolean shouldPreventRecipeBookGuiShift() {
        return config.preventRecipeBookGuiShift;
    }

    public static boolean shouldShowFallbackIndicator() {
        return config.fallbackIndicator;
    }

    public static boolean shouldShowIngredientsInTooltip() {
        return config.recipes.showIngredientsInTooltip;
    }

    public static boolean shouldUngroupRecipes() {
        return config.recipes.ungroup;
    }

    public static boolean shouldEnableRecipeBookMouseWheelScroll() {
        return config.navigation.mouseWheelScroll;
    }

    public static boolean shouldEnableQuickCraftShortcuts() {
        return config.crafting.quickCraft;
    }

    public static boolean shouldAutoCloseRecipeBookOnInsert() {
        return config.recipeBook.autoClose;
    }

    public static boolean shouldCloseRecipeBookOnScreenClose() {
        return config.recipeBook.closeOnScreenClose;
    }

    public static boolean shouldDisableRecipeBookAnimations() {
        return config.recipeBook.disableAnimations;
    }

    public static boolean shouldShowRecipeBookConfigButton() {
        return config.recipeBook.showConfigButton;
    }

    public static boolean shouldShowModifiedRecipeBookTab() {
        return config.recipeBook.showModifiedTab;
    }

    public static boolean shouldShowRecipeBookTabTooltips() {
        return config.recipeBook.showTabTooltips;
    }

    public static boolean shouldShowIngredientTooltipDebugKeys() {
        return config.showIngredientTooltipDebugKeys;
    }

    private static ConfigCategory buildFallbackCategory(ConfigData defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("recipe_fallback.config.category.fallback"))
                .group(group(
                        "recipe_fallback.config.group.fallback",
                        booleanOption(
                                "recipe_fallback.config.enabled",
                                defaults.enabled,
                                () -> config.enabled,
                                value -> config.enabled = value,
                                "recipe_fallback.config.enabled.desc.1",
                                "recipe_fallback.config.enabled.desc.2"
                        )
                ))
                .group(group(
                        "recipe_fallback.config.group.feedback",
                        cyclingOption(
                                "recipe_fallback.config.notification_mode",
                                defaults.notificationMode,
                                () -> config.notificationMode,
                                value -> config.notificationMode = value,
                                NotificationMode.values(),
                                mode -> Component.translatable(mode.translationKey()),
                                "recipe_fallback.config.notification_mode.desc.1",
                                "recipe_fallback.config.notification_mode.desc.2"
                        ),
                        booleanOption(
                                "recipe_fallback.config.fallback_indicator",
                                defaults.fallbackIndicator,
                                () -> config.fallbackIndicator,
                                value -> config.fallbackIndicator = value,
                                "recipe_fallback.config.fallback_indicator.desc.1"
                        )
                ))
                .build();
    }

    private static ConfigCategory buildRecipeBookCategory(ConfigData defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("recipe_fallback.config.category.recipe_book"))
                .group(group(
                        "recipe_fallback.config.group.layout",
                        booleanOption(
                                "recipe_fallback.config.hide_vanilla_recipe_book",
                                defaults.hideVanillaRecipeBook,
                                () -> config.hideVanillaRecipeBook,
                                value -> config.hideVanillaRecipeBook = value,
                                "recipe_fallback.config.hide_vanilla_recipe_book.desc.1",
                                "recipe_fallback.config.hide_vanilla_recipe_book.desc.2"
                        ),
                        booleanOption(
                                "recipe_fallback.config.prevent_recipe_book_gui_shift",
                                defaults.preventRecipeBookGuiShift,
                                () -> config.preventRecipeBookGuiShift,
                                value -> config.preventRecipeBookGuiShift = value,
                                "recipe_fallback.config.prevent_recipe_book_gui_shift.desc.1",
                                "recipe_fallback.config.prevent_recipe_book_gui_shift.desc.2",
                                "recipe_fallback.config.prevent_recipe_book_gui_shift.desc.3"
                        ),
                        booleanOption(
                                "recipe_fallback.config.recipe_book.show_config_button",
                                defaults.recipeBook.showConfigButton,
                                () -> config.recipeBook.showConfigButton,
                                value -> config.recipeBook.showConfigButton = value,
                                "recipe_fallback.config.recipe_book.show_config_button.desc.1",
                                "recipe_fallback.config.recipe_book.show_config_button.desc.2"
                        )
                ))
                .group(group(
                        "recipe_fallback.config.group.browsing",
                        booleanOption(
                                "recipe_fallback.config.recipes.show_ingredients_in_tooltip",
                                defaults.recipes.showIngredientsInTooltip,
                                () -> config.recipes.showIngredientsInTooltip,
                                value -> config.recipes.showIngredientsInTooltip = value,
                                "recipe_fallback.config.recipes.show_ingredients_in_tooltip.desc.1"
                        ),
                        booleanOption(
                                "recipe_fallback.config.recipes.ungroup",
                                defaults.recipes.ungroup,
                                () -> config.recipes.ungroup,
                                value -> config.recipes.ungroup = value,
                                "recipe_fallback.config.recipes.ungroup.desc.1"
                        ),
                        booleanOption(
                                "recipe_fallback.config.recipe_book.show_modified_tab",
                                defaults.recipeBook.showModifiedTab,
                                () -> config.recipeBook.showModifiedTab,
                                value -> config.recipeBook.showModifiedTab = value,
                                "recipe_fallback.config.recipe_book.show_modified_tab.desc.1",
                                "recipe_fallback.config.recipe_book.show_modified_tab.desc.2"
                        ),
                        booleanOption(
                                "recipe_fallback.config.recipe_book.show_tab_tooltips",
                                defaults.recipeBook.showTabTooltips,
                                () -> config.recipeBook.showTabTooltips,
                                value -> config.recipeBook.showTabTooltips = value,
                                "recipe_fallback.config.recipe_book.show_tab_tooltips.desc.1"
                        ),
                        booleanOption(
                                "recipe_fallback.config.navigation.mouse_wheel_scroll",
                                defaults.navigation.mouseWheelScroll,
                                () -> config.navigation.mouseWheelScroll,
                                value -> config.navigation.mouseWheelScroll = value,
                                "recipe_fallback.config.navigation.mouse_wheel_scroll.desc.1"
                        ),
                        booleanOption(
                                "recipe_fallback.config.recipe_book.disable_animations",
                                defaults.recipeBook.disableAnimations,
                                () -> config.recipeBook.disableAnimations,
                                value -> config.recipeBook.disableAnimations = value,
                                "recipe_fallback.config.recipe_book.disable_animations.desc.1"
                        )
                ))
                .group(group(
                        "recipe_fallback.config.group.actions",
                        booleanOption(
                                "recipe_fallback.config.recipe_book.close_on_screen_close",
                                defaults.recipeBook.closeOnScreenClose,
                                () -> config.recipeBook.closeOnScreenClose,
                                value -> config.recipeBook.closeOnScreenClose = value,
                                "recipe_fallback.config.recipe_book.close_on_screen_close.desc.1"
                        ),
                        booleanOption(
                                "recipe_fallback.config.recipe_book.auto_close",
                                defaults.recipeBook.autoClose,
                                () -> config.recipeBook.autoClose,
                                value -> config.recipeBook.autoClose = value,
                                "recipe_fallback.config.recipe_book.auto_close.desc.1",
                                "recipe_fallback.config.recipe_book.auto_close.desc.2"
                        ),
                        booleanOption(
                                "recipe_fallback.config.crafting.quick_craft",
                                defaults.crafting.quickCraft,
                                () -> config.crafting.quickCraft,
                                value -> config.crafting.quickCraft = value,
                                "recipe_fallback.config.crafting.quick_craft.desc.1",
                                "recipe_fallback.config.crafting.quick_craft.desc.2",
                                "recipe_fallback.config.crafting.quick_craft.desc.3",
                                "recipe_fallback.config.crafting.quick_craft.desc.4"
                        )
                ))
                .build();
    }

    private static ConfigCategory buildOverridesCategory(ConfigData defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("recipe_fallback.config.category.overrides"))
                .group(OptionGroup.createBuilder()
                        .name(Component.translatable("recipe_fallback.config.group.overrides"))
                        .option(Option.<ServerOverrideMode>createBuilder()
                                .name(Component.translatable("recipe_fallback.config.server_override_mode"))
                                .description(description(
                                        "recipe_fallback.config.server_override_mode.desc.1",
                                        "recipe_fallback.config.server_override_mode.desc.2"
                                ))
                                .binding(
                                        defaults.serverOverrideMode,
                                        () -> config.serverOverrideMode,
                                        value -> config.serverOverrideMode = value
                                )
                                .controller(option -> CyclingListControllerBuilder.<ServerOverrideMode>create(option)
                                        .values(ServerOverrideMode.values())
                                        .formatValue(mode -> Component.translatable(mode.translationKey())))
                                .build())
                        .build())
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("recipe_fallback.config.server_overrides"))
                        .description(description(
                                "recipe_fallback.config.server_overrides.desc.1",
                                "recipe_fallback.config.server_overrides.desc.2",
                                "recipe_fallback.config.server_overrides.desc.3"
                        ))
                        .binding(
                                new ArrayList<>(defaults.serverOverrides),
                                () -> config.serverOverrides,
                                value -> config.serverOverrides = ConfigData.sanitizeServerOverrides(value)
                        )
                        .initial("")
                        .controller(StringControllerBuilder::create)
                        .minimumNumberOfEntries(0)
                        .maximumNumberOfEntries(128)
                        .insertEntriesAtEnd(true)
                        .collapsed(false)
                        .build())
                .build();
    }

    private static ConfigCategory buildCompatibilityCategory(ConfigData defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("recipe_fallback.config.category.compatibility"))
                .group(group(
                        "recipe_fallback.config.group.warnings",
                        booleanOption(
                                "recipe_fallback.config.hide_rei_warning",
                                defaults.hideReiWarning,
                                () -> config.hideReiWarning,
                                value -> config.hideReiWarning = value,
                                "recipe_fallback.config.hide_rei_warning.desc.1"
                        )
                ))
                .group(group(
                        "recipe_fallback.config.group.bridges",
                        booleanOption(
                                "recipe_fallback.config.rei_bridge_enabled",
                                defaults.reiBridgeEnabled,
                                () -> config.reiBridgeEnabled,
                                value -> config.reiBridgeEnabled = value,
                                "recipe_fallback.config.rei_bridge_enabled.desc.1",
                                "recipe_fallback.config.rei_bridge_enabled.desc.2"
                        ),
                        booleanOption(
                                "recipe_fallback.config.jei_bridge_enabled",
                                defaults.jeiBridgeEnabled,
                                () -> config.jeiBridgeEnabled,
                                value -> config.jeiBridgeEnabled = value,
                                "recipe_fallback.config.jei_bridge_enabled.desc.1",
                                "recipe_fallback.config.jei_bridge_enabled.desc.2"
                        )
                ))
                .build();
    }

    private static ConfigCategory buildAdvancedCategory(ConfigData defaults) {
        return ConfigCategory.createBuilder()
                .name(Component.translatable("recipe_fallback.config.category.advanced"))
                .group(group(
                        "recipe_fallback.config.group.data",
                        booleanOption(
                                "recipe_fallback.config.include_modded_recipes",
                                defaults.includeModdedRecipes,
                                () -> config.includeModdedRecipes,
                                value -> config.includeModdedRecipes = value,
                                "recipe_fallback.config.include_modded_recipes.desc.1",
                                "recipe_fallback.config.include_modded_recipes.desc.2",
                                "recipe_fallback.config.include_modded_recipes.desc.3"
                        ),
                        booleanOption(
                                "recipe_fallback.config.force_show_all_recipes",
                                defaults.forceShowAllRecipes,
                                () -> config.forceShowAllRecipes,
                                value -> config.forceShowAllRecipes = value,
                                "recipe_fallback.config.force_show_all_recipes.desc.1",
                                "recipe_fallback.config.force_show_all_recipes.desc.2",
                                "recipe_fallback.config.force_show_all_recipes.desc.3",
                                "recipe_fallback.config.force_show_all_recipes.desc.4"
                        ),
                        booleanOption(
                                "recipe_fallback.config.server_only_show_all_recipes",
                                defaults.serverOnlyShowAllRecipes,
                                () -> config.serverOnlyShowAllRecipes,
                                value -> config.serverOnlyShowAllRecipes = value,
                                "recipe_fallback.config.server_only_show_all_recipes.desc.1",
                                "recipe_fallback.config.server_only_show_all_recipes.desc.2",
                                "recipe_fallback.config.server_only_show_all_recipes.desc.3",
                                "recipe_fallback.config.server_only_show_all_recipes.desc.4"
                        )
                ))
                .group(group(
                        "recipe_fallback.config.group.debug",
                        booleanOption(
                                "recipe_fallback.config.verbose_logging",
                                defaults.verboseLogging,
                                () -> config.verboseLogging,
                                value -> config.verboseLogging = value,
                                "recipe_fallback.config.verbose_logging.desc.1",
                                "recipe_fallback.config.verbose_logging.desc.2"
                        ),
                        booleanOption(
                                "recipe_fallback.config.show_ingredient_tooltip_debug_keys",
                                defaults.showIngredientTooltipDebugKeys,
                                () -> config.showIngredientTooltipDebugKeys,
                                value -> config.showIngredientTooltipDebugKeys = value,
                                "recipe_fallback.config.show_ingredient_tooltip_debug_keys.desc.1",
                                "recipe_fallback.config.show_ingredient_tooltip_debug_keys.desc.2"
                        )
                ))
                .build();
    }

    private static OptionGroup group(String nameKey, Option<?>... options) {
        OptionGroup.Builder builder = OptionGroup.createBuilder()
                .name(Component.translatable(nameKey));

        for (Option<?> option : options) {
            builder.option(option);
        }

        return builder.build();
    }

    private static Option<Boolean> booleanOption(
            String nameKey,
            boolean defaultValue,
            Supplier<Boolean> getter,
            Consumer<Boolean> setter,
            String... descriptionKeys
    ) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable(nameKey))
                .description(description(descriptionKeys))
                .binding(defaultValue, getter, setter)
                .controller(option -> BooleanControllerBuilder.create(option).yesNoFormatter())
                .build();
    }

    private static <T> Option<T> cyclingOption(
            String nameKey,
            T defaultValue,
            Supplier<T> getter,
            Consumer<T> setter,
            T[] values,
            Function<T, Component> formatter,
            String... descriptionKeys
    ) {
        return Option.<T>createBuilder()
                .name(Component.translatable(nameKey))
                .description(description(descriptionKeys))
                .binding(defaultValue, getter, setter)
                .controller(option -> CyclingListControllerBuilder.<T>create(option)
                        .values(values)
                        .formatValue(formatter::apply))
                .build();
    }

    private static OptionDescription description(String... keys) {
        if (keys.length == 0) {
            return OptionDescription.of();
        }

        Component[] components = new Component[(keys.length * 2) - 1];
        int componentIndex = 0;
        for (int index = 0; index < keys.length; index++) {
            if (index > 0) {
                components[componentIndex++] = Component.empty();
            }
            components[componentIndex++] = Component.translatable(keys[index]);
        }

        return OptionDescription.of(components);
    }

    private static ConfigData loadInternal() {
        if (!Files.exists(CONFIG_PATH)) {
            return ConfigData.defaults();
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded == null) {
                return ConfigData.defaults();
            }

            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load config from {}", CONFIG_PATH, exception);
            return ConfigData.defaults();
        }
    }

    private static void refreshRecipeBook() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ClientRecipeBook recipeBook = minecraft.player.getRecipeBook();
        recipeBook.rebuildCollections();

        if (minecraft.getConnection() != null) {
            minecraft.getConnection().searchTrees().updateRecipes(recipeBook, minecraft.level);
        }

        //? if >=26.2 {
        Screen screen = minecraft.gui.screen();
        //?} else {
        /*Screen screen = minecraft.screen;
        *///?}
        if (screen instanceof RecipeUpdateListener recipeUpdateListener) {
            recipeUpdateListener.recipesUpdated();
        }

        AlwaysVisibleRecipeSync.refresh(minecraft);
    }

    public static final class ConfigData {
        public boolean enabled = true;
        public ServerOverrideMode serverOverrideMode = ServerOverrideMode.BLACKLIST;
        public List<String> serverOverrides = new ArrayList<>();
        public NotificationMode notificationMode = NotificationMode.TOAST;
        public boolean hideReiWarning = true;
        public boolean verboseLogging = false;
        public boolean showIngredientTooltipDebugKeys = false;
        public boolean fallbackIndicator = true;
        public boolean reiBridgeEnabled = true;
        public boolean jeiBridgeEnabled = true;
        public boolean hideVanillaRecipeBook = false;
        public boolean preventRecipeBookGuiShift = true;
        public boolean includeModdedRecipes = true;
        public boolean forceShowAllRecipes = false;
        public boolean serverOnlyShowAllRecipes = false;
        public RecipesConfig recipes = new RecipesConfig();
        public CraftingConfig crafting = new CraftingConfig();
        public RecipeBookConfig recipeBook = new RecipeBookConfig();
        public NavigationConfig navigation = new NavigationConfig();

        public static ConfigData defaults() {
            ConfigData config = new ConfigData();
            config.sanitize();
            return config;
        }

        public boolean isFallbackAllowedForCurrentServer(Minecraft minecraft) {
            if (!this.enabled) {
                return false;
            }

            boolean currentServerListed = this.isCurrentServerListed(minecraft);
            return switch (this.serverOverrideMode) {
                case BLACKLIST -> !currentServerListed;
                case WHITELIST -> currentServerListed;
            };
        }

        public void sanitize() {
            if (this.serverOverrideMode == null) {
                this.serverOverrideMode = ServerOverrideMode.BLACKLIST;
            }
            if (this.notificationMode == null) {
                this.notificationMode = NotificationMode.TOAST;
            }
            if (this.recipes == null) {
                this.recipes = new RecipesConfig();
            }
            if (this.crafting == null) {
                this.crafting = new CraftingConfig();
            }
            if (this.recipeBook == null) {
                this.recipeBook = new RecipeBookConfig();
            }
            if (this.navigation == null) {
                this.navigation = new NavigationConfig();
            }
            this.serverOverrides = sanitizeServerOverrides(this.serverOverrides);
        }

        private boolean isCurrentServerListed(Minecraft minecraft) {
            if (minecraft == null) {
                return false;
            }

            Set<String> candidates = new LinkedHashSet<>();

            //? if >=26.2 {
            if (minecraft.hasSingleplayerServer() || minecraft.isLocalServer()) {
            //?} else {
            /*if (minecraft.isSingleplayer() || minecraft.isLocalServer()) {
            *///?}
                candidates.add("singleplayer");
            }

            ServerData currentServer = minecraft.getCurrentServer();
            if (currentServer != null) {
                if (currentServer.isLan()) {
                    candidates.add("lan");
                }
                if (currentServer.isRealm()) {
                    candidates.add("realm");
                }
                addServerCandidates(candidates, currentServer.ip);
            }

            for (String serverOverride : this.serverOverrides) {
                if (candidates.contains(serverOverride)) {
                    return true;
                }
            }

            return false;
        }

        private static void addServerCandidates(Set<String> candidates, String value) {
            String normalized = normalizeServerValue(value);
            if (normalized.isEmpty()) {
                return;
            }

            candidates.add(normalized);

            int firstColon = normalized.indexOf(':');
            int lastColon = normalized.lastIndexOf(':');
            if (firstColon == -1) {
                candidates.add(normalized + ":25565");
                return;
            }

            if (firstColon == lastColon) {
                String host = normalized.substring(0, firstColon);
                String port = normalized.substring(firstColon + 1);
                if ("25565".equals(port)) {
                    candidates.add(host);
                }
            }
        }

        private static List<String> sanitizeServerOverrides(List<String> values) {
            if (values == null || values.isEmpty()) {
                return new ArrayList<>();
            }

            LinkedHashSet<String> sanitized = new LinkedHashSet<>();
            for (String value : values) {
                String normalized = normalizeServerValue(value);
                if (!normalized.isEmpty()) {
                    sanitized.add(normalized);
                }
            }

            return new ArrayList<>(sanitized);
        }

        private static String normalizeServerValue(String value) {
            if (value == null) {
                return "";
            }

            return value.trim().toLowerCase(Locale.ROOT);
        }
    }

    public static final class RecipesConfig {
        public boolean showIngredientsInTooltip = true;
        public boolean ungroup = false;
    }

    public static final class CraftingConfig {
        public boolean quickCraft = false;
    }

    public static final class RecipeBookConfig {
        public boolean autoClose = false;
        public boolean closeOnScreenClose = false;
        public boolean disableAnimations = false;
        public boolean showConfigButton = true;
        public boolean showModifiedTab = true;
        public boolean showTabTooltips = true;
    }

    public static final class NavigationConfig {
        public boolean mouseWheelScroll = true;
    }

    public enum NotificationMode {
        OFF("recipe_fallback.config.notification_mode.off"),
        TOAST("recipe_fallback.config.notification_mode.toast"),
        CHAT("recipe_fallback.config.notification_mode.chat"),
        LOG_ONLY("recipe_fallback.config.notification_mode.log_only");

        private final String translationKey;

        NotificationMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    public enum ServerOverrideMode {
        BLACKLIST("recipe_fallback.config.server_override_mode.blacklist"),
        WHITELIST("recipe_fallback.config.server_override_mode.whitelist");

        private final String translationKey;

        ServerOverrideMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }
}
