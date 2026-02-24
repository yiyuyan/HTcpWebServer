package cn.ksmcbrigade.hws;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class HTcpWebServerMod {

    public HTcpWebServerMod(IEventBus eventBus) {
        CommonClass.init();
    }
}
