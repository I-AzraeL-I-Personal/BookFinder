package com.mycompany.searchservice.dto;

import io.netty.channel.ChannelHandlerContext;

public interface ObjectMessageVisitor {

    void visit(ChannelHandlerContext channelHandlerContext, TextMessage textMessage);

    void visit(ChannelHandlerContext channelHandlerContext, ServerResponse serverResponse);
}
