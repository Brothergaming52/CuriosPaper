package org.bg52.curiospaper.resourcepack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.bg52.curiospaper.CuriosPaper;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class HttpPacketHandler extends ChannelInboundHandlerAdapter {

    private final CuriosPaper plugin;
    private final File packFile;

    public HttpPacketHandler(CuriosPaper plugin, File packFile) {
        this.plugin = plugin;
        this.packFile = packFile;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;

            // Check for HTTP GET request (G = 71, E = 69, T = 84)
            if (buf.readableBytes() > 3 && buf.getByte(buf.readerIndex()) == 'G'
                    && buf.getByte(buf.readerIndex() + 1) == 'E'
                    && buf.getByte(buf.readerIndex() + 2) == 'T') {

                // It's likely an HTTP request.
                // Remove this handler to prevent re-entry issues if we re-inject
                ctx.pipeline().remove(this);

                // Add HTTP codecs
                ctx.pipeline().addFirst("http-codec", new HttpServerCodec());
                ctx.pipeline().addAfter("http-codec", "http-chunked", new ChunkedWriteHandler());
                ctx.pipeline().addAfter("http-chunked", "http-handler", new SimpleHttpHandler(plugin, packFile));

                // Fire the message again so the new handlers pick it up
                // We need to retain the buffer because we are passing it on
                buf.retain();
                ctx.fireChannelRead(buf);
                return;
            }
        }

        // Not HTTP, pass it along and remove self
        ctx.pipeline().remove(this);
        ctx.fireChannelRead(msg);
    }

    private static class SimpleHttpHandler extends io.netty.channel.SimpleChannelInboundHandler<HttpRequest> {
        private final CuriosPaper plugin;
        private final File packFile;

        public SimpleHttpHandler(CuriosPaper plugin, File packFile) {
            this.plugin = plugin;
            this.packFile = packFile;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
            if (request.uri().equals("/pack.zip") && request.method() == HttpMethod.GET) {
                if (!packFile.exists()) {
                    sendError(ctx, HttpResponseStatus.NOT_FOUND);
                    return;
                }

                RandomAccessFile raf = new RandomAccessFile(packFile, "r");
                long fileLength = raf.length();

                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                HttpUtil.setContentLength(response, fileLength);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/zip");
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

                ctx.write(response);
                ctx.write(new io.netty.handler.stream.ChunkedFile(raf, 0, fileLength, 8192),
                        ctx.newProgressivePromise());
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                        .addListener(io.netty.channel.ChannelFutureListener.CLOSE);
            } else {
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
            }
        }

        private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status,
                    io.netty.buffer.Unpooled.copiedBuffer("Failure: " + status + "\r\n", StandardCharsets.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Ignore errors, just close
            ctx.close();
        }
    }
}