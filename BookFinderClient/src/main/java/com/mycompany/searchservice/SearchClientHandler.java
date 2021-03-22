package com.mycompany.searchservice;

import com.mycompany.searchservice.dto.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.List;

public class SearchClientHandler extends ChannelInboundHandlerAdapter implements ObjectMessageVisitor {

    private BooleanProperty authorized;
    private Label loginMessage;
    private ListView<String> list;
    private Label applicationMessage;

    public void setAuthorized(BooleanProperty authorized) {
        this.authorized = authorized;
    }

    public void setLoginMessage(Label loginMessage) {
        this.loginMessage = loginMessage;
    }

    public void setList(ListView<String> list) {
        this.list = list;
    }

    public void setApplicationMessage(Label applicationMessage) {
        this.applicationMessage = applicationMessage;
    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) {
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) {
        if (object instanceof ObjectMessage) {
            ObjectMessage objectMessage = (ObjectMessage) object;
            objectMessage.accept(channelHandlerContext, this);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
        channelHandlerContext.close();
    }

    @Override
    public void visit(ChannelHandlerContext channelHandlerContext, TextMessage textMessage) {
        System.out.println(textMessage.getMessage());
        Code code = textMessage.getCode();
        if (code == Code.AUTHENTICATED || code == Code.REGISTERED) {
            Platform.runLater(() -> authorized.set(true));
        } else if (code == Code.EXISTS || code == Code.INVALID || code == Code.UNAUTHENTICATED || code == Code.REMIND) {
            Platform.runLater(() -> loginMessage.setText(textMessage.getMessage()));
        }
    }

    @Override
    public void visit(ChannelHandlerContext channelHandlerContext, ServerResponse serverResponse) {
        ObservableList<String> responseList = FXCollections.observableArrayList();
        serverResponse.getFoundNodes().forEach(element -> {
            responseList.addAll(element);
            responseList.add("");
        });
        Platform.runLater(() -> {
            list.setItems(responseList);
            applicationMessage.setText(serverResponse.getFoundNodes().size() + " results found");
        });
    }
}
