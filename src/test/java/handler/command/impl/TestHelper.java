package handler.command.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Test helper class that provides a mock-like Socket implementation for testing.
 * This is necessary because Mockito cannot mock java.net.Socket on Java 25+.
 */
public class TestHelper {

    /**
     * Creates a TestSocket that can be used in tests without Mockito
     */
    public static TestSocket createTestSocket() {
        return new TestSocket("127.0.0.1", 12345);
    }

    public static TestSocket createTestSocket(String host, int port) {
        return new TestSocket(host, port);
    }

    /**
     * A real Socket subclass for testing purposes
     */
    public static class TestSocket extends Socket {
        private final String host;
        private final int port;
        private final ByteArrayOutputStream outputStream;
        private final ByteArrayInputStream inputStream;
        private final InetAddress inetAddress;

        public TestSocket(String host, int port) {
            this.host = host;
            this.port = port;
            this.outputStream = new ByteArrayOutputStream();
            this.inputStream = new ByteArrayInputStream(new byte[0]);
            this.inetAddress = createInetAddress(host);
        }
        
        private static InetAddress createInetAddress(String host) {
            try {
                // Create a real InetAddress from the host string
                // This handles both IP addresses and hostnames
                byte[] addr;
                String[] parts = host.split("\\.");
                if (parts.length == 4) {
                    // It's an IP address
                    addr = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        addr[i] = (byte) Integer.parseInt(parts[i]);
                    }
                    return InetAddress.getByAddress(host, addr);
                } else {
                    // Use loopback for non-IP strings
                    return InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1});
                }
            } catch (UnknownHostException e) {
                // Fallback to loopback address
                try {
                    return InetAddress.getByAddress(host, new byte[]{127, 0, 0, 1});
                } catch (UnknownHostException ex) {
                    return InetAddress.getLoopbackAddress();
                }
            }
        }

        @Override
        public InetAddress getInetAddress() {
            return inetAddress;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public ByteArrayOutputStream getOutputStream() throws IOException {
            return outputStream;
        }

        @Override
        public ByteArrayInputStream getInputStream() throws IOException {
            return inputStream;
        }

        public String getOutputAsString() {
            return outputStream.toString();
        }

        public void resetOutput() {
            outputStream.reset();
        }
    }
}
