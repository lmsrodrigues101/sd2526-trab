package sd2526.trab.api.grpc;

import sd2526.trab.api.grpc.Messages.GrpcMessage; // A classe gerada está aqui dentro!

import java.util.HashSet;

public class DataModelAdaptorMessage {

    public static sd2526.trab.api.Message GrpcMessage_to_Message(GrpcMessage grpcMsg) {
        sd2526.trab.api.Message msg = new sd2526.trab.api.Message();

        msg.setId(grpcMsg.getId());
        msg.setSender(grpcMsg.getSender());
        msg.setSubject(grpcMsg.getSubject());
        msg.setContents(grpcMsg.getContents());
        msg.setCreationTime(grpcMsg.getCreationTime());

        msg.setDestination(new HashSet<>(grpcMsg.getDestinationList()));

        return msg;
    }

    public static GrpcMessage Message_to_GrpcMessage(sd2526.trab.api.Message msg) {
        GrpcMessage.Builder builder = GrpcMessage.newBuilder();

        if (msg.getId() != null) builder.setId(msg.getId());
        if (msg.getSender() != null) builder.setSender(msg.getSender());
        if (msg.getSubject() != null) builder.setSubject(msg.getSubject());
        if (msg.getContents() != null) builder.setContents(msg.getContents());

        builder.setCreationTime(msg.getCreationTime());

        if (msg.getDestination() != null) {
            builder.addAllDestination(msg.getDestination());
        }

        return builder.build();
    }
}