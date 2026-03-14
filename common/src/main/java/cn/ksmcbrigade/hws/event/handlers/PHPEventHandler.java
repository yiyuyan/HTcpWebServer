package cn.ksmcbrigade.hws.event.handlers;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.HTcpWebServerModMain;
import cn.ksmcbrigade.hws.event.ReceiveHttpRequestEvent;
import cn.ksmcbrigade.hws.platform.Services;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public record PHPEventHandler(String phpFile,boolean developmentEnv) {

    public static void init() {
        try {
            File config = new File("config/HTcp-php.json");
            if (!config.exists()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("phpPath", "php");
                obj.addProperty("forceDevelopment",false);
                FileUtils.writeStringToFile(config, obj.toString(), StandardCharsets.UTF_8);
            }

            JsonObject phpJson = JsonParser.parseString(
                    FileUtils.readFileToString(config, StandardCharsets.UTF_8)
            ).getAsJsonObject();


            String phpFile = "php";
            boolean development = Services.SERVICE.isDevelopmentEnvironment();

            if(phpJson.has("phpPath")) phpFile = phpJson.get("phpPath").getAsString();
            if(phpJson.has("forceDevelopment")) development = phpJson.get("forceDevelopment").getAsBoolean();

            boolean phpSupport = false;
            ProcessBuilder processBuilder = new ProcessBuilder(
                    phpFile
                    , "-v");
            Process process = processBuilder.start();
            process.waitFor();
            BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (output.readLine().contains("PHP ")) phpSupport = true;
            if (!phpSupport) {
                Constants.LOG.warn("Can't find PHP.");
                return;
            }
            HTcpWebServerModMain.EVENT_BUS.register(new PHPEventHandler(phpFile,development));

            Constants.LOG.info("PHPHandler loaded.");

        } catch (IOException | InterruptedException e) {
            Constants.LOG.error("Failed to init the {},because: {}", PHPEventHandler.class.getSimpleName(), e.getMessage());
        }
    }

    @Subscribe
    public void onReceiveRequest(ReceiveHttpRequestEvent event) throws IOException, InterruptedException {
        if(event.isRedirected()) return;
        if (event.originalGetUrl.toLowerCase().endsWith(".php") &&
                !event.getRequestFileOrDir().isDirectory()
                && event.getRequestFileOrDir().exists()) {
            ProcessBuilder processBuilder = getProcessBuilder(event);

            Map<String, String> env = processBuilder.environment();

            env.put("QUERY_STRING", event.originalGetArgs);
            String parsedArgsJson = new Gson().toJson(event.getArgs);
            env.put("PARSED_QUERY_JSON", parsedArgsJson);

            for (String s : event.getArgs.keySet()) {
                String value = event.getArgs.get(s).getAsString();
                env.put(s,value);
                env.put("QUERY_PARAM_"+s,value);
            }

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                event.setReturnInfo(output.toString());
            } else {
                event.setErrorInfo("PHP ERROR " + exitCode);
            }
        }
    }

    private @NotNull ProcessBuilder getProcessBuilder(ReceiveHttpRequestEvent event) {
        ProcessBuilder processBuilder;

        if(developmentEnv){
            processBuilder = new ProcessBuilder(
                    phpFile,
                    "-d", "error_reporting=E_ALL",
                    "-d", "display_errors=1",
                    "-d", "display_startup_errors=1",
                    event.getRequestFileOrDir().getAbsolutePath()
            );
        }
        else{
            processBuilder = new ProcessBuilder(
                    phpFile,
                    "-d", "error_reporting=E_ALL & ~E_WARNING & ~E_NOTICE & ~E_DEPRECATED",
                    "-d", "display_errors=0",
                    "-d", "log_errors=1",
                    "-d", "error_log=log/php-errors.log",
                    event.getRequestFileOrDir().getAbsolutePath()
            );
        }
        return processBuilder;
    }
}
