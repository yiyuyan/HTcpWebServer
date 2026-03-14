package cn.ksmcbrigade.hws.platform;

import cn.ksmcbrigade.hws.platform.services.IPlatformHelper;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
