package makamys.bucketnerf;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import makamys.bucketnerf.Packets.HandlerEmptyBucket;
import makamys.bucketnerf.Packets.MessageEmptyBucket;

@Mod(modid = BucketNerf.MODID, version = BucketNerf.VERSION)
public class BucketNerf
{
    public static final String MODID = "bucketnerf";
    public static final String VERSION = "@VERSION@";
    
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    public static SimpleNetworkWrapper networkWrapper;
    
    public static List<Pair<Pair<Item, Integer>, Pair<Item, Integer>>> bucketRecipes = new ArrayList<>();

    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        Config.reloadConfig();
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        
        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        networkWrapper.registerMessage(HandlerEmptyBucket.class, MessageEmptyBucket.class, 0, Side.SERVER);
        
        ClientCommandHandler.instance.registerCommand(new BucketNerfCommand());
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        parseConfig();
    }
    
    public static void parseConfig() {
        bucketRecipes.clear();
        
        for(String line : Config.bucketUsageRecipes) {
            if(line.startsWith("#") || line.isEmpty()) continue;
            
            try {
                String[] leftAndRight = line.split(" ");
                if(leftAndRight.length != 2) {
                    throw new IllegalArgumentException("Line contains more than 2 tokens");
                } else {
                    String left = leftAndRight[0];
                    String right = leftAndRight[1];
                    
                    Pair<Item, Integer> leftItemMeta = parseItemMeta(left);
                    Pair<Item, Integer> rightItemMeta = parseItemMeta(right);
                    
                    bucketRecipes.add(Pair.of(leftItemMeta, rightItemMeta));
                }
            } catch(Exception e) {
                LOGGER.warn("Error parsing line `" + line + "`: " + e.getMessage());
            }
        }
    }
    
    private static Pair<Item, Integer> parseItemMeta(String string) {
        try {
            String[] parts = string.split(":");
            if(parts.length != 2 && parts.length != 3) {
                throw new IllegalArgumentException("String should consist of two or three colon-separated components");
            }
            String namespace = parts[0];
            String name = parts[1];
            int meta = parts.length < 3 ? -1 : Integer.parseInt(parts[2]);
            
            Item item = (Item)Item.itemRegistry.getObject(namespace + ":" + name);
            if(item == null) {
                throw new IllegalArgumentException("Unknown item: " + namespace + ":" + name);
            }
            return Pair.of(item, meta);
        } catch(Exception e) {
            LOGGER.warn("Error parsing item meta string `" + string + "`: " + e.getMessage());
            throw new RuntimeException();
        }
    }
    
    public static Pair<Item, Integer> getRecipeOutput(ItemStack is) {
        if(is == null) return null;
        
        for(Pair<Pair<Item, Integer>, Pair<Item, Integer>> recipe : bucketRecipes) {
            Item inputItem = recipe.getLeft().getLeft();
            int inputMeta = recipe.getLeft().getRight();
            if(inputItem == is.getItem() && (inputMeta == -1 || inputMeta == is.getItemDamage())) {
                return recipe.getRight();
            }
        }
        return null;
    }
    
    @EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        Config.reloadConfig();
        parseConfig();
    }
    
    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        
    }
    
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        Pair<Item, Integer> output = getRecipeOutput(event.entityPlayer.getHeldItem());
        
        if(output != null) {
            event.setCanceled(true);
            networkWrapper.sendToServer(new MessageEmptyBucket(event.entityPlayer));            

            for (int l = 0; l < 8; ++l) {
                event.world.spawnParticle("splash", (double)event.x + Math.random(), (double)event.y + Math.random() + 1, (double)event.z + Math.random(), 0.0D, 0.0D, 0.0D);
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
                        props.setNextMilkTime(worldTime + generateMilkCooldownFor(tameable));
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
    
    private static int generateMilkCooldownFor(Entity entity) {
        if(entity instanceof EntityTameable) {
            Random rand = entity.worldObj.rand;
            String entityName = (String)EntityList.classToStringMapping.get(entity.getClass());
            
            int max = 0;
            int min = 0;
            
            switch(entityName) {
            case "tameArachne":
                max = Config.tameArachneMilkCooldownMax;
                min = Config.tameArachneMilkCooldownMin;
                break;
            case "tameArachneMedium":
                max = Config.tameArachneMediumMilkCooldownMax;
                min = Config.tameArachneMediumMilkCooldownMin;
                break;
            case "harpy":
                max = Config.tameHarpyMilkCooldownMax;
                min = Config.tameHarpyMilkCooldownMin;
                break;
            }
            return min + rand.nextInt(max - min + 1);
        }
        return 0;
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
                    entity.getDataWatcher().updateObject(Config.nextMilkTimeDataID, String.valueOf(myData.getLong("NextMilkTime")));
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