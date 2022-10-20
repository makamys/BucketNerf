package makamys.bucketnerf.compat;

import java.util.List;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInterModComms;
import makamys.bucketnerf.BucketNerf;
import makamys.bucketnerf.Config;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaEntityAccessor;
import mcp.mobius.waila.api.IWailaEntityProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class WailaCompat {

    public static void init() {
        FMLInterModComms.sendMessage("Waila", "register", WailaCompat.class.getName() + ".wailaCallback");
    }
    
    public static void wailaCallback(IWailaRegistrar registrar) {
        if(Config._enableTameableArachne && Loader.isModLoaded("TameableArachneMod")) {
            registrar.registerBodyProvider(new ArachneDataProvider(), (Class<?>)EntityList.stringToClassMapping.get("tameArachne"));
            registrar.registerBodyProvider(new ArachneDataProvider(), (Class<?>)EntityList.stringToClassMapping.get("tameArachneMedium"));
            registrar.registerBodyProvider(new ArachneDataProvider(), (Class<?>)EntityList.stringToClassMapping.get("tameHarpy"));
        }
    }
    
    public static class ArachneDataProvider implements IWailaEntityProvider {

        @Override
        public Entity getWailaOverride(IWailaEntityAccessor accessor, IWailaConfigHandler config) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getWailaHead(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor,
                IWailaConfigHandler config) {
            return currenttip;
        }

        @Override
        public List<String> getWailaBody(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor,
                IWailaConfigHandler config) {
            if(Config._enableTameableArachne) {
                String milkStatus = BucketNerf.getMilkStatusString(entity, accessor.getPlayer());
                if(milkStatus != null) {
                    currenttip.add(milkStatus);
                }
            }
            return currenttip;
        }

        @Override
        public List<String> getWailaTail(Entity entity, List<String> currenttip, IWailaEntityAccessor accessor,
                IWailaConfigHandler config) {
            return currenttip;
        }

        @Override
        public NBTTagCompound getNBTData(EntityPlayerMP player, Entity ent, NBTTagCompound tag, World world) {
            return tag;
        }
        
    }
    
}
