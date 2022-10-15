package makamys.bucketnerf;

import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = BucketNerf.MODID, version = BucketNerf.VERSION)
public class BucketNerf
{
    public static final String MODID = "bucketnerf";
    public static final String VERSION = "@VERSION@";

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
}
