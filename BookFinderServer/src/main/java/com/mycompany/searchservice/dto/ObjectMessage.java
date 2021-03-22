package com.mycompany.searchservice.dto;

import io.netty.channel.ChannelHandlerContext;

public interface ObjectMessage {

    void accept(ChannelHandlerContext channelHandlerContext, ObjectMessageVisitor objectMessageVisitor);
}
