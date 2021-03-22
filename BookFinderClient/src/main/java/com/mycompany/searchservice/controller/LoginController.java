package com.mycompany.searchservice.controller;

import com.mycompany.searchservice.SearchClientHandler;
import com.mycompany.searchservice.dto.Code;
import com.mycompany.searchservice.dto.TextMessage;
import com.mycompany.searchservice.dto.User;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private final static String HOST = "127.0.0.1";
    private final static int PORT = 8080;
    private Channel channel;
    private EventLoopGroup eventLoopGroup;
    private final SearchClientHandler searchClientHandler = new SearchClientHandler();

    @FXML private TextField username;
    @FXML private TextField password;
    @FXML private Button login;
    @FXML private Button register;
    @FXML private Button remind;
    @FXML private Label loginMessage;

    private final BooleanProperty authorized = new SimpleBooleanProperty();
    private final BooleanProperty connected = new SimpleBooleanProperty();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        searchClientHandler.setAuthorized(authorized);
        searchClientHandler.setLoginMessage(loginMessage);

        connect();

        login.setOnAction(actionEvent ->
                sendCredentials(new User(username.getText(), password.getText(), User.Type.LOGIN)));
        register.setOnAction(actionEvent ->
                sendCredentials(new User(username.getText(), password.getText(), User.Type.REGISTER)));
        remind.setOnAction(actionEvent ->
                sendCredentials(new User(username.getText(), null, User.Type.REMIND)));
        authorized.addListener(observable -> {
            try {
                openApplicationWindow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void openApplicationWindow() throws IOException {
        username.getScene().getWindow().hide();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/application.fxml"));
        Parent root = fxmlLoader.load();
        Controller applicationController = fxmlLoader.getController();
        applicationController.initController(channel, eventLoopGroup, searchClientHandler);
        Scene scene = new Scene(root);
        Stage window = (Stage) username.getScene().getWindow();
        window.setScene(scene);
        window.setOnCloseRequest(event -> {
            event.consume();
            disconnect();
            Platform.exit();
        });
        window.setResizable(false);
        window.show();
    }

    @FXML
    public void connect() {
        if (connected.get()) {
            return;
        }
        eventLoopGroup = new NioEventLoopGroup();
        Task<Channel> task = new Task<>() {
            @Override
            protected Channel call() throws Exception {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel channel) {
                                ChannelPipeline channelPipeline = channel.pipeline();
                                channelPipeline.addLast(
                                        sslContext.newHandler(channel.alloc(), HOST, PORT),
                                        new ObjectDecoder(ClassResolvers.cacheDisabled(getClass().getClassLoader())),
                                        new ObjectEncoder(),
                                        searchClientHandler);
                            }
                        });
                ChannelFuture channelFuture = bootstrap.connect(HOST, PORT);
                Channel channel = channelFuture.channel();
                channelFuture.sync();
                return channel;
            }

            @Override
            protected void succeeded() {
                channel = getValue();
                connected.set(true);
            }

            @Override
            protected void failed() {
                loginMessage.setText("Cannot connect to the server");
                connected.set(false);
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void sendCredentials(User user) {
        if (!connected.get()) {
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ChannelFuture channelFuture = channel.writeAndFlush(user);
                channelFuture.sync();
                return null;
            }

            @Override
            protected void failed() {
                connected.set(false);
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void disconnect() {
        if (!connected.get()) {
            eventLoopGroup.shutdownGracefully();
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                channel.writeAndFlush(new TextMessage(Code.EXIT, "Exit"));
                channel.close().sync();
                eventLoopGroup.shutdownGracefully();
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
