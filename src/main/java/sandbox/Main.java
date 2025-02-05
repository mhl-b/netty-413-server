package sandbox;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        var bg = new NioEventLoopGroup(1);
        var wg = new NioEventLoopGroup();
        try {
            var b = new ServerBootstrap().group(bg, wg)
                                         .channel(NioServerSocketChannel.class)
                                         .childHandler(new HttpPipeline());
            var ch =b.bind(9091).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bg.shutdownGracefully();
            wg.shutdownGracefully();
        }
    }
}

class HttpPipeline extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        var p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpMsgHandler());
    }
}

class HttpMsgHandler extends SimpleChannelInboundHandler<HttpObject> {
    static final FullHttpResponse TOO_LARGE_CLOSE = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
            Unpooled.EMPTY_BUFFER,
            new DefaultHttpHeaders().add(CONTENT_LENGTH, 0).add(CONNECTION, HttpHeaderValues.CLOSE),
            EmptyHttpHeaders.INSTANCE
    );

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest req) {
            System.out.format("request method=%s uri=%s len=%d", req.method(), req.uri(),
                              HttpUtil.getContentLength(req, 0));
            ctx.writeAndFlush(TOO_LARGE_CLOSE).addListener(ChannelFutureListener.CLOSE);
        }
    }
}