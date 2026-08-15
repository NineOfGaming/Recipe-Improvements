package dev.nineofgaming.recipe_fallback.mixins;

import dev.nineofgaming.recipe_fallback.config.RecipeFallbackConfig;
import dev.nineofgaming.recipe_fallback.state.FallbackDisplayState;
import dev.nineofgaming.recipe_fallback.ui.ModifiedRecipeBookCategory;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookAutoCloseHost;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookCloseHandler;
import dev.nineofgaming.recipe_fallback.ui.RecipeBookScrollHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin<T extends RecipeBookMenu> implements RecipeBookScrollHandler, RecipeBookCloseHandler {
    @Unique
    private static final int recipe_fallback$unshiftedLayoutDelta = 77;

    @Unique
    private static final int recipe_fallback$maxCraftAllPlacementRounds = 256;

    @Unique
    private static final boolean recipe_fallback$rbipLoaded = FabricLoader.getInstance().isModLoaded("rbip");

    @Unique
    private static final String recipe_fallback$rbipPageButtonClass = "dev.zenfyr.rbip.RecipeBookPageButton";

    @Final
    @Shadow
    protected T menu;

    @Shadow
    private ClientRecipeBook book;

    @Shadow
    private int xOffset;

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Final
    @Shadow
    private GhostSlots ghostSlots;

    @Shadow
    private boolean widthTooNarrow;

    @Final
    @Shadow
    private RecipeBookPage recipeBookPage;

    @Final
    @Shadow
    private List<RecipeBookTabButton> tabButtons;

    @Mutable
    @Final
    @Shadow
    private List<RecipeBookComponent.TabInfo> tabInfos;

    @Shadow
    protected Minecraft minecraft;

    @Shadow
    protected abstract void setVisible(boolean visible);

    @Shadow
    public abstract boolean isVisible();

    @Shadow
    private int getXOrigin() {
        throw new AssertionError();
    }

    @Shadow
    private int getYOrigin() {
        throw new AssertionError();
    }

    @Shadow
    private boolean tryPlaceRecipe(RecipeCollection recipeCollection, RecipeDisplayId recipeId, boolean craftAll) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void fillGhostRecipe(RecipeDisplay display);

    @Unique
    private boolean recipe_fallback$pendingInsertAutoClose;

    @Unique
    private int recipe_fallback$pendingInsertAutoCloseTicks;

    @Unique
    private int recipe_fallback$pendingInsertAutoCloseContainerId = -1;

    @Unique
    private List<Integer> recipe_fallback$pendingInsertTrackedSlots = List.of();

    @Unique
    private List<ItemStack> recipe_fallback$pendingInsertSlotStacks = List.of();

    @Unique
    private boolean recipe_fallback$trackInsertAttempt;

    @Unique
    private List<Integer> recipe_fallback$insertAttemptTrackedSlots = List.of();

    @Unique
    private List<ItemStack> recipe_fallback$insertAttemptSlotStacks = List.of();

    @Inject(method = "init", at = @At("TAIL"))
    private void recipe_fallback$closeHiddenVanillaRecipeBook(
            int width,
            int height,
            Minecraft minecraft,
            boolean widthTooNarrow,
            CallbackInfo callbackInfo
    ) {
        if (RecipeFallbackConfig.shouldHideVanillaRecipeBook()
                && this.book.isOpen(this.menu.getRecipeBookType())) {
            this.setVisible(false);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void recipe_fallback$addModifiedRecipeTabInfo(
            T menu,
            List<RecipeBookComponent.TabInfo> tabInfos,
            CallbackInfo callbackInfo
    ) {
        if (!RecipeFallbackConfig.shouldShowModifiedRecipeBookTab()) {
            return;
        }

        ModifiedRecipeBookCategory.fromTabInfos(this.tabInfos).ifPresent(category -> {
            if (this.tabInfos.stream().anyMatch(tabInfo -> tabInfo.category() == category)) {
                return;
            }

            List<RecipeBookComponent.TabInfo> updatedTabInfos = new ArrayList<>(this.tabInfos);
            int insertionIndex = updatedTabInfos.size();
            for (int index = 0; index < updatedTabInfos.size(); index++) {
                if (updatedTabInfos.get(index).category() instanceof SearchRecipeBookCategory) {
                    insertionIndex = index + 1;
                    break;
                }
            }

            updatedTabInfos.add(insertionIndex, category.createTabInfo());
            this.tabInfos = updatedTabInfos;
        });
    }

    @Inject(method = "initVisuals", at = @At("TAIL"))
    private void recipe_fallback$moveRbipPageButtonsNextToCenteredGui(CallbackInfo callbackInfo) {
        if (!recipe_fallback$rbipLoaded
                || !RecipeFallbackConfig.shouldPreventRecipeBookGuiShift()
                || this.widthTooNarrow) {
            return;
        }

        for (Field field : RecipeBookComponent.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    || !field.getType().getName().equals(recipe_fallback$rbipPageButtonClass)) {
                continue;
            }

            try {
                field.setAccessible(true);
                if (field.get((RecipeBookComponent<?>) (Object) this) instanceof AbstractWidget widget) {
                    widget.setPosition(widget.getX() - recipe_fallback$unshiftedLayoutDelta, widget.getY());
                }
            } catch (IllegalAccessException ignored) {
                // RBIP is optional; if its internals change, leave its buttons where RBIP placed them.
            }
        }
    }

    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$disableVanillaRecipeBookVisibility(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (RecipeFallbackConfig.shouldHideVanillaRecipeBook()) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "toggleVisibility", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$disableVanillaRecipeBookToggle(CallbackInfo callbackInfo) {
        if (RecipeFallbackConfig.shouldHideVanillaRecipeBook()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "updateScreenPosition", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$keepRecipeBookScreensCentered(
            int width,
            int imageWidth,
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        if (RecipeFallbackConfig.shouldHideVanillaRecipeBook()
                || RecipeFallbackConfig.shouldPreventRecipeBookGuiShift()) {
            callbackInfo.setReturnValue((width - imageWidth) / 2);
        }
    }

    @Inject(method = "getXOrigin", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$moveRecipeBookNextToCenteredGui(CallbackInfoReturnable<Integer> callbackInfo) {
        if (!RecipeFallbackConfig.shouldPreventRecipeBookGuiShift() || this.widthTooNarrow) {
            return;
        }

        int defaultXOrigin = ((this.width - 147) / 2) - this.xOffset;
        callbackInfo.setReturnValue(defaultXOrigin - recipe_fallback$unshiftedLayoutDelta);
    }

    @ModifyArg(
            method = "updateTabs",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookTabButton;setPosition(II)V"
            ),
            index = 0
    )
    private int recipe_fallback$moveTabsNextToCenteredGui(
            int x
    ) {
        if (!RecipeFallbackConfig.shouldPreventRecipeBookGuiShift() || this.widthTooNarrow) {
            return x;
        }

        return x - recipe_fallback$unshiftedLayoutDelta;
    }

    @Override
    public boolean recipe_fallback$mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!RecipeFallbackConfig.shouldEnableRecipeBookMouseWheelScroll() || !this.isVisible()) {
            return false;
        }

        if (!this.recipe_fallback$isMouseOverBookPanel(mouseX, mouseY)) {
            return false;
        }

        return ((RecipeBookScrollHandler) this.recipeBookPage).recipe_fallback$mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Redirect(
            method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;tryPlaceRecipe(Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection;Lnet/minecraft/world/item/crafting/display/RecipeDisplayId;Z)Z"
            )
    )
    private boolean recipe_fallback$quickCraftRecipeSelection(
            RecipeBookComponent<T> instance,
            RecipeCollection recipeCollection,
            RecipeDisplayId recipeId,
            boolean craftAll,
            MouseButtonEvent mouseButtonEvent,
            boolean ignoreTextInputGuard
    ) {
        if (!RecipeFallbackConfig.shouldEnableQuickCraftShortcuts()
                || mouseButtonEvent.button() != 0
                || !mouseButtonEvent.hasControlDown()) {
            return this.tryPlaceRecipe(recipeCollection, recipeId, craftAll);
        }

        boolean placed = this.tryPlaceRecipe(recipeCollection, recipeId, mouseButtonEvent.hasShiftDown());
        if (placed) {
            this.recipe_fallback$quickMoveCraftingResult();
        }

        return placed;
    }

    @Inject(method = "tryPlaceRecipe", at = @At("HEAD"))
    private void recipe_fallback$captureRecipeBookInsertAttempt(
            RecipeCollection recipeCollection,
            RecipeDisplayId recipeId,
            boolean craftAll,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        this.recipe_fallback$trackInsertAttempt = RecipeFallbackConfig.shouldAutoCloseRecipeBookOnInsert()
                && this.isVisible()
                && recipeCollection.isCraftable(recipeId);
        this.recipe_fallback$insertAttemptTrackedSlots = this.recipe_fallback$trackInsertAttempt
                ? this.recipe_fallback$getTrackedSlotIndices()
                : List.of();
        this.recipe_fallback$insertAttemptSlotStacks = this.recipe_fallback$insertAttemptTrackedSlots.isEmpty()
                ? List.of()
                : this.recipe_fallback$captureTrackedSlotStacks(this.recipe_fallback$insertAttemptTrackedSlots);
    }

    @Inject(method = "tryPlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void recipe_fallback$placeFallbackRecipeLocally(
            RecipeCollection recipeCollection,
            RecipeDisplayId recipeId,
            boolean craftAll,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!FallbackDisplayState.isFallbackDisplay(recipeId)
                || this.book == null
                || this.minecraft == null
                || this.minecraft.level == null) {
            return;
        }

        RecipeDisplayEntry displayEntry = ((ClientRecipeBookAccessor) this.book)
                .recipe_fallback$getKnown()
                .get(recipeId);
        if (displayEntry == null) {
            return;
        }

        List<Integer> trackedSlots = RecipeFallbackConfig.shouldAutoCloseRecipeBookOnInsert() && this.isVisible()
                ? this.recipe_fallback$getTrackedSlotIndices()
                : List.of();
        List<ItemStack> slotStacks = trackedSlots.isEmpty()
                ? List.of()
                : this.recipe_fallback$captureTrackedSlotStacks(trackedSlots);

        RecipeDisplay display = displayEntry.display();
        this.ghostSlots.clear();

        boolean placed = this.recipe_fallback$tryPlaceFallbackDisplay(display, craftAll);
        this.recipe_fallback$trackInsertAttempt = false;
        this.recipe_fallback$insertAttemptTrackedSlots = List.of();
        this.recipe_fallback$insertAttemptSlotStacks = List.of();

        if (placed) {
            this.ghostSlots.clear();
            this.recipe_fallback$queueRecipeBookAutoCloseAfterLocalInsert(trackedSlots, slotStacks);
        } else {
            this.recipe_fallback$clearPendingInsertAutoClose();
            if (!this.recipe_fallback$hasRealInputItemsForFallbackDisplay(display)) {
                this.fillGhostRecipe(display);
            }
        }

        callbackInfo.setReturnValue(true);
    }

    @Inject(method = "tryPlaceRecipe", at = @At("RETURN"))
    private void recipe_fallback$queueRecipeBookAutoCloseAfterInsert(
            RecipeCollection recipeCollection,
            RecipeDisplayId recipeId,
            boolean craftAll,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!callbackInfo.getReturnValue()
                || !this.recipe_fallback$trackInsertAttempt
                || this.recipe_fallback$insertAttemptTrackedSlots.isEmpty()
                || this.recipe_fallback$insertAttemptSlotStacks.isEmpty()
                || this.recipe_fallback$insertAttemptTrackedSlots.size() != this.recipe_fallback$insertAttemptSlotStacks.size()) {
            this.recipe_fallback$trackInsertAttempt = false;
            this.recipe_fallback$insertAttemptTrackedSlots = List.of();
            this.recipe_fallback$insertAttemptSlotStacks = List.of();
            this.recipe_fallback$clearPendingInsertAutoClose();
            return;
        }

        this.recipe_fallback$pendingInsertAutoClose = true;
        this.recipe_fallback$pendingInsertAutoCloseTicks = 40;
        this.recipe_fallback$pendingInsertAutoCloseContainerId = this.menu.containerId;
        this.recipe_fallback$pendingInsertTrackedSlots = this.recipe_fallback$insertAttemptTrackedSlots;
        this.recipe_fallback$pendingInsertSlotStacks = this.recipe_fallback$insertAttemptSlotStacks;
        this.recipe_fallback$trackInsertAttempt = false;
        this.recipe_fallback$insertAttemptTrackedSlots = List.of();
        this.recipe_fallback$insertAttemptSlotStacks = List.of();
    }

    @Inject(method = "tryPlaceRecipe", at = @At("RETURN"))
    private void recipe_fallback$fillLocalGhostRecipeForFallbackDisplays(
            RecipeCollection recipeCollection,
            RecipeDisplayId recipeId,
            boolean craftAll,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!callbackInfo.getReturnValue()
                || recipeCollection.isCraftable(recipeId)
                || !FallbackDisplayState.isFallbackDisplay(recipeId)
                || this.book == null
                || this.minecraft == null
                || this.minecraft.level == null) {
            return;
        }

        RecipeDisplayEntry displayEntry = ((ClientRecipeBookAccessor) this.book)
                .recipe_fallback$getKnown()
                .get(recipeId);
        if (displayEntry != null) {
            this.fillGhostRecipe(displayEntry.display());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void recipe_fallback$autoCloseRecipeBookAfterRealInsert(CallbackInfo callbackInfo) {
        if (!this.recipe_fallback$pendingInsertAutoClose) {
            return;
        }

        if (!this.isVisible()
                || this.minecraft == null
                || this.minecraft.player == null
                || this.minecraft.player.containerMenu != this.menu
                || this.menu.containerId != this.recipe_fallback$pendingInsertAutoCloseContainerId) {
            this.recipe_fallback$clearPendingInsertAutoClose();
            return;
        }

        if (this.recipe_fallback$haveTrackedSlotsChanged(
                this.recipe_fallback$pendingInsertTrackedSlots,
                this.recipe_fallback$pendingInsertSlotStacks
        )) {
            this.recipe_fallback$clearPendingInsertAutoClose();

            //? if >=26.2 {
            Screen screen = Minecraft.getInstance().gui.screen();
            //?} else {
            /*Screen screen = Minecraft.getInstance().screen;
            *///?}
            if (screen instanceof RecipeBookAutoCloseHost autoCloseHost) {
                autoCloseHost.recipe_fallback$autoCloseRecipeBook();
            }
            return;
        }

        this.recipe_fallback$pendingInsertAutoCloseTicks--;
        if (this.recipe_fallback$pendingInsertAutoCloseTicks <= 0) {
            this.recipe_fallback$clearPendingInsertAutoClose();
        }
    }

    @Unique
    private boolean recipe_fallback$isMouseOverBookPanel(double mouseX, double mouseY) {
        int xOrigin = this.getXOrigin();
        int yOrigin = this.getYOrigin();

        return mouseX >= xOrigin
                && mouseX < xOrigin + RecipeBookComponent.IMAGE_WIDTH
                && mouseY >= yOrigin
                && mouseY < yOrigin + RecipeBookComponent.IMAGE_HEIGHT;
    }

    @Unique
    private void recipe_fallback$quickMoveCraftingResult() {
        if (!(this.menu instanceof AbstractCraftingMenu craftingMenu)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        LocalPlayer player = minecraft.player;
        if (gameMode == null || player == null) {
            return;
        }

        Slot resultSlot = craftingMenu.getResultSlot();
        if (resultSlot.index < 0) {
            return;
        }

        gameMode.handleContainerInput(this.menu.containerId, resultSlot.index, 0, ContainerInput.QUICK_MOVE, player);
    }

    @Unique
    private boolean recipe_fallback$tryPlaceFallbackDisplay(RecipeDisplay display, boolean craftAll) {
        if (display instanceof ShapedCraftingRecipeDisplay shapedDisplay
                && this.menu instanceof AbstractCraftingMenu craftingMenu) {
            return this.recipe_fallback$tryPlaceShapedFallbackDisplay(craftingMenu, shapedDisplay, craftAll);
        }

        if (display instanceof ShapelessCraftingRecipeDisplay shapelessDisplay
                && this.menu instanceof AbstractCraftingMenu craftingMenu) {
            return this.recipe_fallback$tryPlaceShapelessFallbackDisplay(craftingMenu, shapelessDisplay, craftAll);
        }

        if (display instanceof FurnaceRecipeDisplay furnaceDisplay
                && this.menu instanceof AbstractFurnaceMenu furnaceMenu) {
            return this.recipe_fallback$tryPlaceFurnaceFallbackDisplay(furnaceMenu, furnaceDisplay, craftAll);
        }

        return false;
    }

    @Unique
    private boolean recipe_fallback$hasRealInputItemsForFallbackDisplay(RecipeDisplay display) {
        if ((display instanceof ShapedCraftingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay)
                && this.menu instanceof AbstractCraftingMenu craftingMenu) {
            for (Slot inputSlot : craftingMenu.getInputGridSlots()) {
                if (inputSlot.hasItem()) {
                    return true;
                }
            }
        }

        return display instanceof FurnaceRecipeDisplay
                && this.menu instanceof AbstractFurnaceMenu
                && (this.menu.getSlot(AbstractFurnaceMenu.INGREDIENT_SLOT).hasItem()
                || this.menu.getSlot(AbstractFurnaceMenu.FUEL_SLOT).hasItem());
    }

    @Unique
    private boolean recipe_fallback$tryPlaceShapedFallbackDisplay(
            AbstractCraftingMenu craftingMenu,
            ShapedCraftingRecipeDisplay display,
            boolean craftAll
    ) {
        List<Slot> inputSlots = craftingMenu.getInputGridSlots();
        Map<Slot, SlotDisplay> targets = new LinkedHashMap<>();
        PlaceRecipeHelper.placeRecipe(
                craftingMenu.getGridWidth(),
                craftingMenu.getGridHeight(),
                display.width(),
                display.height(),
                display.ingredients(),
                (slotDisplay, slotIndex, x, y) -> {
                    if (slotIndex >= 0
                            && slotIndex < inputSlots.size()
                            && !this.recipe_fallback$isEmptyDisplay(slotDisplay)) {
                        targets.put(inputSlots.get(slotIndex), slotDisplay);
                    }
                }
        );

        return this.recipe_fallback$tryPlaceFallbackTargets(targets, craftAll);
    }

    @Unique
    private boolean recipe_fallback$tryPlaceShapelessFallbackDisplay(
            AbstractCraftingMenu craftingMenu,
            ShapelessCraftingRecipeDisplay display,
            boolean craftAll
    ) {
        List<Slot> inputSlots = craftingMenu.getInputGridSlots();
        Map<Slot, SlotDisplay> targets = new LinkedHashMap<>();
        int targetCount = Math.min(inputSlots.size(), display.ingredients().size());
        for (int index = 0; index < targetCount; index++) {
            SlotDisplay slotDisplay = display.ingredients().get(index);
            if (!this.recipe_fallback$isEmptyDisplay(slotDisplay)) {
                targets.put(inputSlots.get(index), slotDisplay);
            }
        }

        return this.recipe_fallback$tryPlaceFallbackTargets(targets, craftAll);
    }

    @Unique
    private boolean recipe_fallback$tryPlaceFurnaceFallbackDisplay(
            AbstractFurnaceMenu furnaceMenu,
            FurnaceRecipeDisplay display,
            boolean craftAll
    ) {
        Map<Slot, SlotDisplay> targets = new LinkedHashMap<>();
        if (!this.recipe_fallback$isEmptyDisplay(display.ingredient())) {
            targets.put(this.menu.getSlot(AbstractFurnaceMenu.INGREDIENT_SLOT), display.ingredient());
        }

        if (!this.recipe_fallback$isEmptyDisplay(display.fuel())) {
            targets.put(this.menu.getSlot(AbstractFurnaceMenu.FUEL_SLOT), display.fuel());
        }

        return this.recipe_fallback$tryPlaceFallbackTargets(targets, craftAll);
    }

    @Unique
    private boolean recipe_fallback$tryPlaceFallbackTargets(Map<Slot, SlotDisplay> targets, boolean craftAll) {
        if (!craftAll) {
            return this.recipe_fallback$tryPlaceFallbackTargetsOnce(targets);
        }

        boolean placedAny = false;
        for (int round = 0; round < recipe_fallback$maxCraftAllPlacementRounds; round++) {
            if (!this.recipe_fallback$tryPlaceFallbackTargetsOnce(targets)) {
                break;
            }

            placedAny = true;
        }

        return placedAny;
    }

    @Unique
    private boolean recipe_fallback$tryPlaceFallbackTargetsOnce(Map<Slot, SlotDisplay> targets) {
        if (targets.isEmpty()
                || this.minecraft == null
                || this.minecraft.level == null
                || this.minecraft.player == null
                || this.minecraft.gameMode == null
                || !this.menu.getCarried().isEmpty()) {
            return false;
        }

        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        List<Slot> targetSlots = new ArrayList<>(targets.keySet());
        Map<Slot, Slot> placements = new LinkedHashMap<>();
        Map<Integer, Integer> reservedSourceCounts = new LinkedHashMap<>();

        for (Map.Entry<Slot, SlotDisplay> target : targets.entrySet()) {
            Slot targetSlot = target.getKey();
            SlotDisplay slotDisplay = target.getValue();
            boolean anyFuel = this.recipe_fallback$isAnyFuelDisplay(slotDisplay);
            List<ItemStack> alternatives = anyFuel
                    ? List.of()
                    : this.recipe_fallback$resolveIngredientStacks(slotDisplay, context);
            if (!anyFuel && alternatives.isEmpty()) {
                return false;
            }

            ItemStack targetStack = targetSlot.getItem();
            List<ItemStack> sourceAlternatives = alternatives;
            boolean sourceAnyFuel = anyFuel;
            if (!targetStack.isEmpty()) {
                if (!this.recipe_fallback$matchesIngredient(targetStack, alternatives, anyFuel)
                        || !targetSlot.mayPlace(targetStack)) {
                    return false;
                }

                if (targetStack.getCount() >= targetSlot.getMaxStackSize(targetStack)) {
                    continue;
                }

                sourceAlternatives = List.of(targetStack.copyWithCount(1));
                sourceAnyFuel = false;
            }

            Slot sourceSlot = this.recipe_fallback$findSourceSlotForIngredient(
                    targetSlot,
                    targetSlots,
                    sourceAlternatives,
                    sourceAnyFuel,
                    reservedSourceCounts
            );
            if (sourceSlot == null) {
                return false;
            }

            reservedSourceCounts.merge(sourceSlot.index, 1, Integer::sum);
            placements.put(targetSlot, sourceSlot);
        }

        return !placements.isEmpty() && this.recipe_fallback$executeFallbackPlacementPlan(placements);
    }

    @Unique
    private Slot recipe_fallback$findSourceSlotForIngredient(
            Slot targetSlot,
            List<Slot> targetSlots,
            List<ItemStack> alternatives,
            boolean anyFuel,
            Map<Integer, Integer> reservedSourceCounts
    ) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return null;
        }

        for (Slot sourceSlot : this.menu.slots) {
            if (targetSlots.contains(sourceSlot)
                    || this.recipe_fallback$isResultSlot(sourceSlot)
                    || !sourceSlot.hasItem()
                    || !sourceSlot.mayPickup(player)) {
                continue;
            }

            ItemStack sourceStack = sourceSlot.getItem();
            int reservedCount = reservedSourceCounts.getOrDefault(sourceSlot.index, 0);
            if (sourceStack.getCount() <= reservedCount) {
                continue;
            }

            ItemStack singleItem = sourceStack.copyWithCount(1);
            if (targetSlot.mayPlace(singleItem)
                    && this.recipe_fallback$matchesIngredient(singleItem, alternatives, anyFuel)) {
                return sourceSlot;
            }
        }

        return null;
    }

    @Unique
    private boolean recipe_fallback$executeFallbackPlacementPlan(Map<Slot, Slot> placements) {
        MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        LocalPlayer player = this.minecraft.player;
        if (gameMode == null || player == null || !this.menu.getCarried().isEmpty()) {
            return false;
        }

        for (Map.Entry<Slot, Slot> placement : placements.entrySet()) {
            Slot targetSlot = placement.getKey();
            Slot sourceSlot = placement.getValue();
            ItemStack targetStackBefore = targetSlot.getItem().copy();
            if ((!targetStackBefore.isEmpty() && targetStackBefore.getCount() >= targetSlot.getMaxStackSize(targetStackBefore))
                    || !sourceSlot.hasItem()
                    || !sourceSlot.mayPickup(player)) {
                return false;
            }

            gameMode.handleContainerInput(this.menu.containerId, sourceSlot.index, 0, ContainerInput.PICKUP, player);
            if (this.menu.getCarried().isEmpty()) {
                return false;
            }

            gameMode.handleContainerInput(this.menu.containerId, targetSlot.index, 1, ContainerInput.PICKUP, player);
            boolean inserted = this.recipe_fallback$didTargetSlotReceiveItem(targetSlot, targetStackBefore);
            if (!this.menu.getCarried().isEmpty()) {
                gameMode.handleContainerInput(this.menu.containerId, sourceSlot.index, 0, ContainerInput.PICKUP, player);
            }

            if (!inserted || !this.menu.getCarried().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Unique
    private boolean recipe_fallback$didTargetSlotReceiveItem(Slot targetSlot, ItemStack targetStackBefore) {
        ItemStack targetStackAfter = targetSlot.getItem();
        if (targetStackBefore.isEmpty()) {
            return !targetStackAfter.isEmpty();
        }

        return ItemStack.isSameItemSameComponents(targetStackBefore, targetStackAfter)
                && targetStackAfter.getCount() > targetStackBefore.getCount();
    }

    @Unique
    private List<ItemStack> recipe_fallback$resolveIngredientStacks(SlotDisplay slotDisplay, ContextMap context) {
        if (slotDisplay instanceof SlotDisplay.WithRemainder withRemainder) {
            return this.recipe_fallback$resolveIngredientStacks(withRemainder.input(), context);
        }

        return slotDisplay.resolveForStacks(context).stream()
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    @Unique
    private boolean recipe_fallback$matchesIngredient(
            ItemStack stack,
            List<ItemStack> alternatives,
            boolean anyFuel
    ) {
        if (anyFuel) {
            return true;
        }

        for (ItemStack alternative : alternatives) {
            if (ItemStack.isSameItemSameComponents(stack, alternative)
                    || (alternative.getComponentsPatch().isEmpty() && ItemStack.isSameItem(stack, alternative))) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean recipe_fallback$isEmptyDisplay(SlotDisplay slotDisplay) {
        return slotDisplay instanceof SlotDisplay.Empty;
    }

    @Unique
    private boolean recipe_fallback$isAnyFuelDisplay(SlotDisplay slotDisplay) {
        return switch (slotDisplay) {
            case SlotDisplay.AnyFuel ignored -> true;
            case SlotDisplay.WithRemainder withRemainder -> this.recipe_fallback$isAnyFuelDisplay(withRemainder.input());
            default -> false;
        };
    }

    @Unique
    private boolean recipe_fallback$isResultSlot(Slot slot) {
        if (this.menu instanceof AbstractCraftingMenu craftingMenu && slot == craftingMenu.getResultSlot()) {
            return true;
        }

        return this.menu instanceof AbstractFurnaceMenu furnaceMenu && slot == furnaceMenu.getResultSlot();
    }

    @Unique
    private void recipe_fallback$queueRecipeBookAutoCloseAfterLocalInsert(
            List<Integer> trackedSlots,
            List<ItemStack> slotStacks
    ) {
        if (trackedSlots.isEmpty()
                || slotStacks.isEmpty()
                || trackedSlots.size() != slotStacks.size()) {
            this.recipe_fallback$clearPendingInsertAutoClose();
            return;
        }

        this.recipe_fallback$pendingInsertAutoClose = true;
        this.recipe_fallback$pendingInsertAutoCloseTicks = 40;
        this.recipe_fallback$pendingInsertAutoCloseContainerId = this.menu.containerId;
        this.recipe_fallback$pendingInsertTrackedSlots = trackedSlots;
        this.recipe_fallback$pendingInsertSlotStacks = slotStacks;
    }

    @Override
    public void recipe_fallback$closeForScreenClose() {
        this.recipe_fallback$clearPendingInsertAutoClose();
        if (this.isVisible()) {
            this.setVisible(false);
        }
    }

    @Unique
    private void recipe_fallback$clearPendingInsertAutoClose() {
        this.recipe_fallback$pendingInsertAutoClose = false;
        this.recipe_fallback$pendingInsertAutoCloseTicks = 0;
        this.recipe_fallback$pendingInsertAutoCloseContainerId = -1;
        this.recipe_fallback$pendingInsertTrackedSlots = List.of();
        this.recipe_fallback$pendingInsertSlotStacks = List.of();
        this.recipe_fallback$trackInsertAttempt = false;
        this.recipe_fallback$insertAttemptTrackedSlots = List.of();
        this.recipe_fallback$insertAttemptSlotStacks = List.of();
    }

    @Unique
    private List<ItemStack> recipe_fallback$captureTrackedSlotStacks(List<Integer> trackedSlotIndices) {
        if (trackedSlotIndices.isEmpty()) {
            return List.of();
        }

        List<ItemStack> snapshot = new ArrayList<>(trackedSlotIndices.size());
        for (int slotIndex : trackedSlotIndices) {
            if (!this.menu.isValidSlotIndex(slotIndex)) {
                continue;
            }

            snapshot.add(this.menu.getSlot(slotIndex).getItem().copy());
        }

        return snapshot;
    }

    @Unique
    private List<Integer> recipe_fallback$getTrackedSlotIndices() {
        if (this.menu instanceof AbstractCraftingMenu craftingMenu) {
            List<Integer> slotIndices = new ArrayList<>(craftingMenu.getInputGridSlots().size() + 1);
            slotIndices.add(craftingMenu.getResultSlot().index);
            for (Slot inputSlot : craftingMenu.getInputGridSlots()) {
                slotIndices.add(inputSlot.index);
            }
            return slotIndices;
        }

        if (this.menu instanceof AbstractFurnaceMenu furnaceMenu) {
            return List.of(
                    AbstractFurnaceMenu.INGREDIENT_SLOT,
                    AbstractFurnaceMenu.FUEL_SLOT,
                    furnaceMenu.getResultSlot().index
            );
        }

        return List.of();
    }

    @Unique
    private boolean recipe_fallback$haveTrackedSlotsChanged(List<Integer> trackedSlots, List<ItemStack> slotSnapshot) {
        if (trackedSlots.size() != slotSnapshot.size()) {
            return true;
        }

        for (int index = 0; index < trackedSlots.size(); index++) {
            int slotIndex = trackedSlots.get(index);
            if (!this.menu.isValidSlotIndex(slotIndex)) {
                return true;
            }

            ItemStack currentStack = this.menu.getSlot(slotIndex).getItem();
            if (!ItemStack.matches(slotSnapshot.get(index), currentStack)) {
                return true;
            }
        }

        return false;
    }
}
