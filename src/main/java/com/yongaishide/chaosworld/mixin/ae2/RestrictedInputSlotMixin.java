package com.yongaishide.chaosworld.mixin.ae2;

import appeng.menu.slot.RestrictedInputSlot;
import com.yongaishide.chaosworld.ae2.OmniCellComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物质聚合器槽位兼容:
 * <ul>
 *   <li>顶部存储组件槽默认只接受 {@link appeng.api.implementations.items.IStorageComponent},
 *       让 ae2omnicells 的 omni 存储组件(普通 Item,不实现该接口)也能放入;</li>
 *   <li>销毁/输入槽(TRASH 类型)拒绝 omni 存储组件,防止被当垃圾桶销毁。</li>
 * </ul>
 */
@Mixin(value = RestrictedInputSlot.class, remap = false)
public class RestrictedInputSlotMixin {

    @Shadow(remap = false)
    private RestrictedInputSlot.PlacableItemType which;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, remap = false)
    private void chaosworld_restrictOmniComponent(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!OmniCellComponents.isOmniCellComponent(stack)) {
            return;
        }
        if (this.which == RestrictedInputSlot.PlacableItemType.STORAGE_COMPONENT) {
            cir.setReturnValue(true);
        } else if (this.which == RestrictedInputSlot.PlacableItemType.TRASH) {
            cir.setReturnValue(false);
        }
    }
}
