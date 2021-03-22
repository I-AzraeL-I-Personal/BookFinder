package com.mycompany.searchservice;

import com.mycompany.searchservice.dto.Code;
import com.mycompany.searchservice.dto.TextMessage;
import com.mycompany.searchservice.dto.User;
import com.mycompany.searchservice.dto.Users;
import com.mycompany.searchservice.util.Logger;
import com.mycompany.searchservice.util.XPathSearcher;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.net.InetAddress;
import java.util.Optional;

@Sharable
public class SearchServerAuthHandler extends ChannelInboundHandlerAdapter {

    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;
    private final File databaseFile;
    private final XPathSearcher bookFinder;

    public SearchServerAuthHandler(Marshaller marshaller, Unmarshaller unmarshaller, File databaseFile, XPathSearcher bookFinder) {
        this.marshaller = marshaller;
        this.unmarshaller = unmarshaller;
        this.databaseFile = databaseFile;
        this.bookFinder = bookFinder;
    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) {
        channelHandlerContext.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                (GenericFutureListener<Future<Channel>>) future -> {
                    sendTextMessage(channelHandlerContext, "Welcome to " + InetAddress.getLocalHost().getHostName() + " search service!", Code.NONE);
                    sendTextMessage(channelHandlerContext, "Your session is protected by " +
                            channelHandlerContext.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +
                            " cipher suite.\n", Code.NONE);
                    Logger.printLog(channelHandlerContext, Code.JOINED);
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) throws JAXBException {
        if (object instanceof User) {
            User user = (User) object;

            Users users = readFromXml();
            Optional<User> userOptional = users.getUsers().stream()
                    .filter(u -> u.getUserName().equals(user.getUserName()))
                    .findFirst();

            if (user.getType() == User.Type.REMIND) {
                Logger.printLog(channelHandlerContext, Code.REMIND);
                userOptional.ifPresentOrElse(
                        value -> sendTextMessage(channelHandlerContext, "Your password is: " + value.getPassword(), Code.REMIND),
                        () -> Logger.sendAndPrint(channelHandlerContext, Code.INVALID));
            } else if (user.getType() == User.Type.LOGIN) {
                if (userOptional.isPresent() && userOptional.get().getPassword().equals(user.getPassword())) {
                    Logger.sendAndPrint(channelHandlerContext, Code.AUTHENTICATED);
                    channelHandlerContext.pipeline().addLast(new SearchServerHandler(bookFinder));
                    channelHandlerContext.pipeline().remove(this);
                } else {
                    Logger.sendAndPrint(channelHandlerContext, Code.UNAUTHENTICATED);
                }
            } else if (user.getType() == User.Type.REGISTER) {
                if (userOptional.isPresent()) {
                    Logger.sendAndPrint(channelHandlerContext, Code.EXISTS);
                } else if (isValid(user)) {
                    Logger.sendAndPrint(channelHandlerContext, Code.REGISTERED);
                    users.getUsers().add(user);
                    writeToXml(users);
                    channelHandlerContext.pipeline().addLast(new SearchServerHandler(bookFinder));
                    channelHandlerContext.pipeline().remove(this);
                } else {
                    Logger.sendAndPrint(channelHandlerContext, Code.INVALID);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
        Logger.printLog(channelHandlerContext, Code.LEFT);
        channelHandlerContext.close();
    }

    private boolean isValid(User user) {
        return !(user.getUserName().isEmpty() || user.getPassword().isEmpty());
    }

    private void sendTextMessage(ChannelHandlerContext channelHandlerContext, String message, Code code) {
        channelHandlerContext.channel().writeAndFlush(new TextMessage(code, message));
    }

    private Users readFromXml() throws JAXBException {
        return (Users) unmarshaller.unmarshal(databaseFile);
    }

    private void writeToXml(Users users) throws JAXBException {
        marshaller.marshal(users, databaseFile);
    }
}