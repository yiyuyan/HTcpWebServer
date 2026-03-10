package cn.ksmcbrigade.hws.event.handlers;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.HTcpWebServerModMain;
import cn.ksmcbrigade.hws.event.ReceiveHttpRequestEvent;
import com.google.common.eventbus.Subscribe;

import java.io.*;
import java.util.Map;

public class PHPEventHandler {
    public static void init() {
        try {
            boolean phpSupport = false;
            ProcessBuilder processBuilder = new ProcessBuilder("php","-v");
            Process process = processBuilder.start();
            process.wait();
            BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if(output.readLine().contains("PHP ")) phpSupport = true;
            if(!phpSupport){
                Constants.LOG.warn("Can't find PHP. Please make sure that PHP has been added to the PATH.");
                return;
            }
            HTcpWebServerModMain.EVENT_BUS.register(new PHPEventHandler());
        } catch (IOException | InterruptedException e) {
            Constants.LOG.error("Failed to init the {},because: {}",PHPEventHandler.class.getSimpleName(),e.getMessage());
        }
    }

    @Subscribe
    public void onReceiveRequest(ReceiveHttpRequestEvent event) throws IOException, InterruptedException {
        if(event.originalGetUrl.toLowerCase().endsWith(".php") &&
                !event.requestFileOrDir.isDirectory()
        && event.requestFileOrDir.exists()){
            ProcessBuilder processBuilder = new ProcessBuilder("php", event.requestFileOrDir.getAbsolutePath());
            Map<String, String> env = processBuilder.environment();
            env.put("QUERY_STRING",event.originalGetArgs);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();

            if(exitCode==0){
                event.setReturnInfo(output.toString());
            }
            else{
                event.setErrorInfo("PHP ERROR "+exitCode);
            }
        }
    }
}
