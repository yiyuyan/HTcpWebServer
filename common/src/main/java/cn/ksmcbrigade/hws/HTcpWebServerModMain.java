package cn.ksmcbrigade.hws;

import cn.ksmcbrigade.hws.event.handlers.BlockEventHandler;
import cn.ksmcbrigade.hws.event.handlers.PHPEventHandler;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class HTcpWebServerModMain {

    public static final EventBus EVENT_BUS = new EventBus(Constants.MOD_ID);

    public static String webDir = "web";
    public static List<String> indexes = List.of(
            "index.htm",
            "index.html",
            "index.shtml"
    );

    public static void init() {
        Constants.LOG.info("Hello to {}",Constants.MOD_NAME);
        BlockEventHandler.init();
        PHPEventHandler.init();
    }

    public static void genConfig() throws IOException {
        File configDir = new File("config");
        while(!configDir.exists()) configDir.mkdirs();
        File configFile = new File("config/HTcp-config.json");

        if(!configFile.exists()){
            JsonObject object = new JsonObject();
            JsonArray array = new JsonArray();
            for (String index : indexes) array.add(index);

            object.addProperty("webDir",webDir);
            object.add("indexes",array);

            FileUtils.writeStringToFile(configFile,object.toString(), Charset.defaultCharset());
        }

        JsonObject object = JsonParser.parseString(FileUtils.readFileToString(configFile,Charset.defaultCharset())).getAsJsonObject();
        if(object.has("webDir")) webDir = object.get("webDir").getAsString();
        if(object.has("indexes")){
            JsonElement element = object.get("indexes");
            if(element instanceof JsonArray array){
                indexes = new ArrayList<>();
                for (JsonElement jsonElement : array) {
                    indexes.add(jsonElement.getAsString());
                }
            }
        }
    }
}
