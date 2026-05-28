package cloud.sta.stashield.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class MinecraftProtocol {
    private MinecraftProtocol() {
    }

    public static VarIntResult readVarIntWithBytes(InputStream input) throws IOException {
        int value = 0;
        int position = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(5);

        while (position < 5) {
            int current = input.read();
            if (current < 0) {
                throw new ProtocolException("Unexpected end of stream while reading VarInt");
            }
            bytes.write(current);
            value |= (current & 0x7f) << (position * 7);

            if ((current & 0x80) == 0) {
                return new VarIntResult(value, bytes.toByteArray());
            }
            position++;
        }

        throw new ProtocolException("VarInt is too large");
    }

    static byte[] readFully(InputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(bytes, offset, length - offset);
            if (read < 0) {
                throw new ProtocolException("Unexpected end of stream while reading packet");
            }
            offset += read;
        }
        return bytes;
    }

    public static final class VarIntResult {
        private final int value;
        private final byte[] bytes;

        private VarIntResult(int value, byte[] bytes) {
            this.value = value;
            this.bytes = bytes;
        }

        public int value() {
            return value;
        }

        public byte[] bytes() {
            return bytes;
        }
    }
}
