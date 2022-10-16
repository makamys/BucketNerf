package makamys.bucketnerf;

import static makamys.bucketnerf.BucketNerf.MODID;
import static makamys.bucketnerf.BucketNerf.LOGGER;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.config.Property.Type;

public class Config {

    @ConfigBoolean(cat="waterBucket", def=true, com="Enable water bucket nerf. When using a water bucket, it will turn into an empty bucket without placing a water block.")
    public static boolean _enableWaterBucketNerf;
    @ConfigStringList(cat="waterBucket", def={"# Vanilla bucket", "minecraft:water_bucket minecraft:bucket", "", "# GT6 wooden water buckets", "gregtech:gt.multiitem.randomtools:2001 gregtech:gt.multiitem.randomtools:2000", "gregtech:gt.multiitem.randomtools:2101 gregtech:gt.multiitem.randomtools:2100", "gregtech:gt.multiitem.randomtools:2201 gregtech:gt.multiitem.randomtools:2200", "gregtech:gt.multiitem.randomtools:2301 gregtech:gt.multiitem.randomtools:2300", "gregtech:gt.multiitem.randomtools:2401 gregtech:gt.multiitem.randomtools:2400", "gregtech:gt.multiitem.randomtools:2501 gregtech:gt.multiitem.randomtools:2500", "gregtech:gt.multiitem.randomtools:2601 gregtech:gt.multiitem.randomtools:2600", "gregtech:gt.multiitem.randomtools:2701 gregtech:gt.multiitem.randomtools:2700", "gregtech:gt.multiitem.randomtools:2801 gregtech:gt.multiitem.randomtools:2800"}, com="Each line is a 'recipe' describing what will happen when you use a bucket. The item on the left side is the input (the item you're right clicking), and the item on the right side is the output (what the item turns into once used). Lines starting with # and empty lines are ignored.")
    public static String[] bucketUsageRecipes;
    
    @ConfigBoolean(cat="tameableArachne", def=true, com="Enable Tameable Arachne module. Adds a random cooldown after milking a monster girl.")
    public static boolean _enableTameableArachne;
    @ConfigInt(cat="tameableArachne", def=20*60*20, min=0, max=Integer.MAX_VALUE, com="Minimum cooldown time (in ticks) of small arachne (tameArachne)")
    public static int tameArachneMilkCooldownMin;
    @ConfigInt(cat="tameableArachne", def=30*60*20, min=0, max=Integer.MAX_VALUE, com="Maximum cooldown time (in ticks) of small arachne (tameArachne)")
    public static int tameArachneMilkCooldownMax;
    @ConfigInt(cat="tameableArachne", def=10*60*20, min=0, max=Integer.MAX_VALUE, com="Minimum cooldown time (in ticks) of large arachne (tameArachneMedium)")
    public static int tameArachneMediumMilkCooldownMin;
    @ConfigInt(cat="tameableArachne", def=20*60*20, min=0, max=Integer.MAX_VALUE, com="Maximum cooldown time (in ticks) of large arachne (tameArachneMedium)")
    public static int tameArachneMediumMilkCooldownMax;
    @ConfigInt(cat="tameableArachne", def=30*60*20, min=0, max=Integer.MAX_VALUE, com="Minimum cooldown time (in ticks) of harpy (tameHarpy)")
    public static int tameHarpyMilkCooldownMin;
    @ConfigInt(cat="tameableArachne", def=40*60*20, min=0, max=Integer.MAX_VALUE, com="Maximum cooldown time (in ticks) of harpy (tameHarpy)")
    public static int tameHarpyMilkCooldownMax;
    @ConfigInt(cat="tameableArachne", def=29, min=0, max=32, com="DataWatcher ID for tracking next milk time.")
    public static int nextMilkTimeDataID;
    
    private static Configuration config;
    private static File configFile = new File(Launch.minecraftHome, "config/" + MODID + ".cfg");

    
    public static void reloadConfig() {
        config = new Configuration(configFile);
        
        config.load();
        loadFields(config);
        
        if(config.hasChanged()) {
            config.save();
        }
    }
    
    private static boolean loadFields(Configuration config) {
        boolean needReload = false;
        
        for(Field field : Config.class.getFields()) {
            if(!Modifier.isStatic(field.getModifiers())) continue;
            
            NeedsReload needsReload = null;
            ConfigBoolean configBoolean = null;
            ConfigInt configInt = null;
            ConfigEnum configEnum = null;
            ConfigStringList configStringList = null;
            
            for(Annotation an : field.getAnnotations()) {
                if(an instanceof NeedsReload) {
                    needsReload = (NeedsReload) an;
                } else if(an instanceof ConfigInt) {
                    configInt = (ConfigInt) an;
                } else if(an instanceof ConfigBoolean) {
                    configBoolean = (ConfigBoolean) an;
                } else if(an instanceof ConfigEnum) {
                    configEnum = (ConfigEnum) an;
                } else if(an instanceof ConfigStringList) {
                    configStringList = (ConfigStringList) an;
                }
            }
            
            if(configBoolean == null && configInt == null && configEnum == null && configStringList == null) continue;
            
            Object currentValue = null;
            Object newValue = null;
            try {
                currentValue = field.get(null);
            } catch (Exception e) {
                LOGGER.error("Failed to get value of field " + field.getName());
                e.printStackTrace();
                continue;
            }
            
            if(configBoolean != null) {
                newValue = config.getBoolean(field.getName(), configBoolean.cat(), configBoolean.def(), configBoolean.com());
            } else if(configInt != null) {
                newValue = config.getInt(field.getName(), configInt.cat(), configInt.def(), configInt.min(), configInt.max(), configInt.com()); 
            } else if(configEnum != null) {
                boolean lowerCase = true;
                
                Class<? extends Enum> configClass = configEnum.clazz();
                Map<String, ? extends Enum> enumMap = EnumUtils.getEnumMap(configClass);
                String[] valuesStrUpper = (String[])enumMap.keySet().stream().toArray(String[]::new);
                String[] valuesStr = Arrays.stream(valuesStrUpper).map(s -> lowerCase ? s.toLowerCase() : s).toArray(String[]::new);
                
                // allow upgrading boolean to string list
                ConfigCategory cat = config.getCategory(configEnum.cat());
                Property oldProp = cat.get(field.getName());
                String oldVal = null;
                if(oldProp != null && oldProp.getType() != Type.STRING) {
                    oldVal = oldProp.getString();
                    cat.remove(field.getName());
                }
                
                String newValueStr = config.getString(field.getName(), configEnum.cat(),
                        lowerCase ? configEnum.def().toLowerCase() : configEnum.def().toUpperCase(), configEnum.com(), valuesStr);
                if(oldVal != null) {
                    newValueStr = oldVal;
                }
                if(!enumMap.containsKey(newValueStr.toUpperCase())) {
                    newValueStr = configEnum.def().toUpperCase();
                    if(lowerCase) {
                        newValueStr = newValueStr.toLowerCase();
                    }
                }
                newValue = enumMap.get(newValueStr.toUpperCase());
                
                Property newProp = cat.get(field.getName());
                if(!newProp.getString().equals(newValueStr)) {
                    newProp.set(newValueStr);
                }
            } else if(configStringList != null) {
                newValue = config.getStringList(field.getName(), configStringList.cat(), configStringList.def(), configStringList.com());
            }
            
            if(needsReload != null && !newValue.equals(currentValue)) {
                needReload = true;
            }
            
            try {
                field.set(null, newValue);
            } catch (Exception e) {
                LOGGER.error("Failed to set value of field " + field.getName());
                e.printStackTrace();
            }
        }
        
        return needReload;
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface NeedsReload {

    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface ConfigBoolean {

        String cat();
        boolean def();
        String com() default "";

    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface ConfigInt {

        String cat();
        int min();
        int max();
        int def();
        String com() default "";

    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface ConfigStringList {

        String cat();
        String[] def();
        String com() default "";

    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface ConfigEnum {

        String cat();
        String def();
        String com() default "";
        Class<? extends Enum> clazz();

    }
    
}


