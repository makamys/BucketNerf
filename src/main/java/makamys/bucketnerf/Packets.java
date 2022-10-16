package makamys.bucketnerf;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

public class Packets {
    
    public static class HandlerEmptyBucket implements IMessageHandler<MessageEmptyBucket, IMessage> {

        @Override
        public IMessage onMessage(MessageEmptyBucket msg, MessageContext ctx) {
            WorldServer[] worldServers = MinecraftServer.getServer().worldServers;
            if(msg.dim >= 0 && msg.dim < worldServers.length) {
                EntityPlayer player = MinecraftServer.getServer().worldServers[msg.dim].func_152378_a(new UUID(msg.uuidMostSig, msg.uuidLeastSig));
                if(player != null) {
                    Pair<Item, Integer> output = BucketNerf.getRecipeOutput(player.getHeldItem());
                    if(output != null) {
                        player.inventory.mainInventory[player.inventory.currentItem] = new ItemStack(output.getLeft(), 1, output.getRight() == -1 ? 0 : output.getRight());
                        
                        player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "game.player.swim", 0.3F, 0.75f + player.worldObj.rand.nextFloat() * 0.5f);
                    }
                    if(player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP)player).sendContainerToPlayer(player.inventoryContainer);
                    }
                }
            }
            return null;
        }
        
    }
    
    public static class MessageEmptyBucket implements IMessage {
        
        private EntityPlayer player;
        public int dim;
        public long uuidMostSig;
        public long uuidLeastSig;
        
        public MessageEmptyBucket(EntityPlayer player) {
            this.player = player;
        }
        
        public MessageEmptyBucket() {
            
        }
        
        @Override
        public void fromBytes(ByteBuf buf) {
            dim = buf.readInt();
            uuidMostSig = buf.readLong();
            uuidLeastSig = buf.readLong();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(player.dimension);
            buf.writeLong(player.getUniqueID().getMostSignificantBits());
            buf.writeLong(player.getUniqueID().getLeastSignificantBits());
        }
        
    }
    
}
