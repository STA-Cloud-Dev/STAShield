package cloud.sta.stashield.network;

public final class HandshakeIntent {
    private static final int HANDSHAKE_PACKET_ID = 0;
    private static final int MAX_SERVER_ADDRESS_BYTES = 1024;

    private final int protocolVersion;
    private final String serverAddress;
    private final int serverPort;
    private final NextState nextState;

    private HandshakeIntent(int protocolVersion, String serverAddress, int serverPort, NextState nextState) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nextState = nextState;
    }

    public static HandshakeIntent parse(MinecraftFrame frame) throws ProtocolException {
        ByteArrayCursor cursor = new ByteArrayCursor(frame.payload());
        int packetId = cursor.readVarInt();
        if (packetId != HANDSHAKE_PACKET_ID) {
            throw new ProtocolException("Expected handshake packet id 0, got " + packetId);
        }

        int protocolVersion = cursor.readVarInt();
        String serverAddress = cursor.readString(MAX_SERVER_ADDRESS_BYTES);
        int serverPort = cursor.readUnsignedShort();
        int nextStateValue = cursor.readVarInt();
        NextState nextState = NextState.fromId(nextStateValue);
        if (nextState == null) {
            throw new ProtocolException("Unsupported handshake next state: " + nextStateValue);
        }
        return new HandshakeIntent(protocolVersion, serverAddress, serverPort, nextState);
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public String serverAddress() {
        return serverAddress;
    }

    public int serverPort() {
        return serverPort;
    }

    public NextState nextState() {
        return nextState;
    }

    public enum NextState {
        STATUS(1),
        LOGIN(2);

        private final int id;

        NextState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static NextState fromId(int id) {
            for (NextState state : values()) {
                if (state.id == id) {
                    return state;
                }
            }
            return null;
        }
    }
}
