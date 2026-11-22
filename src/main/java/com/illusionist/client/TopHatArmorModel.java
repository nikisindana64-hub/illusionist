package com.illusionist.client;

import com.illusionist.IllusionistMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Объёмный цилиндр на голове игрока (вместо плоского «шлема»).
 * Это HumanoidModel без частей тела: на голове только поля, тулья и лента. Текстура 64x64.
 */
public class TopHatArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(IllusionistMod.id("top_hat"), "main");

    public TopHatArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                // поля шляпы 12x1x12
                .texOffs(0, 0).addBox(-6.0F, -8.6F, -6.0F, 12.0F, 1.0F, 12.0F)
                // тулья 8x7x8
                .texOffs(0, 16).addBox(-4.0F, -15.6F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.05F))
                // алая лента
                .texOffs(0, 32).addBox(-4.0F, -10.6F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
