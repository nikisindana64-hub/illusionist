package com.illusionist.client;

import com.illusionist.IllusionistMod;
import com.illusionist.entity.MagicianPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Высокий тонкий Фокусник: цилиндр с широкими полями, жабо, фрак с хвостами,
 * трость и пять карт, вращающихся вокруг тела. Текстура 128x128 (см. tools/gen_magician_skins.py).
 *
 * Раскладка UV (u,v): голова 0,0 | поля 0,16 | тулья 0,30 | торс 32,30 | пр.рука 56,30 | лев.рука 68,30 |
 * пр.нога 80,30 | лев.нога 92,30 | хвосты 104,30 | трость 0,50 | карта 10,50 | жабо 20,50
 */
public class MagicianModel<T extends PathfinderMob> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(IllusionistMod.id("magician"), "main");

    private static final float DEG = (float) Math.PI / 180F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart tails;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart orbit;

    public MagicianModel(ModelPart part) {
        this.root = part.getChild("root");
        this.head = root.getChild("head");
        this.torso = root.getChild("torso");
        this.tails = torso.getChild("tails");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.orbit = root.getChild("card_orbit");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition base = mesh.getRoot();
        PartDefinition root = base.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        // Голова + шляпа
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(0, 16).addBox(-6.5F, -8.5F, -6.5F, 13.0F, 1.0F, 13.0F)
                        .texOffs(0, 30).addBox(-4.0F, -18.5F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, -30.0F, 0.0F));

        // Торс + жабо + хвосты фрака
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                        .texOffs(32, 30).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 14.0F, 4.0F)
                        .texOffs(20, 50).addBox(-5.0F, -1.0F, -5.0F, 10.0F, 2.0F, 10.0F),
                PartPose.offset(0.0F, -30.0F, 0.0F));
        torso.addOrReplaceChild("tails", CubeListBuilder.create()
                        .texOffs(104, 30).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 12.0F, 1.0F),
                PartPose.offset(0.0F, 12.0F, 2.0F));

        // Руки (правая держит трость)
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(56, 30).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F),
                PartPose.offset(-4.0F, -29.0F, 0.0F));
        rightArm.addOrReplaceChild("cane", CubeListBuilder.create()
                        .texOffs(0, 50).addBox(-2.0F, 6.0F, -4.0F, 1.0F, 20.0F, 1.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(68, 30).addBox(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F),
                PartPose.offset(4.0F, -29.0F, 0.0F));

        // Ноги
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(80, 30).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F),
                PartPose.offset(-2.0F, -16.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(92, 30).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F),
                PartPose.offset(2.0F, -16.0F, 0.0F));

        // Пять карт по кругу радиуса 10
        PartDefinition orbit = root.addOrReplaceChild("card_orbit", CubeListBuilder.create(),
                PartPose.offset(0.0F, -22.0F, 0.0F));
        for (int i = 0; i < 5; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 5.0);
            orbit.addOrReplaceChild("card_" + i, CubeListBuilder.create()
                            .texOffs(10, 50).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 4.0F, 1.0F),
                    PartPose.offsetAndRotation(Mth.cos(angle) * 10.0F, (i % 2 == 0 ? -2.0F : 2.0F),
                            Mth.sin(angle) * 10.0F, 0.0F, -angle + (float) Math.PI / 2F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        MagicianPose pose = entity instanceof MagicianPose p ? p : null;
        boolean watched = pose != null && pose.isWatchedPose();
        boolean casting = pose != null && pose.getCastTicks() > 0;

        this.head.yRot = netHeadYaw * DEG;
        this.head.xRot = headPitch * DEG;
        this.head.zRot = Mth.sin(ageInTicks * 0.07F) * 0.08F;

        float swing = watched ? 0.0F : limbSwing;
        float amount = watched ? 0.0F : limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(swing * 0.6662F) * 0.85F * amount;
        this.leftLeg.xRot = Mth.cos(swing * 0.6662F + (float) Math.PI) * 0.85F * amount;

        this.rightArm.zRot = 0.04F;
        this.leftArm.zRot = -0.04F;
        this.rightArm.yRot = 0.0F;
        if (watched) {
            // Замер в позе «та-да»: руки распахнуты, голова склонена слишком сильно
            this.rightArm.xRot = -0.25F;
            this.leftArm.xRot = -0.25F;
            this.rightArm.zRot = -1.25F;
            this.leftArm.zRot = 1.25F;
            this.head.zRot = 0.42F;
            this.head.yRot *= 0.3F;
        } else if (casting) {
            this.rightArm.xRot = -1.65F + Mth.sin(ageInTicks * 0.6F) * 0.25F;
            this.leftArm.xRot = -1.65F - Mth.sin(ageInTicks * 0.6F) * 0.25F;
        } else {
            this.rightArm.xRot = Mth.cos(swing * 0.6662F + (float) Math.PI) * 0.45F * amount - 0.12F;
            this.leftArm.xRot = Mth.cos(swing * 0.6662F) * 0.45F * amount;
        }

        this.torso.zRot = Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.tails.xRot = 0.08F + limbSwingAmount * 0.6F + Mth.sin(ageInTicks * 0.1F) * 0.04F;

        this.orbit.yRot = ageInTicks * (casting ? 0.25F : 0.08F);
        this.orbit.y = -22.0F + Mth.sin(ageInTicks * 0.12F) * 1.6F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
