package cn.ksmcbrigade.hws.platform;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.platform.services.IPlatformHelper;

import java.io.File;
import java.util.ServiceLoader;

public class Services {

    public static final IPlatformHelper SERVICE = load(IPlatformHelper.class);

     public static <T> T load(Class<T> clazz) {
            final T loadedService = ServiceLoader.load(clazz).findFirst().orElse((T) new IPlatformHelper(){

                @Override
                public boolean isDevelopmentEnvironment() {
                    File file = new File(System.getProperty("user.dir"));
                    if(file.getName().equals("run")) return true;
                    return new File(System.getProperty("user.dir")).getParentFile().getParentFile().getName().equals("run");
                }

            });
            Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
            return loadedService;
     }
}
