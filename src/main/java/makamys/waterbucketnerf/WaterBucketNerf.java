package makamys.waterbucketnerf;

import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = WaterBucketNerf.MODID, version = WaterBucketNerf.VERSION)
public class WaterBucketNerf
{
    public static final String MODID = "waterbucketnerf";
    public static final String VERSION = "@VERSION@";

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        ItemBucket bucket = (ItemBucket)event.current.getItem();
        if(bucket == Items.water_bucket) {
            event.setCanceled(true);
        }
    }
}
