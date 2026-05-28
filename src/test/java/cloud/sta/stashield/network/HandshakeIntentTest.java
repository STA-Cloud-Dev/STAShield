package cloud.sta.stashield.network;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HandshakeIntentTest {
    @Test
    void parsesStatusHandshake() throws Exception {
        MinecraftFrame frame = MinecraftFrame.read(new ByteArrayInputStream(handshake(763, "mc.sta.local", 25565, 1)), 4096);

        HandshakeIntent intent = HandshakeIntent.parse(frame);

        assertEquals(763, intent.protocolVersion());
        assertEquals("mc.sta.local", intent.serverAddress());
        assertEquals(25565, intent.serverPort());
        assertEquals(HandshakeIntent.NextState.STATUS, intent.nextState());
    }

    @Test
    void rejectsUnsupportedNextState() throws Exception {
        MinecraftFrame frame = MinecraftFrame.read(new ByteArrayInputStream(handshake(763, "mc.sta.local", 25565, 3)), 4096);

        assertThrows(ProtocolException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                HandshakeIntent.parse(frame);
            }
        });
    }

    private static byte[] handshake(int protocol, String host, int port, int nextState) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeVarInt(payload, 0);
        writeVarInt(payload, protocol);
        writeString(payload, host);
        payload.write((port >>> 8) & 0xff);
        payload.write(port & 0xff);
        writeVarInt(payload, nextState);

        byte[] payloadBytes = payload.toByteArray();
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        writeVarInt(frame, payloadBytes.length);
        frame.write(payloadBytes);
        return frame.toByteArray();
    }

    private static void writeString(ByteArrayOutputStream output, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        int remaining = value;
        do {
            int current = remaining & 0x7f;
            remaining >>>= 7;
            if (remaining != 0) {
                current |= 0x80;
            }
            output.write(current);
        } while (remaining != 0);
    }
}
