package tizio.dev.lle.core;

import com.mojang.logging.LogUtils;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;


@Mod(MainClass.MODID)
public class MainClass {

    public static final String MODID = "lle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MainClass() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

    }

}