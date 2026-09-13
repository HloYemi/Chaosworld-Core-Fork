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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物质聚合器：接入 AE 网络。
 * <p>
 * AE2 的网格设备依靠 AENetworkedBlockEntity 挂载节点；冷凝器并非网格机器，本 Mixin
 * 通过声明 superclass 混入（CondenserBlockEntity 的直接父类）覆写其生命周期，
 * 并用目标类自身声明的方法作注入点：{@code <init>} 创建 managed node，
 * {@code loadTag}/{@code saveAdditional} 持久化节点状态，{@code fillOutput} 产出后唤醒
 * 网格 ticker，将输出槽物品自动推入网络存储。
 */
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

    // Mixin 不合并构造器；此声明仅供编译期满足 super 约束。
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
        // 与 AENetworkedBlockEntity 一致:重载后重新调度首 tick 初始化(触发 onReady -> createNode)
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

    @Inject(method = "fillOutput", at = @At("TAIL"), remap = false)
    private void ufo$afterFillOutput(CallbackInfo ci) {
        this.ufo$alertTickManager();
    }

    /**
     * 产出瞬间直接推入 AE 网络,不经输出槽。
     * <p>
     * {@code fillOutput()} 循环内每产出一个就调用 {@code addOutput()}:这里拦截,
     * 网络在线且能收就直插网络并跳过原逻辑(产物不进输出槽);网络不可用/收不下
     * 则回落到原 {@code addOutput()}(进输出槽等 ticker 再导出)。
     */
    @Inject(method = "addOutput", at = @At("HEAD"), cancellable = true, remap = false)
    private void ufo$addOutput(CallbackInfo ci) {
        if (this.ufo$pushOutputDirectToNetwork()) {
            ci.cancel();
            this.saveChanges();
        }
    }

    /**
     * 尝试把即将产出的物品直插网络存储。
     *
     * @return true = 已成功插入(调用方应跳过 addOutput),false = 网络不可用/满了,走原逻辑
     */
    @Unique
    private boolean ufo$pushOutputDirectToNetwork() {
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
        ItemStack pending = this.ufo$peekOutputStack();
        if (pending == null || pending.isEmpty()) {
            return false;
        }
        long inserted = storage.insert(AEItemKey.of(pending), pending.getCount(), Actionable.MODULATE, this.ufo$actionSource);
        return inserted > 0;
    }

    /**
     * 读取即将产出的产物本体(单件)。{@code getOutput()} 按当前模式返回
     * matter ball / singularity,TRASH 模式返回空,此时不走直推。
     */
    @Unique
    private ItemStack ufo$peekOutputStack() {
        ItemStack output = this.getOutput();
        if (output == null || output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = output.copy();
        copy.setCount(1);
        return copy;
    }

    @Shadow(remap = false)
    protected abstract ItemStack getOutput();

    /**
     * 将输出槽的物品尝试插入网络存储；成功则 FASTER 继续清空，槽空则 SLEEP，
     * 网络已满则 IDLE 保持频率重试。
     */
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
