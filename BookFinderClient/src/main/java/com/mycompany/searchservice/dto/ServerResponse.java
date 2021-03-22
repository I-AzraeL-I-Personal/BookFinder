package com.mycompany.searchservice.dto;

import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class ServerResponse implements Serializable, ObjectMessage {

    private final static long serialVersionUID = 1L;
    private Set<List<String>> foundNodes;

    public ServerResponse(Set<List<String>> foundNodes) {
        this.foundNodes = foundNodes;
    }

    public Set<List<String>> getFoundNodes() {
        return foundNodes;
    }

    public void setFoundNodes(Set<List<String>> foundNodes) {
        this.foundNodes = foundNodes;
    }

    @Override
    public void accept(ChannelHandlerContext channelHandlerContext, ObjectMessageVisitor objectMessageVisitor) {
        objectMessageVisitor.visit(channelHandlerContext, this);
    }
}
