package cn.ksmcbrigade.hws.event.handlers;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.HTcpWebServerModMain;
import cn.ksmcbrigade.hws.event.ReceiveHttpRequestEvent;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static cn.ksmcbrigade.hws.HTcpWebServerModMain.webDir;

public record RedirectEventHandler(Map<String,String> redirectMap) {

    public static void init() {
        try {
            File config = new File("config/HTcp-redirect.json");
            if (!config.exists()) {
                FileUtils.writeStringToFile(config, "{}", StandardCharsets.UTF_8);
            }

            JsonObject jsonMap = JsonParser.parseString(
                    FileUtils.readFileToString(config, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            Map<String,String> map = new HashMap<>();

            for (String s : jsonMap.keySet()) {
                map.put(s,jsonMap.get(s).getAsString());
            }

            HTcpWebServerModMain.EVENT_BUS.register(new RedirectEventHandler(map));
        } catch (IOException e) {
            Constants.LOG.error("Failed to init the {},because: {}", PHPEventHandler.class.getSimpleName(), e.getMessage());
        }
    }

    @Subscribe
    public void onReceiveRequest(ReceiveHttpRequestEvent event) {
        if(event.isCanNOTBeRedirected()) return;
        for (String s : redirectMap.keySet()) {
            if(event.originalGetUrl.equals(s)){
                event.redirectTo(new File(
                        System.getProperty("user.dir") +
                                "/" +
                                webDir +
                                redirectMap.get(s)));
                return;
            }
        }
    }
}
