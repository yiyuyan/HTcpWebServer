package cn.ksmcbrigade.hws.platform;

import cn.ksmcbrigade.hws.platform.services.IPlatformHelper;
import net.fabricmc.loader.impl.FabricLoaderImpl;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment();
    }
}
