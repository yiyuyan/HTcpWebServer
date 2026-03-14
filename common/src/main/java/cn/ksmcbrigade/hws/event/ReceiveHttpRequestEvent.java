package cn.ksmcbrigade.hws.event;

import cn.ksmcbrigade.hws.handlers.HTCPHandler;
import cn.ksmcbrigade.hws.utils.HttpUtils;
import com.google.gson.JsonObject;

import java.io.File;

public class ReceiveHttpRequestEvent {
    public final String originalGetInfo;
    public final String originalGetUrl;
    public final String originalGetArgs;

    public final JsonObject getArgs;

    private File requestFileOrDir;

    public HttpUtils.FileInfo returnInfo = null;

    private boolean redirected = false;
    private final boolean canBeRedirected;

    public ReceiveHttpRequestEvent(String originalGetInfo, File fileOrDir,boolean canBeRedirected){
        this.originalGetInfo = originalGetInfo;
        this.requestFileOrDir = fileOrDir;

        String[] infos = originalGetInfo.split("\\?");
        originalGetUrl = HTCPHandler.normallyString(infos[0]);
        getArgs = new JsonObject();

        if(infos.length>=2){
            originalGetArgs = infos[1].substring(0,infos[1].length()-1);
            for (String s : originalGetArgs.split("&")) {
                String[] info = s.split("=");
                if(info.length>=2) getArgs.addProperty(info[0],info[1]);
            }
        }
        else{
            originalGetArgs = "";
        }

        this.canBeRedirected = canBeRedirected;
    }

    public void setReturnInfo(HttpUtils.FileInfo returnInfo) {
        this.returnInfo = returnInfo;
    }

    public void setReturnInfo(String info){
        this.setReturnInfo(info.getBytes());
    }

    public void setReturnInfo(byte[] bytes){
        this.setReturnInfo(new HttpUtils.FileInfo(null,bytes));
    }

    public void setErrorInfo(int errorCode){
        this.setReturnInfo(HTCPHandler.createResponseHtml(errorCode));
    }

    public void setErrorInfo(String errorInfo){
        this.setReturnInfo(HTCPHandler.createResponseHtml(errorInfo));
    }

    public void redirectTo(File file){
        if(isCanNOTBeRedirected()) return;
        this.requestFileOrDir = file;
        this.redirected = true;
    }

    public File getRequestFileOrDir() {
        return requestFileOrDir;
    }

    public boolean isRedirected() {
        return redirected;
    }

    public boolean isCanNOTBeRedirected() {
        return !canBeRedirected;
    }
}
