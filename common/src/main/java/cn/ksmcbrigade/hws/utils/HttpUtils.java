package cn.ksmcbrigade.hws.utils;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    // === HTTP REQUEST DECIDER

    public static boolean isHttpRequest(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        if (readableBytes < 14) {
            return false;
        }

        int readerIndex = buf.readerIndex();

        byte firstByte = buf.getByte(readerIndex);
        firstByte |= 0x20; // To lowest

        int methodStart;
        if (firstByte == 'g') {
            if ((buf.getByte(readerIndex + 1) | 0x20) != 'e') return false;
            if ((buf.getByte(readerIndex + 2) | 0x20) != 't') return false;
            if (buf.getByte(readerIndex + 3) != ' ') return false;
            methodStart = readerIndex + 4;
        } else if (firstByte == 'p') {
            if ((buf.getByte(readerIndex + 1) | 0x20) != 'o') return false;
            if ((buf.getByte(readerIndex + 2) | 0x20) != 's') return false;
            if ((buf.getByte(readerIndex + 3) | 0x20) != 't') return false;
            if (buf.getByte(readerIndex + 4) != ' ') return false;
            methodStart = readerIndex + 5;
        } else {
            return false;
        }

        return findHttp11Fast(buf, methodStart, readerIndex + readableBytes);
    }

    private static boolean findHttp11Fast(ByteBuf buf, int start, int end) {
        final byte[] PATTERN = {'H', 'T', 'T', 'P', '/', '1', '.', '1'};
        final byte[] PATTERN_LOWER = {'h', 't', 't', 'p', '/', '1', '.', '1'};

        for (int i = start; i <= end - 8; i++) {
            byte b = buf.getByte(i);
            if (b != 'H' && b != 'h') continue;

            boolean match = true;
            for (int j = 1; j < 8; j++) {
                byte current = buf.getByte(i + j);
                byte expected = PATTERN[j];
                byte expectedLower = PATTERN_LOWER[j];

                if (current != expected && current != expectedLower) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return true;
            }
        }

        return false;
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
