package com.yongaishide.chaosworld.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.ITickManager;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.misc.CondenserBlockEntity;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.me.helpers.MachineSource;
import appeng.util.inv.AppEngInternalInventory;
import com.yongaishide.chaosworld.ae2.OmniCellComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CondenserBlockEntity.class, remap = false)
public abstract class CondenserBlockEntityMixin extends AEBaseInvBlockEntity
        implements IGridConnectedBlockEntity, IGridTickable {

    @Unique
    private IManagedGridNode ufo$mainNode;

    @Unique
    private IActionSource ufo$actionSource;

    @Shadow(remap = false)
    private AppEngInternalInventory outputSlot;

    @Shadow(remap = false)
    private AppEngInternalInventory storageSlot;

    public CondenserBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void ufo$init(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
        this.ufo$mainNode = GridHelper.createManagedNode((IGridConnectedBlockEntity) (Object) this, BlockEntityNodeListener.INSTANCE)
                .setInWorldNode(true)
                .setTagName("main")
                .setIdlePowerUsage(0.0)
                .addService(IGridTickable.class, (IGridTickable) (Object) this);
        this.ufo$actionSource = new MachineSource((IActionHost) (Object) this);
    }

    @Override
    public IManagedGridNode getMainNode() {
        return this.ufo$mainNode;
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        return AECableType.SMART;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State state) {
        this.ufo$alertTickManager();
    }

    @Inject(method = "getStorage", at = @At("HEAD"), cancellable = true, remap = false)
    private void ufo$getStorage(CallbackInfoReturnable<Double> cir) {
        ItemStack stack = this.storageSlot.getStackInSlot(0);
        long bytes = OmniCellComponents.bytesOf(stack);
        if (bytes > 0) {
            cir.setReturnValue((double) bytes * 8);
        }
    }

    @Inject(method = "loadTag", at = @At("TAIL"), remap = false)
    private void ufo$loadTag(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this.ufo$mainNode != null) {
            this.ufo$mainNode.loadFromNBT(tag);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = false)
    private void ufo$saveAdditional(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this.ufo$mainNode != null) {
            this.ufo$mainNode.saveToNBT(tag);
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.ufo$createNode();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        this.scheduleInit();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.ufo$mainNode != null) {
            this.ufo$mainNode.destroy();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.ufo$mainNode != null) {
            this.ufo$mainNode.destroy();
        }
    }

    @Unique
    private void ufo$createNode() {
        if (this.ufo$mainNode.getNode() != null) {
            return;
        }
        this.ufo$mainNode
                .setVisualRepresentation(this.getBlockState().getBlock().asItem())
                .create(this.getLevel(), this.getBlockPos());
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 10, true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLast) {
        return this.ufo$exportToNetwork();
    }

    /**
     * AE2 19.2.18 的 fillOutput() 中，产物通过 outputSlot.insertItem(0, output, false) 写入。
     * 这里重定向该调用：先尝试直推 ME 网络，成功则跳过原版插入，失败则调用原版逻辑。
     */
    @Redirect(method = "fillOutput", at = @At(value = "INVOKE",
            target = "Lappeng/util/inv/AppEngInternalInventory;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private ItemStack ufo$redirectInsertOutput(AppEngInternalInventory inventory, int slot, ItemStack stack, boolean simulate) {
        if (!simulate && this.ufo$pushOutputDirectToNetwork(stack)) {
            this.saveChanges();
            return ItemStack.EMPTY;
        }
        return inventory.insertItem(slot, stack, simulate);
    }

    /**
     * 尝试把即将产出的物品直插网络存储。
     */
    @Unique
    private boolean ufo$pushOutputDirectToNetwork(ItemStack pending) {
        IGridNode node = this.ufo$mainNode.getNode();
        if (node == null || !node.isActive()) {
            return false;
        }
        IGrid grid = node.getGrid();
        if (grid == null) {
            return false;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        if (storage == null) {
            return false;
        }
        if (pending == null || pending.isEmpty()) {
            return false;
        }
        long inserted = storage.insert(AEItemKey.of(pending), pending.getCount(), Actionable.MODULATE, this.ufo$actionSource);
        return inserted > 0;
    }

    @Unique
    private TickRateModulation ufo$exportToNetwork() {
        IGridNode node = this.ufo$mainNode.getNode();
        if (node == null || !node.isActive()) {
            return TickRateModulation.SLEEP;
        }
        IGrid grid = node.getGrid();
        if (grid == null) {
            return TickRateModulation.SLEEP;
        }
        ItemStack stack = this.outputSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            return TickRateModulation.SLEEP;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        if (storage == null) {
            return TickRateModulation.SLEEP;
        }
        long inserted = storage.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, this.ufo$actionSource);
        if (inserted > 0) {
            this.outputSlot.extractItem(0, (int) inserted, false);
            this.saveChanges();
        }
        return inserted > 0 ? TickRateModulation.FASTER : TickRateModulation.IDLE;
    }

    @Unique
    private void ufo$alertTickManager() {
        IGridNode node = this.ufo$mainNode.getNode();
        if (node == null || node.getGrid() == null) {
            return;
        }
        ITickManager tickManager = node.getGrid().getService(ITickManager.class);
        if (tickManager != null) {
            tickManager.alertDevice(node);
        }
    }

    @Mixin(targets = "appeng.blockentity.misc.CondenserBlockEntity$CondenseItemHandler", remap = false)
    public abstract static class CondenseItemHandlerMixin {

        @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true, remap = false)
        private void chaosworld$blockOmniComponent(int slot, ItemStack stack, boolean simulate,
                CallbackInfoReturnable<ItemStack> cir) {
            if (OmniCellComponents.isOmniCellComponent(stack)) {
                cir.setReturnValue(stack);
            }
        }
    }

    @Mixin(targets = "appeng.blockentity.misc.CondenserMEStorage", remap = false)
    public abstract static class CondenserMEStorageMixin {

        @Inject(method = "insert", at = @At("HEAD"), cancellable = true, remap = false)
        private void chaosworld$blockOmniComponent(appeng.api.stacks.AEKey key, long amount,
                appeng.api.config.Actionable mode, appeng.api.networking.security.IActionSource source,
                CallbackInfoReturnable<Long> cir) {
            if (key instanceof AEItemKey itemKey && OmniCellComponents.isOmniCellComponent(itemKey.toStack())) {
                cir.setReturnValue(0L);
            }
        }
    }
}
