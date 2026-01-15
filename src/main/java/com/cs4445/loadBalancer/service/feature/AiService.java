package com.cs4445.loadBalancer.service.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Service
public class AiService {

    //==========================================Constant==========================================
    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final int BUFFER_SIZE = 4096;

    //===========================================Method===========================================

    /**
     * Gửi request đến AI server qua TCP connection
     * @param host địa chỉ host của AI server
     * @param port port của AI server
     * @param request nội dung request cần gửi
     * @return response từ AI server
     */
    public String sendTcpRequest(String host, int port, String request) {
        return sendTcpRequest(host, port, request, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Gửi request đến AI server qua TCP connection với timeout tùy chỉnh
     * @param host địa chỉ host của AI server
     * @param port port của AI server
     * @param request nội dung request cần gửi
     * @param timeoutMs thời gian timeout (milliseconds)
     * @return response từ AI server
     */
    public String sendTcpRequest(String host, int port, String request, int timeoutMs) {
        log.debug("Sending TCP request to {}:{}", host, port);
        long startTime = System.currentTimeMillis();

        try (Socket socket = createSocket(host, port, timeoutMs);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            // Gửi request
            sendData(out, request);
            log.debug("Request sent successfully");

            // Nhận response
            String response = receiveData(in);
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Response received in {} ms", processingTime);

            return response;

        } catch (SocketTimeoutException e) {
            log.error("TCP connection timeout to {}:{} - {}", host, port, e.getMessage());
            throw new RuntimeException("Connection timeout to AI server: " + host + ":" + port, e);

        } catch (IOException e) {
            log.error("TCP connection error to {}:{} - {}", host, port, e.getMessage());
            throw new RuntimeException("Failed to connect to AI server: " + host + ":" + port, e);
        }
    }

    /**
     * Gửi request và nhận response với protocol đơn giản (length-prefixed)
     * Format: [4 bytes length][payload]
     */
    public String sendTcpRequestWithLengthPrefix(String host, int port, String request, int timeoutMs) {
        log.debug("Sending length-prefixed TCP request to {}:{}", host, port);

        try (Socket socket = createSocket(host, port, timeoutMs);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Gửi request với length prefix
            byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
            out.writeInt(requestBytes.length);
            out.write(requestBytes);
            out.flush();
            log.debug("Request sent: {} bytes", requestBytes.length);

            // Nhận response với length prefix
            int responseLength = in.readInt();
            byte[] responseBytes = new byte[responseLength];
            in.readFully(responseBytes);
            String response = new String(responseBytes, StandardCharsets.UTF_8);
            log.debug("Response received: {} bytes", responseLength);

            return response;

        } catch (SocketTimeoutException e) {
            log.error("TCP timeout to {}:{}", host, port);
            throw new RuntimeException("Connection timeout to AI server", e);

        } catch (IOException e) {
            log.error("TCP error to {}:{} - {}", host, port, e.getMessage());
            throw new RuntimeException("Failed to connect to AI server", e);
        }
    }

    /**
     * Kiểm tra kết nối TCP đến AI server
     * @return true nếu kết nối thành công
     */
    public boolean checkTcpConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 5000);
            log.debug("TCP connection check successful: {}:{}", host, port);
            return true;
        } catch (IOException e) {
            log.warn("TCP connection check failed: {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }

    //==========================================Private===========================================

    private Socket createSocket(String host, int port, int timeoutMs) throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        return socket;
    }

    private void sendData(OutputStream out, String data) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(data);
        writer.newLine();
        writer.flush();
    }

    private String receiveData(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        char[] buffer = new char[BUFFER_SIZE];
        int charsRead;

        // Đọc cho đến khi hết dữ liệu hoặc gặp newline
        String line = reader.readLine();
        if (line != null) {
            response.append(line);
        }

        return response.toString();
    }
}
