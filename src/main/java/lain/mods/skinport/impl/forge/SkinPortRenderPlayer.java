package lain.mods.skinport.impl.forge;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lain.mods.skinport.impl.forge.network.packet.PacketGet1;
import lain.mods.skinport.init.forge.ForgeSkinPort;
import lain.mods.skins.impl.PlayerProfile;

@SideOnly(Side.CLIENT)
public class SkinPortRenderPlayer extends RenderPlayer {

    public SkinPortModelPlayer modelPlayer;

    public SkinPortRenderPlayer(RenderManager manager, boolean smallArms) {
        super();

        setRenderManager(manager);
        mainModel = new SkinPortModelPlayer(0.0F, smallArms);
        modelBipedMain = (ModelBiped) mainModel;
        modelPlayer = (SkinPortModelPlayer) mainModel;
    }

    @Override
    public void doRender(AbstractClientPlayer p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_,
        float p_76986_8_, float p_76986_9_) {
        applyCustomizationFlags(
            p_76986_1_,
            () -> super.doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_));
    }

    @Override
    protected void renderEquippedItems(AbstractClientPlayer p_77029_1_, float p_77029_2_) {
        applyCustomizationFlags(p_77029_1_, () -> super.renderEquippedItems(p_77029_1_, p_77029_2_));
    }

    @Override
    public void renderFirstPersonArm(EntityPlayer player) {
        applyCustomizationFlags(player, () -> {
            modelPlayer.isRiding = modelPlayer.isSneak = false;
            super.renderFirstPersonArm(player);
        });
    }

    /**
     * Applies skin customization flags to the model, executes the rendering action, then restores original state.
     */
    private void applyCustomizationFlags(EntityPlayer player, Runnable renderAction) {
        ModelVisibilityState state = saveModelVisibility();

        int flags = getFlags(player);
        applyFlagsToModel(flags);

        renderAction.run();

        restoreModelVisibility(state);
    }

    private ModelVisibilityState saveModelVisibility() {
        return new ModelVisibilityState(
            modelPlayer.bipedHeadwear.showModel,
            modelPlayer.bipedLeftLegwear.showModel,
            modelPlayer.bipedRightLegwear.showModel,
            modelPlayer.bipedLeftArmwear.showModel,
            modelPlayer.bipedRightArmwear.showModel,
            modelPlayer.bipedBodyWear.showModel,
            modelPlayer.bipedCloak.showModel);
    }

    private void restoreModelVisibility(ModelVisibilityState state) {
        modelPlayer.bipedHeadwear.showModel = state.headwear;
        modelPlayer.bipedLeftLegwear.showModel = state.leftLegwear;
        modelPlayer.bipedRightLegwear.showModel = state.rightLegwear;
        modelPlayer.bipedLeftArmwear.showModel = state.leftArmwear;
        modelPlayer.bipedRightArmwear.showModel = state.rightArmwear;
        modelPlayer.bipedBodyWear.showModel = state.bodyWear;
        modelPlayer.bipedCloak.showModel = state.cloak;
    }

    private void applyFlagsToModel(int flags) {
        if (modelPlayer.bipedHeadwear.showModel)
            modelPlayer.bipedHeadwear.showModel = SkinCustomization.contains(flags, SkinCustomization.hat);
        if (modelPlayer.bipedLeftLegwear.showModel) modelPlayer.bipedLeftLegwear.showModel = SkinCustomization
            .contains(flags, SkinCustomization.left_pants_leg);
        if (modelPlayer.bipedRightLegwear.showModel) modelPlayer.bipedRightLegwear.showModel = SkinCustomization
            .contains(flags, SkinCustomization.right_pants_leg);
        if (modelPlayer.bipedLeftArmwear.showModel)
            modelPlayer.bipedLeftArmwear.showModel = SkinCustomization.contains(flags, SkinCustomization.left_sleeve);
        if (modelPlayer.bipedRightArmwear.showModel)
            modelPlayer.bipedRightArmwear.showModel = SkinCustomization.contains(flags, SkinCustomization.right_sleeve);
        if (modelPlayer.bipedBodyWear.showModel)
            modelPlayer.bipedBodyWear.showModel = SkinCustomization.contains(flags, SkinCustomization.jacket);
        if (modelPlayer.bipedCloak.showModel)
            modelPlayer.bipedCloak.showModel = SkinCustomization.contains(flags, SkinCustomization.cape);
    }

    private int getFlags(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().thePlayer) return SkinCustomization.ClientFlags;
        UUID uuid = player.getUniqueID();
        Optional<UUID> uuid2 = Optional.ofNullable(
            PlayerProfile.wrapGameProfile(player.getGameProfile())
                .getPlayerID());
        Integer flags = SkinCustomization.Flags.get(Side.CLIENT, uuid, uuid2);
        if (flags == null) {
            SkinCustomization.Flags.put(Side.CLIENT, uuid, uuid2, flags = SkinCustomization.getDefaultFlags());
            ForgeSkinPort.network.sendToServer(new PacketGet1(uuid));
        }
        return flags;
    }

    /**
     * Immutable holder for model visibility state.
     */
    private static class ModelVisibilityState {

        final boolean headwear;
        final boolean leftLegwear;
        final boolean rightLegwear;
        final boolean leftArmwear;
        final boolean rightArmwear;
        final boolean bodyWear;
        final boolean cloak;

        ModelVisibilityState(boolean headwear, boolean leftLegwear, boolean rightLegwear, boolean leftArmwear,
            boolean rightArmwear, boolean bodyWear, boolean cloak) {
            this.headwear = headwear;
            this.leftLegwear = leftLegwear;
            this.rightLegwear = rightLegwear;
            this.leftArmwear = leftArmwear;
            this.rightArmwear = rightArmwear;
            this.bodyWear = bodyWear;
            this.cloak = cloak;
        }
    }

}
