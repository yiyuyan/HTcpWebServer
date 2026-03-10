package cn.ksmcbrigade.hws;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod(Constants.MOD_ID)
public class HTcpWebServerMod {

    public HTcpWebServerMod(IEventBus eventBus) {
        if(FMLLoader.getDist().isDedicatedServer())HTcpWebServerModMain.init();
    }
}
