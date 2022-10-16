package makamys.bucketnerf;

import net.minecraft.command.ICommandSender;

import net.minecraft.command.CommandBase;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class BucketNerfCommand extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "bucketnerf";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }
    
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if(args.length == 1) {
            if(args[0].equals("reload")) {
                Config.reloadConfig();
                BucketNerf.parseConfig();
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "Reloaded config."));
                return;
            }
        }
        throw new WrongUsageException("bucketnerf reload");
    }
    
}
