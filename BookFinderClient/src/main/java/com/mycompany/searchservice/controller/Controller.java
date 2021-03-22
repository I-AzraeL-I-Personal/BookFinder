package com.mycompany.searchservice.controller;

import com.mycompany.searchservice.SearchClientHandler;
import com.mycompany.searchservice.dto.ClientRequest;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class Controller implements Initializable {

    private Channel channel;
    private EventLoopGroup eventLoopGroup;

    private final BooleanProperty connected = new SimpleBooleanProperty(true);

    @FXML private TextField author;
    @FXML private TextField genre;
    @FXML private TextField title;
    @FXML private TextField description;
    @FXML private Button search;
    @FXML private ListView<String> list;
    @FXML private Label applicationMessage;

    public void initController(Channel channel, EventLoopGroup eventLoopGroup, SearchClientHandler searchClientHandler) {
        this.channel = channel;
        this.eventLoopGroup = eventLoopGroup;
        searchClientHandler.setList(list);
        searchClientHandler.setApplicationMessage(applicationMessage);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        search.setOnAction(actionEvent -> {
            Map<String, String> phrases = new TreeMap<>();
            phrases.put("author", author.getText());
            phrases.put("genre", genre.getText());
            phrases.put("title", title.getText());
            phrases.put("description", description.getText());
            sendRequest(new ClientRequest(phrases));
        });
    }

    @FXML
    public void sendRequest(ClientRequest clientRequest) {
        if (!connected.get()) {
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ChannelFuture channelFuture = channel.writeAndFlush(clientRequest);
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
