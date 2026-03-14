package cn.ksmcbrigade.hws.platform;

import cn.ksmcbrigade.hws.platform.services.IPlatformHelper;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
