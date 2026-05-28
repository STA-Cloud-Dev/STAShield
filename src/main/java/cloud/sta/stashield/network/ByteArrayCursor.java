package cloud.sta.stashield.network;

import java.nio.charset.StandardCharsets;

final class ByteArrayCursor {
    private final byte[] data;
    private int position;

    ByteArrayCursor(byte[] data) {
        this.data = data;
    }

    int readVarInt() throws ProtocolException {
        int value = 0;
        int varIntPosition = 0;

        while (varIntPosition < 5) {
            int current = readUnsignedByte();
            value |= (current & 0x7f) << (varIntPosition * 7);
            if ((current & 0x80) == 0) {
                return value;
            }
            varIntPosition++;
        }

        throw new ProtocolException("VarInt is too large");
    }

    String readString(int maxBytes) throws ProtocolException {
        int length = readVarInt();
        if (length < 0 || length > maxBytes) {
            throw new ProtocolException("String length out of bounds: " + length);
        }
        require(length);
        String value = new String(data, position, length, StandardCharsets.UTF_8);
        position += length;
        return value;
    }

    int readUnsignedShort() throws ProtocolException {
        require(2);
        int high = data[position++] & 0xff;
        int low = data[position++] & 0xff;
        return (high << 8) | low;
    }

    boolean hasRemaining() {
        return position < data.length;
    }

    private int readUnsignedByte() throws ProtocolException {
        require(1);
        return data[position++] & 0xff;
    }

    private void require(int length) throws ProtocolException {
        if (position + length > data.length) {
            throw new ProtocolException("Packet ended before expected field");
        }
    }
}
