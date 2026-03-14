package cn.ksmcbrigade.hws.handlers;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.HTcpWebServerModMain;
import cn.ksmcbrigade.hws.event.ReceiveHttpRequestEvent;
import cn.ksmcbrigade.hws.utils.HttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static cn.ksmcbrigade.hws.utils.HttpUtils.getFileList;
import static cn.ksmcbrigade.hws.utils.HttpUtils.isHttpRequest;
import static java.nio.charset.StandardCharsets.UTF_8;

public class HTCPHandler extends ChannelInboundHandlerAdapter {

    private final List<String> indexes;

    private final String webDir;

    public HTCPHandler(String webDir,List<String> indexes) throws IOException {
        this.webDir = webDir;
        this.indexes = indexes;

        File webD = new File(webDir);
        while(!webD.exists()) webD.mkdirs();
        if(!indexes.isEmpty()){
            boolean noIndex = true;
            for (String index : indexes) {
                if(webD.toPath().resolve(index).toFile().exists()){
                    noIndex = false;
                    break;
                }
            }
            if(noIndex){
                FileUtils.writeStringToFile(
                        webD.toPath().resolve(indexes.getFirst()).toFile(),
                        "<p>Hello HTcp Minecraft Server World!</p>",
                        UTF_8);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf buf) {

            buf.markReaderIndex();

            if (isHttpRequest(buf)) {
                HttpUtils.FileInfo context = getFile(buf);
                buf.resetReaderIndex();
                handleHttpRequest(ctx, buf,context);
                return;
            } else {
                buf.resetReaderIndex();
            }
        }

        super.channelRead(ctx, msg);
    }

    public HttpUtils.FileInfo getFile(ByteBuf byteBuf) throws IOException {
        String ref = "";
        String originalInfo = "";
        for (String s : HttpUtils.toString(byteBuf).split("\n")) {
            if(s.toUpperCase().startsWith("GET ")){
                String requestLine = s.substring(4).replace(" HTTP/1.1","");
                originalInfo = requestLine;
                int queryIndex = requestLine.indexOf('?');
                if (queryIndex != -1) {
                    requestLine = requestLine.substring(0, queryIndex);
                }
                ref = normallyString(URLDecoder.decode(requestLine, UTF_8));
                break;
            }
        }

        File file = new File(System.getProperty("user.dir")+"/"+webDir+ref);

        ReceiveHttpRequestEvent event = new ReceiveHttpRequestEvent(originalInfo,file,true);
        HTcpWebServerModMain.EVENT_BUS.post(event);
        if(event.returnInfo!=null) return event.returnInfo;

        file = event.getRequestFileOrDir();

        if(file==null) return new HttpUtils.FileInfo(null,createResponseHtml(404).getBytes(UTF_8));

        ReceiveHttpRequestEvent event2 = new ReceiveHttpRequestEvent(originalInfo,file,false);
        HTcpWebServerModMain.EVENT_BUS.post(event2);
        if(event2.returnInfo!=null) return event2.returnInfo;

        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files!=null){
                for (File file1 : files) {
                    if(indexes.contains(file1.getName().toLowerCase())){
                        if(HttpUtils.isText(file1.getName())){
                            return new HttpUtils.FileInfo(file1, Files.readString(file1.toPath(), UTF_8).getBytes(UTF_8));
                        }
                        else{
                            return new HttpUtils.FileInfo(file1, FileUtils.readFileToByteArray(file1));
                        }
                    }
                }
                return new HttpUtils.FileInfo(null,getFileList(files,webDir).getBytes(UTF_8));
            }
            else{
                return new HttpUtils.FileInfo(null,createResponseHtml(404).getBytes(UTF_8));
            }
        }
        else if(file.exists()){
            if(HttpUtils.isText(file.getName())){
                return new HttpUtils.FileInfo(file, Files.readString(file.toPath(), UTF_8).getBytes(UTF_8));
            }
            else{
                return new HttpUtils.FileInfo(file, FileUtils.readFileToByteArray(file));
            }
        }
        else{
            return new HttpUtils.FileInfo(null,createResponseHtml(404).getBytes(UTF_8));
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, ByteBuf buf,HttpUtils.FileInfo context) {
        try {
            ReferenceCountUtil.release(buf);

            byte[] contentBytes = context.context();
            String type = "text/html";
            if(context.file()!=null){
                type = HttpUtils.getContentType(context.file().getName());
            }

            String headerBuilder = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + type + ";charset=utf-8" + "\r\n" +
                    "Content-Length: " + contentBytes.length + "\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Server: Minecraft-Server\r\n" +
                    "Cache-Control: public, max-age=1800\r\n" +
                    "\r\n";

            ByteBuf responseBuf = Unpooled.buffer();
            responseBuf.writeBytes(headerBuilder.getBytes(StandardCharsets.ISO_8859_1));

            responseBuf.writeBytes(contentBytes);

            ctx.writeAndFlush(responseBuf)
                    .addListener((ChannelFutureListener) future -> {
                        try {
                            future.channel().close();
                        } catch (Exception e) {
                            Constants.LOG.debug("Failed to close the http channel.",e);
                        }
                    });

        } catch (Exception e) {
            Constants.LOG.error("Error handling HTTP request", e);
            ctx.close();
        }
    }

    public static String createResponseHtml(int errorCode){
        return createResponseHtml("ERROR "+errorCode);
    }

    public static String createResponseHtml(String info) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Minecraft Web Server</title>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<h1>" + info + "</h1>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Constants.LOG.debug("Error in handling http request(s): {}", cause.getMessage());
        ctx.close();
    }

    public static String normallyString(String s){
        StringBuilder builder = new StringBuilder();
        boolean f = true;
        for (char c : s.toCharArray()) {
            if(c==':' && System.getProperty("os.name").toLowerCase().contains("windows") && f){
                f = false;
                builder.append(c);
                continue;
            }
            if(!isInvalidPathChar(c)) builder.append(c);
        }
        return builder.toString();
    }

    private static final String reservedChars = "<>:\"|?*";
    private static boolean isInvalidPathChar(char ch) {
        return ch < ' ' || reservedChars.indexOf(ch) != -1;
    }
}
