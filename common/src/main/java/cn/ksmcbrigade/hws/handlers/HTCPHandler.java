package cn.ksmcbrigade.hws.handlers;

import cn.ksmcbrigade.hws.Constants;
import cn.ksmcbrigade.hws.utils.HttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

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
                FileUtils.writeStringToFile(webD.toPath().resolve(indexes.getFirst()).toFile(),"<p>Hello HTcp Minecraft Server World!</p>", Charset.defaultCharset());
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf buf) {

            buf.markReaderIndex();

            if (isHttpRequest(buf)) {
                FileInfo context = getFile(buf);
                buf.resetReaderIndex();
                handleHttpRequest(ctx, buf,context);
                return;
            } else {
                buf.resetReaderIndex();
            }
        }

        super.channelRead(ctx, msg);
    }

    private String toString(ByteBuf byteBuf){
        return byteBuf.copy().toString(StandardCharsets.US_ASCII);
    }

    private FileInfo getFile(ByteBuf byteBuf) throws IOException {
        String ref = "";
        for (String s : toString(byteBuf).split("\n")) {
            if(s.toUpperCase().startsWith("GET ")){
                ref = s.substring(4).replace(" HTTP/1.1","");
            }
        }
        File file = new File(normally(System.getProperty("user.dir")+"/"+webDir+ref));
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files!=null){
                for (File file1 : files) {
                    if(indexes.contains(file1.getName().toLowerCase())){
                        if(HttpUtils.isText(file1.getName())){
                            return new FileInfo(null, Files.readString(file1.toPath()).getBytes());
                        }
                        else{
                            return new FileInfo(file1,FileUtils.readFileToByteArray(file1));
                        }
                    }
                }
                return new FileInfo(null,getFileList(files).getBytes());
            }
            else{
                return new FileInfo(null,createResponseHtml().getBytes());
            }
        }
        else if(file.exists()){
            if(HttpUtils.isText(file.getName())){
                return new FileInfo(null, Files.readString(file.toPath()).getBytes());
            }
            else{
                return new FileInfo(file,FileUtils.readFileToByteArray(file));
            }
        }
        else{
            return new FileInfo(null,createResponseHtml().getBytes());
        }
    }

    public String getFileList(File[] files) {
        StringBuilder builder = new StringBuilder("<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Minecraft Web Server</title>" +
                "</head>" +
                "<body>");
        for (File file : files) {
            builder.append("<a href=\"")
                    .append(file.getAbsolutePath().replace(
                            new File(
                                    System.getProperty("user.dir")+"/"+webDir)
                                    .getAbsolutePath(),"").replace(File.separatorChar,'/')
                    )
                    .append("\">")
                    .append(file.getAbsolutePath().replace(file.getParentFile().getAbsolutePath(),""))
                    .append("</a>")
                    .append("<p></p>")
                    .append("\n");
        }
        builder.append("</body>" +
                "</html>");
        return builder.toString();
    }

    private boolean isHttpRequest(ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            return false;
        }

        ByteBuf copy = buf.copy();
        try {
            String prefix = copy.toString(StandardCharsets.US_ASCII).toUpperCase();
            return prefix.startsWith("GET ") || prefix.startsWith("POST ");
        } finally {
            copy.release();
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, ByteBuf buf,FileInfo context) {
        try {
            ReferenceCountUtil.release(buf);

            byte[] contentBytes = context.context;
            String type = "text/html";
            if(context.file!=null){
                type = HttpUtils.getContentType(context.file.getName());
            }

            String headerBuilder = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + type + "\r\n" +
                    "Content-Length: " + contentBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "Server: Minecraft-Server\r\n" +
                    "\r\n";

            ByteBuf responseBuf = Unpooled.buffer();
            responseBuf.writeBytes(headerBuilder.getBytes(CharsetUtil.US_ASCII));
            responseBuf.writeBytes(contentBytes);

            ctx.writeAndFlush(responseBuf)
                    .addListener(ChannelFutureListener.CLOSE)
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            Constants.LOG.error("Failed to send HTTP response", future.cause());
                        }
                    });

        } catch (Exception e) {
            Constants.LOG.error("Error handling HTTP request", e);
            ctx.close();
        }
    }

    private String createResponseHtml() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Minecraft Web Server</title>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<h1>ERROR 404 NOT FOUND</h1>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Constants.LOG.debug("Error in handling http request(s): {}", cause.getMessage());
        ctx.close();
    }

    private String normally(String s){
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

    record FileInfo(File file,byte[] context){}
}
