package com.y271727uy.sellingbin.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.y271727uy.sellingbin.block.SellingBinBlock;
import com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity;
import com.y271727uy.sellingbin.client.render.model.SellingBinModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nonnull;

public class SellingBinBlockEntityRenderer implements BlockEntityRenderer<SellingBinBlockEntity> {
    private final SellingBinModel model;

    public SellingBinBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new SellingBinModel(context.bakeLayer(SellingBinModel.LAYER_LOCATION));
    }

    @Override
    public void render(@Nonnull SellingBinBlockEntity blockEntity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(SellingBinBlock.FACING)) return;
        Direction facing = state.getValue(SellingBinBlock.FACING);
        poseStack.pushPose();
        poseStack.scale(1, -1, 1);
        poseStack.translate(0.5f, -1.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
        model.applyAnimation(blockEntity.getLidOpenProgress(partialTick));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(SellingBinModel.TEXTURE_LOCATION));
        model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}



