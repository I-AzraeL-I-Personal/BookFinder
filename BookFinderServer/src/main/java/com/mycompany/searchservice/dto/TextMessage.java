package com.mycompany.searchservice.dto;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

public class TextMessage implements Serializable, ObjectMessage {

    private final String message;

    private final Code code;

    public TextMessage(Code code, String message) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public Code getCode() {
        return code;
    }

    @Override
    public void accept(ChannelHandlerContext channelHandlerContext, ObjectMessageVisitor objectMessageVisitor) {
        objectMessageVisitor.visit(channelHandlerContext, this);
    }
}
