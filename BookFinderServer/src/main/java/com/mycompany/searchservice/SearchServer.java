package com.mycompany.searchservice;

import com.mycompany.searchservice.dto.Users;
import com.mycompany.searchservice.util.XPathSearcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class SearchServer {

    private final static int DEFAULT_PORT = 8080;
    private final static String pathBooks = "src/main/resources/books.xml";
    private final static String pathDatabase = "src/main/resources/database.xml";
    private final static String pathBooksAccess = "catalog/book";

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            JAXBContext ctx = JAXBContext.newInstance(Users.class);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            File databaseFile = new File(pathDatabase);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            Document document = documentBuilder.parse(pathBooks);
            XPathSearcher bookFinder = new XPathSearcher(document, pathBooksAccess);

            SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
            SslContext sslContext = SslContextBuilder.forServer(
                    selfSignedCertificate.certificate(),
                    selfSignedCertificate.privateKey()).build();

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline channelPipeline = channel.pipeline();
                            channelPipeline.addLast(
                                    sslContext.newHandler(channel.alloc()),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(getClass().getClassLoader())),
                                    new ObjectEncoder(),
                                    new SearchServerAuthHandler(marshaller, unmarshaller, databaseFile, bookFinder));
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(DEFAULT_PORT).sync();
            System.err.println("SERVER READY");

            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Connection error. shutting down...");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
