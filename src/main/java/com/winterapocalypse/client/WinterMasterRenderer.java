package com.winterapocalypse.client;

import com.winterapocalypse.WinterApocalypse;
import com.winterapocalypse.entity.WinterMaster;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WinterMasterRenderer extends MobRenderer<WinterMaster, VillagerModel<WinterMaster>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WinterApocalypse.MODID, "textures/entity/winter_master.png");

    public WinterMasterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(WinterMaster entity) {
        return TEXTURE;
    }
}
