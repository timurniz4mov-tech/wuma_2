package com.winterapocalypse.client;

import com.winterapocalypse.entity.FrostMonster;
import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.geom.ModelPart;

/** Зомби-поза (руки вперёд при агрессии) для ледяных монстров. */
public class FrostZombieModel extends AbstractZombieModel<FrostMonster> {
    public FrostZombieModel(ModelPart root) {
        super(root);
    }

    @Override
    public boolean isAggressive(FrostMonster entity) {
        return entity.isAggressive();
    }
}
