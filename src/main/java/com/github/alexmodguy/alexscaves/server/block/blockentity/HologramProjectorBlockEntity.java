package com.github.alexmodguy.alexscaves.server.block.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Function;

public class HologramProjectorBlockEntity extends BlockEntity {

    public int tickCount;
    private EntityType entityType;
    private CompoundTag entityTag;
    private Entity displayEntity;
    private Entity prevDisplayEntity;

    private float prevSwitchProgress;
    private float switchProgress;
    private float previousRotation;
    private float rotation;

    private UUID lastPlayerUUID;

    public HologramProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntityRegistry.HOLOGRAM_PROJECTOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState state, HologramProjectorBlockEntity entity) {
        entity.tickCount++;
        entity.prevSwitchProgress = entity.switchProgress;
        entity.previousRotation = entity.rotation;
        if(entity.prevDisplayEntity != entity.displayEntity || entity.isPlayerRender()){
            if(entity.displayEntity == null && !entity.isPlayerRender()){
                if(entity.switchProgress > 0.0F){
                    entity.switchProgress--;
                }else{
                    entity.prevDisplayEntity = null;
                }
            }else{
                if(entity.switchProgress < 10.0F){
                    entity.switchProgress++;
                }else{
                    entity.prevDisplayEntity = entity.displayEntity;
                }

            }
        }
        if(entity.isPlayerRender() && entity.lastPlayerUUID == null){
            entity.lastPlayerUUID = entity.entityTag.getUUID("UUID");
        }
        float redstoneSignal = level.getBestNeighborSignal(blockPos) * 1F;
        if(redstoneSignal > 0.0F){
            entity.rotation = entity.rotation + redstoneSignal;
        }
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        if(tag.contains("EntityType")){
            String str = tag.getString("EntityType");
            this.entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(str));
        }
        this.entityTag = tag.getCompound("EntityTag");
        this.rotation = tag.getFloat("Rotation");
        if(tag.contains("LastPlayerUUID")){
            this.lastPlayerUUID = tag.getUUID("LastPlayerUUID");
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if(this.entityType != null){
            tag.putString("EntityType", ForgeRegistries.ENTITY_TYPES.getKey(this.entityType).toString());
        }
        if(this.entityTag != null){
            tag.put("EntityTag", this.entityTag);
        }
        tag.putFloat("Rotation", this.rotation);
        if(lastPlayerUUID != null){
            tag.putUUID("LastPlayerUUID", lastPlayerUUID);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        float f = displayEntity == null ? 1.0F : Math.max(displayEntity.getBbWidth(), displayEntity.getBbHeight());
        return new AABB(pos.offset(-1, -1, -1), pos.offset(2, 2, 2)).inflate(Math.max(f - 0.5F, 1F));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet != null && packet.getTag() != null) {
            if(packet.getTag().contains("EntityType")){
                String str = packet.getTag().getString("EntityType");
                this.entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(str));
            }
            this.entityTag = packet.getTag().getCompound("EntityTag");
            this.rotation = packet.getTag().getFloat("Rotation");
            if(packet.getTag().contains("LastPlayerUUID")){
                this.lastPlayerUUID = packet.getTag().getUUID("LastPlayerUUID");
            }
        }
    }

    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public void setEntity(EntityType entityType, CompoundTag entityTag, float playerRot){
        this.entityType = entityType;
        this.entityTag = entityTag;
        this.rotation = playerRot;
        displayEntity = null;
        lastPlayerUUID = null;
    }

    public boolean isPlayerRender(){
        return entityType == EntityType.PLAYER;
    }

    public UUID getLastPlayerUUID(){
        return isPlayerRender() ? lastPlayerUUID : null;
    }

    public Entity getDisplayEntity(Level level){
        if(isPlayerRender()){
            return null;
        }
        if(displayEntity == null && entityType != null){
            displayEntity = EntityType.loadEntityRecursive(entityTag, level, Function.identity());
        }
        if(displayEntity == null && prevDisplayEntity != null){
            return prevDisplayEntity;
        }
        return displayEntity;
    }

    public float getSwitchAmount(float partialTicks) {
        return (prevSwitchProgress + (switchProgress - prevSwitchProgress) * partialTicks) * 0.1F;
    }

    public float getRotation(float partialTicks) {
        return previousRotation + (rotation - previousRotation) * partialTicks;
    }
}