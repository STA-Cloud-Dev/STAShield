package cloud.sta.stashield.network;

import java.io.IOException;
import java.io.InputStream;

public final class MinecraftFrame {
    private final byte[] payload;
    private final byte[] raw;

    private MinecraftFrame(byte[] payload, byte[] raw) {
        this.payload = payload;
        this.raw = raw;
    }

    public static MinecraftFrame read(InputStream input, int maxPayloadBytes) throws IOException {
        MinecraftProtocol.VarIntResult length = MinecraftProtocol.readVarIntWithBytes(input);
        if (length.value() < 1) {
            throw new ProtocolException("Packet length must be positive");
        }
        if (length.value() > maxPayloadBytes) {
            throw new ProtocolException("Packet length " + length.value() + " exceeds max " + maxPayloadBytes);
        }

        byte[] payload = MinecraftProtocol.readFully(input, length.value());
        byte[] raw = new byte[length.bytes().length + payload.length];
        System.arraycopy(length.bytes(), 0, raw, 0, length.bytes().length);
        System.arraycopy(payload, 0, raw, length.bytes().length, payload.length);
        return new MinecraftFrame(payload, raw);
    }

    public byte[] payload() {
        return payload;
    }

    public byte[] raw() {
        return raw;
    }
}
