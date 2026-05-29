package com.y271727uy.sellingbin.block.entity;

import com.y271727uy.sellingbin.api.economic.ShopcoreCurrency;
import com.y271727uy.sellingbin.all.ModBlockEntities;
import com.y271727uy.sellingbin.all.ModMenus;
import com.y271727uy.sellingbin.all.ModRecipes;
import com.y271727uy.sellingbin.client.menu.SellingBinMenu;
import com.y271727uy.sellingbin.economic.CurrencyDenomination;
import com.y271727uy.sellingbin.economic.CurrencyOperationResult;
import com.y271727uy.sellingbin.economic.Tax;
import com.y271727uy.sellingbin.event.SellingBinEvents;
import com.y271727uy.sellingbin.gameplay.sellingbin.SellingBinGroupManager;
import com.y271727uy.sellingbin.recipe.SellingBinRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@net.minecraft.MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@SuppressWarnings({"unused", "all"})
public class SellingBinBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INTERVAL_TICKS = 10 * 60 * 20;
    private static final float LID_ANIMATION_STEP = 0.1F;
    private static final java.util.Set<SellingBinBlockEntity> LOADED_INSTANCES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? ticksUntilRun : INTERVAL_TICKS;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                ticksUntilRun = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!suppressInventorySync && level != null && !level.isClientSide) {
                syncClientState(worldPosition, getBlockState());
            }
        }
    };

    private final LazyOptional<IItemHandler> handlerOptional = LazyOptional.of(() -> itemHandler);
    private int ticksUntilRun = INTERVAL_TICKS;
    private boolean lidTargetOpen;
    private float lastLidOpenProgress;
    private float lidOpenProgress;
    private boolean suppressInventorySync;
    @Nullable private UUID boundPlayerUuid;
    @Nullable private String boundPlayerName;
    private boolean boundTaxExempt;
    private boolean transactionNotificationEnabled;

    public SellingBinBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SELLING_BIN.get(), pos, state);
    }

    public SellingBinBlockEntity(BlockEntityType<? extends SellingBinBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            LOADED_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED_INSTANCES.remove(this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerOptional.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? handlerOptional.cast() : super.getCapability(cap, side);
    }

    public static java.util.Collection<SellingBinBlockEntity> getLoadedInstances() {
        return List.copyOf(LOADED_INSTANCES);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public boolean isBound() {
        return boundPlayerUuid != null;
    }

    public boolean isBoundTo(Player player) {
        return player != null && player.getUUID().equals(boundPlayerUuid);
    }

    public boolean isBoundTo(UUID playerUuid) {
        return playerUuid != null && playerUuid.equals(boundPlayerUuid);
    }

    public boolean isTaxExempt() {
        return boundTaxExempt;
    }

    public boolean toggleTransactionNotification() {
        transactionNotificationEnabled = !transactionNotificationEnabled;
        syncClientState(worldPosition, getBlockState());
        return transactionNotificationEnabled;
    }

    @Nullable
    public UUID getBoundPlayerUuid() {
        return boundPlayerUuid;
    }

    @Nullable
    public String getBoundPlayerName() {
        return boundPlayerName;
    }

    public boolean bindTo(Player player, boolean taxExempt) {
        if (player == null) {
            return false;
        }

        boundPlayerUuid = player.getUUID();
        boundPlayerName = player.getScoreboardName();
        boundTaxExempt = taxExempt;
        syncClientState(worldPosition, getBlockState());
        return true;
    }

    public boolean unbind(Player player) {
        if (!isBoundTo(player)) {
            return false;
        }

        boundPlayerUuid = null;
        boundPlayerName = null;
        boundTaxExempt = false;
        syncClientState(worldPosition, getBlockState());
        return true;
    }

    public void setLidTargetOpen(boolean open) {
        if (this.lidTargetOpen == open) {
            return;
        }

        this.lidTargetOpen = open;
        playLidSound(open);
        syncClientState(worldPosition, getBlockState());
    }

    private void playLidSound(boolean open) {
        if (level == null || level.isClientSide) {
            return;
        }

        level.playSound(
                null,
                worldPosition,
                open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                SoundSource.BLOCKS,
                0.5F,
                level.random.nextFloat() * 0.1F + 0.9F
        );
    }

    private void syncClientState(BlockPos pos, BlockState state) {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public float getLidOpenProgress(float partialTick) {
        float targetProgress = lidTargetOpen ? 1.0F : 0.0F;
        if (lidOpenProgress == targetProgress) {
            return targetProgress;
        }
        if (partialTick == 1) {
            return lidOpenProgress;
        }
        float original = Mth.lerp(partialTick, lastLidOpenProgress, lidOpenProgress);

        return original < 0.5f
                ? 4 * original * original * original
                : (float) (1 - Math.pow(-2 * original + 2, 3) / 2);
    }

    public static void runAllRecipesBroadcast(Level level, SellingBinBlockEntity blockEntity) {
        if (level == null || level.isClientSide) {
            return;
        }

        blockEntity.runAllRecipes(level);
        blockEntity.setChanged();
        blockEntity.syncClientState(blockEntity.worldPosition, blockEntity.getBlockState());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SellingBinBlockEntity blockEntity) {
        blockEntity.tick(level, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        float targetProgress = lidTargetOpen ? 1.0F : 0.0F;
        this.lastLidOpenProgress = lidOpenProgress;
        if (lidOpenProgress != targetProgress) {
            if (lidOpenProgress < targetProgress) {
                lidOpenProgress = Math.min(targetProgress, lidOpenProgress + LID_ANIMATION_STEP);
            } else {
                lidOpenProgress = Math.max(targetProgress, lidOpenProgress - LID_ANIMATION_STEP);
            }
        }
        if (level.isClientSide) {
            return;
        }
        if (--ticksUntilRun > 0) {
            setChanged();
            return;
        }

        ticksUntilRun = INTERVAL_TICKS;
        runAllRecipesBroadcast(level, this);
        setChanged();
    }

    public void dropInventory(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
                this.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        this.setChanged();
    }

    public static void dropInventory(Level level, BlockPos pos, SellingBinBlockEntity blockEntity) {
        blockEntity.dropInventory(level, pos);
    }

    private void runAllRecipes(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        int slots = itemHandler.getSlots();
        record Planned(int slot, SellingBinRecipe recipe, int sellCount) {}
        List<Planned> planned = new ArrayList<>();
        boolean marketChanged = false;

        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            var wrapper = new SellingBinRecipe.RecipeInput(List.of(stack));
            Optional<SellingBinRecipe> recipeOpt = level.getRecipeManager().getRecipeFor(ModRecipes.SELLING_BIN_RECIPE_TYPE.get(), wrapper, level);
            if (recipeOpt.isEmpty()) {
                continue;
            }

            SellingBinRecipe recipe = recipeOpt.get();
            int inputCount = Math.max(1, recipe.getInputCount());
            int sellCount = stack.getCount() / inputCount;
            if (sellCount <= 0) {
                continue;
            }

            planned.add(new Planned(slot, recipe, sellCount));
        }

        for (Planned p : planned) {
            ItemStack current = itemHandler.getStackInSlot(p.slot());
            if (current.isEmpty()) {
                continue;
            }

            ItemStack soldStack = current.copy();
            int available = current.getCount();
            int toSell = Math.min(p.sellCount(), available);
            if (toSell <= 0) {
                continue;
            }

            int inputCount = Math.max(1, p.recipe().getInputCount());
            int requiredItems = toSell * inputCount;
            if (requiredItems <= 0 || available < requiredItems) {
                continue;
            }

            int totalOut = 0;
            for (int i = 0; i < toSell; i++) {
                totalOut += p.recipe().rollOutputCount(level, current);
            }
            if (totalOut <= 0) {
                continue;
            }

            ItemStack out = p.recipe().output.copy();
            out.setCount(totalOut);

            if (boundPlayerUuid != null) {
                if (!depositBoundRevenue(serverLevel, out)) {
                    continue;
                }

                itemHandler.extractItem(p.slot(), requiredItems, false);
                marketChanged |= SellingBinGroupManager.recordSale(serverLevel, p.recipe(), soldStack, toSell);
                continue;
            }

            itemHandler.extractItem(p.slot(), requiredItems, false);
            marketChanged |= SellingBinGroupManager.recordSale(serverLevel, p.recipe(), soldStack, toSell);

            ItemStack remaining = out;
            for (int i = 0; i < slots; i++) {
                remaining = itemHandler.insertItem(i, remaining, false);
                if (remaining.isEmpty()) {
                    break;
                }
            }

            if (!remaining.isEmpty()) {
                Block.popResource(level, worldPosition, remaining);
            }
        }

        if (marketChanged) {
            SellingBinEvents.syncAllPlayers(serverLevel);
        }
    }

    private boolean depositBoundRevenue(net.minecraft.server.level.ServerLevel serverLevel, ItemStack outputStack) {
        if (boundPlayerUuid == null) {
            return false;
        }

        var denomination = CurrencyDenomination.fromItemStack(outputStack);
        if (denomination.isEmpty()) {
            return false;
        }

        long grossAmount = denomination.get().totalValue(outputStack.getCount());
        if (grossAmount <= 0L) {
            return false;
        }

        Tax.TaxResult taxResult = Tax.calculate(grossAmount, boundTaxExempt);
        long netAmount = taxResult.netAmount();
        if (netAmount <= 0L) {
            return false;
        }

        CurrencyOperationResult result = ShopcoreCurrency.increase(boundPlayerUuid, (double) netAmount);
        boolean success = result.success();
        if (success && transactionNotificationEnabled) {
            Player onlinePlayer = serverLevel.getServer().getPlayerList().getPlayer(boundPlayerUuid);
            if (onlinePlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.sellingbin.selling_bin.revenue_notice", Component.literal(Long.toString(netAmount)).withStyle(net.minecraft.ChatFormatting.GOLD))
                                .withStyle(net.minecraft.ChatFormatting.GREEN),
                        false
                );
            }
        }

        return success;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("TicksUntilRun", ticksUntilRun);
        tag.putBoolean("LidTargetOpen", lidTargetOpen);
        tag.putFloat("LidOpenProgress", lidOpenProgress);
        tag.putFloat("LastLidOpenProgress", lastLidOpenProgress);
        if (boundPlayerUuid != null) {
            tag.putUUID("BoundPlayerUuid", boundPlayerUuid);
        }
        if (boundPlayerName != null) {
            tag.putString("BoundPlayerName", boundPlayerName);
        }
        tag.putBoolean("BoundTaxExempt", boundTaxExempt);
        tag.putBoolean("TransactionNotificationEnabled", transactionNotificationEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        suppressInventorySync = true;
        try {
            if (tag.contains("Inventory")) {
                itemHandler.deserializeNBT(tag.getCompound("Inventory"));
            }
        } finally {
            suppressInventorySync = false;
        }
        ticksUntilRun = tag.contains("TicksUntilRun") ? tag.getInt("TicksUntilRun") : INTERVAL_TICKS;
        lidTargetOpen = tag.getBoolean("LidTargetOpen");
        lidOpenProgress = tag.getFloat("LidOpenProgress");
        lastLidOpenProgress = tag.getFloat("LastLidOpenProgress");
        boundPlayerUuid = tag.hasUUID("BoundPlayerUuid") ? tag.getUUID("BoundPlayerUuid") : null;
        boundPlayerName = tag.contains("BoundPlayerName") ? tag.getString("BoundPlayerName") : null;
        boundTaxExempt = tag.getBoolean("BoundTaxExempt");
        transactionNotificationEnabled = tag.getBoolean("TransactionNotificationEnabled");
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        suppressInventorySync = true;
        try {
            if (tag.contains("Inventory")) {
                itemHandler.deserializeNBT(tag.getCompound("Inventory"));
            }
        } finally {
            suppressInventorySync = false;
        }
        ticksUntilRun = tag.contains("TicksUntilRun") ? tag.getInt("TicksUntilRun") : INTERVAL_TICKS;
        lidTargetOpen = tag.getBoolean("LidTargetOpen");
        lidOpenProgress = tag.getFloat("LidOpenProgress");
        lastLidOpenProgress = tag.getFloat("LastLidOpenProgress");
        boundPlayerUuid = tag.hasUUID("BoundPlayerUuid") ? tag.getUUID("BoundPlayerUuid") : null;
        boundPlayerName = tag.contains("BoundPlayerName") ? tag.getString("BoundPlayerName") : null;
        boundTaxExempt = tag.getBoolean("BoundTaxExempt");
        transactionNotificationEnabled = tag.getBoolean("TransactionNotificationEnabled");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("TicksUntilRun", ticksUntilRun);
        tag.putBoolean("LidTargetOpen", lidTargetOpen);
        tag.putFloat("LidOpenProgress", lidOpenProgress);
        tag.putFloat("LastLidOpenProgress", lastLidOpenProgress);
        if (boundPlayerUuid != null) {
            tag.putUUID("BoundPlayerUuid", boundPlayerUuid);
        }
        if (boundPlayerName != null) {
            tag.putString("BoundPlayerName", boundPlayerName);
        }
        tag.putBoolean("BoundTaxExempt", boundTaxExempt);
        tag.putBoolean("TransactionNotificationEnabled", transactionNotificationEnabled);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.sellingbin.selling_bin");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SellingBinMenu(ModMenus.SELLING_BIN.get(), containerId, playerInventory, this, dataAccess);
    }
}
