package cn.ksmcbrigade.hws.event.handlers;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.HTcpWebServerModMain;
import cn.ksmcbrigade.hws.event.ReceiveHttpRequestEvent;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public record BlockEventHandler(Map<String, Integer> list) {

    private static final File CONFIG_FILE = new File("config/HTcp-errorCodeList.json");

    public static void init() {
        try {
            if (!CONFIG_FILE.exists()) FileUtils.writeStringToFile(CONFIG_FILE, "{}", Charset.defaultCharset());
            HashMap<String, Integer> list = new HashMap<>();
            JsonObject object = JsonParser.parseString(FileUtils.readFileToString(CONFIG_FILE,Charset.defaultCharset())).getAsJsonObject();
            for (String s : object.keySet()) {
                try {
                    list.put(s, object.get(s).getAsInt());
                } catch (Exception e) {
                    Constants.LOG.error("Failed to parse {} in {}.", s, CONFIG_FILE.getName());
                }
            }

            HTcpWebServerModMain.EVENT_BUS.register(new BlockEventHandler(list));
        } catch (IOException e) {
            Constants.LOG.error("Failed to init the {}", BlockEventHandler.class.getSimpleName(),e);
        }
    }

    @Subscribe
    public void onReceiveRequest(ReceiveHttpRequestEvent event) {
        if(event.isRedirected()) return;
        for (String s : list.keySet()) {
            if (s.equals(event.originalGetUrl)) {
                event.setErrorInfo(list.get(s));
                break;
            } else {
                try {
                    Pattern pattern = Pattern.compile(s);
                    if (pattern.matcher(event.originalGetUrl).matches()) {
                        event.setErrorInfo(list.get(s));
                        break;
                    }
                } catch (Exception e) {
                    //not a pattern
                }
            }
        }
    }
}
