package cn.ksmcbrigade.hws.utils;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    public static boolean isHttpRequest(ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            return false;
        }

        ByteBuf copy = buf.copy();
        try {
            String prefix = copy.toString(StandardCharsets.US_ASCII).toUpperCase();
            return (prefix.startsWith("GET ") || prefix.startsWith("POST ")) && prefix.contains("HTTP/1.1");
        } finally {
            copy.release();
        }
    }

    // === HTTP FILE LIST ===

    public static String toString(ByteBuf byteBuf){
        return byteBuf.copy().toString(StandardCharsets.US_ASCII);
    }

    public static String getFileList(File[] files,String web) {
        StringBuilder builder = new StringBuilder("<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<title>Minecraft Web Server</title>" +
                "</head>" +
                "<body>");
        builder.append("<li><a href=\"..\">..</a></li>\n");
        for (File file : files) {
            builder.append("<li><a href=\"")
                    .append(file.getAbsolutePath().replace(
                            new File(
                                    System.getProperty("user.dir")+"/"+web)
                                    .getAbsolutePath(),"").replace(File.separatorChar,'/')
                    )
                    .append("\">")
                    .append(file.getAbsolutePath().replace(file.getParentFile().getAbsolutePath(),""))
                    .append("</a></li>")
                    .append("\n");
        }
        builder.append("</body>" +
                "</html>");
        return builder.toString();
    }

    public record FileInfo(File file,byte[] context){}

    // === HTTP CONTENT TYPES ===

    public static String getContentType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = fileName.substring(dotIndex + 1).toLowerCase();
            switch (ext) {
                case "png": return "image/png";
                case "jpg":
                case "jpeg": return "image/jpeg";
                case "gif": return "image/gif";
                case "ico": return "image/x-icon";
                case "svg": return "image/svg+xml";
                case "webp": return "image/webp";
                case "bmp": return "image/bmp";

                case "html":
                case "htm": return "text/html";
                case "css": return "text/css";
                case "js": return "application/javascript";
                case "json": return "application/json";
                case "txt": return "text/plain";
                case "xml": return "application/xml";

                case "woff": return "font/woff";
                case "woff2": return "font/woff2";
                case "ttf": return "font/ttf";
                case "eot": return "application/vnd.ms-fontobject";

                case "mp3": return "audio/mpeg";
                case "mp4": return "video/mp4";
                case "webm": return "video/webm";
                case "ogg": return "audio/ogg";

                case "pdf": return "application/pdf";
                case "zip": return "application/zip";
                case "rar": return "application/x-rar-compressed";
                case "7z": return "application/x-7z-compressed";
            }
        }
        return "application/octet-stream";
    }

    public static boolean isText(String fileName){
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = fileName.substring(dotIndex + 1).toLowerCase();
            switch (ext) {
                case "html":
                case "htm":
                case "css":
                case "js":
                case "json":
                case "txt":
                case "xml": return true;
            }
        }
        return false;
    }
}
