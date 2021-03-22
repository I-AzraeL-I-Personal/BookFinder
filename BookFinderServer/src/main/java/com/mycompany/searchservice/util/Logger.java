package com.mycompany.searchservice.util;

import com.mycompany.searchservice.dto.Code;
import com.mycompany.searchservice.dto.TextMessage;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Logger {

    public static void printLog(ChannelHandlerContext channelHandlerContext, Code code) {
        System.out.println("Client " + channelHandlerContext.channel() + ": " + prepareLog(code));
    }

    public static void sendAndPrint(ChannelHandlerContext channelHandlerContext, Code code) {
        String message = prepareLog(code);
        channelHandlerContext.channel().writeAndFlush(new TextMessage(code, message));
        System.out.println("Client " + channelHandlerContext.channel() + ": " + message);
    }

    private static String prepareLog(Code code) {
        return switch (code) {
            case NONE -> "no code";
            case AUTHENTICATED -> "Authenticated";
            case UNAUTHENTICATED -> "Not authenticated";
            case EXISTS -> "User already exists";
            case REGISTERED -> "Registered";
            case INVALID -> "Invalid data";
            case JOINED -> "Joined";
            case LEFT -> "Left";
            case EXIT -> "Exit";
            case INTERNAL_ERROR -> "Internal server error";
            case REMIND -> "Remind password";
        };
    }
}
