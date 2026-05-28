package cloud.sta.stashield.network;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginStartTest {
    @Test
    void parsesUsernameFromLoginStart() throws Exception {
        MinecraftFrame frame = MinecraftFrame.read(new ByteArrayInputStream(loginStart("STAPlayer")), 4096);

        LoginStart loginStart = LoginStart.parse(frame);

        assertEquals("STAPlayer", loginStart.username());
    }

    @Test
    void parsesUsernameWhenNewerProtocolAddsTrailingFields() throws Exception {
        MinecraftFrame frame = MinecraftFrame.read(new ByteArrayInputStream(loginStart("STAPlayer", true)), 4096);

        LoginStart loginStart = LoginStart.parse(frame);

        assertEquals("STAPlayer", loginStart.username());
    }

    @Test
    void rejectsUnexpectedPacketId() throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeVarInt(payload, 1);
        writeString(payload, "STAPlayer");

        ByteArrayOutputStream frameBytes = new ByteArrayOutputStream();
        byte[] payloadBytes = payload.toByteArray();
        writeVarInt(frameBytes, payloadBytes.length);
        frameBytes.write(payloadBytes);
        MinecraftFrame frame = MinecraftFrame.read(new ByteArrayInputStream(frameBytes.toByteArray()), 4096);

        assertThrows(ProtocolException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                LoginStart.parse(frame);
            }
        });
    }

    private static byte[] loginStart(String username) throws Exception {
        return loginStart(username, false);
    }

    private static byte[] loginStart(String username, boolean trailingUuid) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeVarInt(payload, 0);
        writeString(payload, username);
        if (trailingUuid) {
            payload.write(new byte[16]);
        }

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
