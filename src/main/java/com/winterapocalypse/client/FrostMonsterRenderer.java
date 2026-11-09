package com.winterapocalypse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.winterapocalypse.WinterApocalypse;
import com.winterapocalypse.entity.FrostMonster;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Один рендерер для обоих монстров: разные текстуры и масштаб (1.8 блока и 4.2 блока). */
public class FrostMonsterRenderer extends HumanoidMobRenderer<FrostMonster, FrostZombieModel> {
    private final ResourceLocation texture;
    private final float scale;

    public FrostMonsterRenderer(EntityRendererProvider.Context ctx, String textureName, float scale, float shadow) {
        super(ctx, new FrostZombieModel(ctx.bakeLayer(ModelLayers.ZOMBIE)), shadow);
        this.texture = new ResourceLocation(WinterApocalypse.MODID, "textures/entity/" + textureName + ".png");
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(FrostMonster entity) {
        return texture;
    }

    // модель зомби = 2.0 блока: 0.9 -> 1.8 (рост игрока), 2.1 -> 4.2
    @Override
    protected void scale(FrostMonster entity, PoseStack stack, float partialTick) {
        stack.scale(scale, scale, scale);
    }
}
