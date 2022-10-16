package makamys.bucketnerf;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = BucketNerf.MODID, version = BucketNerf.VERSION)
public class BucketNerf
{
    public static final String MODID = "bucketnerf";
    public static final String VERSION = "@VERSION@";
    
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    public static SimpleNetworkWrapper networkWrapper;
    
    public static final int MILK_COOLDOWN = 20 * 10; // 10 seconds
    public static final int NEXT_MILK_TIME_DATA_ID = 29;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        if(!event.entityPlayer.capabilities.isCreativeMode) {
            ItemBucket bucket = (ItemBucket)event.current.getItem();
            if(bucket == Items.water_bucket) {
                event.setCanceled(true);
            }
        }
    }
    
    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        if(event.entityPlayer.getHeldItem() != null && event.entityPlayer.getHeldItem().getItem() == Items.bucket) {
            if(isMilkableArachne(event.target)) {
                EntityTameable tameable = (EntityTameable)event.target;   
                if(tameable.func_152114_e(event.entityPlayer)) { // isTamedBy
                    BucketNerfProperties props = BucketNerfProperties.fromEntity(tameable);
                    
                    long worldTime = event.entityLiving.worldObj.getTotalWorldTime();
                    
                    LOGGER.trace("worldTime: " + worldTime + " nextMilkTime: " + props.getNextMilkTime());
                    
                    if(worldTime > props.getNextMilkTime()) {
                        props.setNextMilkTime(worldTime + MILK_COOLDOWN);
                        LOGGER.trace("can milk, set next time to " + props.getNextMilkTime());
                    } else {
                        LOGGER.trace("can not milk.");
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
        if(isMilkableArachne(event.entity)) {
            event.entity.getDataWatcher().addObjectByDataType(29, 4);
            event.entity.getDataWatcher().updateObject(29, "-1");
            event.entity.registerExtendedProperties(MODID, new BucketNerfProperties());
        }
    }
    
    private static boolean isMilkableArachne(Entity entity) {
        if(entity instanceof EntityTameable) {
            String entityName = (String)EntityList.classToStringMapping.get(entity.getClass());
            return entityName.equals("tameArachne") || entityName.equals("tameArachneMedium") || entityName.equals("tameHarpy");
        }
        return false;
    }
    
    public static class BucketNerfProperties implements IExtendedEntityProperties {
        
        Entity entity;
        
        public long getNextMilkTime() {
            return Long.parseLong(entity.getDataWatcher().getWatchableObjectString(29));
        }
        
        public void setNextMilkTime(long t) {
            entity.getDataWatcher().updateObject(29, String.valueOf(t));
        }
        
        @Override
        public void saveNBTData(NBTTagCompound compound) {
            NBTTagCompound myData = new NBTTagCompound();
            
            long nextMilkTime = getNextMilkTime();
            if(nextMilkTime > 0) {
                myData.setLong("NextMilkTime", nextMilkTime);
            }
            
            if(!myData.func_150296_c().isEmpty()) {
                compound.setTag(MODID, myData);
            }
        }

        @Override
        public void loadNBTData(NBTTagCompound compound) {
            if(compound.hasKey(MODID)) {
                NBTTagCompound myData = compound.getCompoundTag(MODID);
                if(myData.hasKey("NextMilkTime")) {
                    entity.getDataWatcher().updateObject(29, String.valueOf(myData.getLong("NextMilkTime")));
                }
            }
        }

        @Override
        public void init(Entity entity, World world) {
            this.entity = entity;
        }
        
        public static BucketNerfProperties fromEntity(Entity entity) {
            BucketNerfProperties props = (BucketNerfProperties)entity.getExtendedProperties(MODID);
            if(props == null) {
                throw new NullPointerException();
            }
            return props;
        }
        
    }
}
