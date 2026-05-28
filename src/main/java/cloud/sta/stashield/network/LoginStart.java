package cloud.sta.stashield.network;

public final class LoginStart {
    private static final int LOGIN_START_PACKET_ID = 0;
    private static final int MAX_USERNAME_BYTES = 64;

    private final String username;

    private LoginStart(String username) {
        this.username = username;
    }

    public static LoginStart parse(MinecraftFrame frame) throws ProtocolException {
        ByteArrayCursor cursor = new ByteArrayCursor(frame.payload());
        int packetId = cursor.readVarInt();
        if (packetId != LOGIN_START_PACKET_ID) {
            throw new ProtocolException("Expected login start packet id 0, got " + packetId);
        }

        String username = cursor.readString(MAX_USERNAME_BYTES);
        if (username.trim().isEmpty()) {
            throw new ProtocolException("Login username is empty");
        }
        return new LoginStart(username);
    }

    public String username() {
        return username;
    }
}
