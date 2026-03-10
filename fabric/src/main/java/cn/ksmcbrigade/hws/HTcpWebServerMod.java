package cn.ksmcbrigade.hws;

import net.fabricmc.api.DedicatedServerModInitializer;

public class HTcpWebServerMod implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        HTcpWebServerModMain.init();
    }
}
