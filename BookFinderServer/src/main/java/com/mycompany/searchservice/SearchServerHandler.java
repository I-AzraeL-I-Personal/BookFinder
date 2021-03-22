package com.mycompany.searchservice;

import com.mycompany.searchservice.dto.*;
import com.mycompany.searchservice.util.Logger;
import com.mycompany.searchservice.util.XPathSearcher;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;
import java.util.stream.Collectors;

@Sharable
public class SearchServerHandler extends ChannelInboundHandlerAdapter implements ObjectMessageVisitor {

    private final XPathSearcher bookFinder;

    public SearchServerHandler(XPathSearcher bookFinder) {
        this.bookFinder = bookFinder;
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
        Logger.printLog(channelHandlerContext, Code.LEFT);
        channelHandlerContext.close();
    }

    private Set<List<String>> parseBookNodesToList(Set<String> nodes) {
        final int nodesToReturn = 10;
        return nodes.stream()
                .limit(nodesToReturn)
                .map(element -> {
                    List<String> temp = Arrays.asList(element.split("\n"));
                    List<String> result = new ArrayList<>(temp.subList(0, 4));
                    result.add(String.join(" ", temp.subList(4, temp.size())));
                    return result;
                })
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void visit(ChannelHandlerContext channelHandlerContext, TextMessage textMessage) {
        Logger.printLog(channelHandlerContext, textMessage.getCode());
        if (textMessage.getCode() == Code.EXIT) {
            channelHandlerContext.close();
        }
    }

    @Override
    public void visit(ChannelHandlerContext channelHandlerContext, ClientRequest clientRequest) {
        try {
            Set<List<String>> result = parseBookNodesToList(bookFinder.find(clientRequest.getPhrases()));
            channelHandlerContext.writeAndFlush(new ServerResponse(result));
        } catch (XPathExpressionException e) {
            Logger.sendAndPrint(channelHandlerContext, Code.INTERNAL_ERROR);
        }
    }
}
