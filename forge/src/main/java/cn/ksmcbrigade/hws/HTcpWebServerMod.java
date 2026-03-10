package cn.ksmcbrigade.hws;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(Constants.MOD_ID)
public class HTcpWebServerMod {

    public HTcpWebServerMod() {
        if(FMLLoader.getDist().isDedicatedServer())HTcpWebServerModMain.init();
    }
}
