package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.BufferedHeadInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BufferedHeadInputStreamTest {

    @Test
    void testRead() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);

        int firstByte = rewindableStream.read();
        Assertions.assertEquals('T', firstByte);

        byte[] buffer = new byte[9];
        int bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(9, bytesRead);
        Assertions.assertArrayEquals("his is a ".getBytes(StandardCharsets.UTF_8), buffer);

        // Read more bytes
        bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(9, bytesRead);
        Assertions.assertArrayEquals("test inpu".getBytes(StandardCharsets.UTF_8), buffer);

        // Read remaining bytes
        bytesRead = rewindableStream.read(buffer, 0, 8);
        Assertions.assertEquals(8, bytesRead);
        Assertions.assertArrayEquals("t stream".getBytes(StandardCharsets.UTF_8),
                new byte[]{buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]});
    }

    @Test
    void testReadWithOffsetGreaterThanBufferLength() {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);
        byte[] buffer = new byte[10];
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rewindableStream.read(buffer, 11, 1));
    }

    @Test
    void testReadWithLengthGreaterThanBufferLength() {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);
        byte[] buffer = new byte[10];
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rewindableStream.read(buffer, 0, 11));
    }

    @Test
    void testReadWithOffsetPlusLengthGreaterThanBufferLength() {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);
        byte[] buffer = new byte[10];
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> rewindableStream.read(buffer, 6, 5));
    }

    @Test
    void testReadWhenEndOfStream() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);
        byte[] buffer = new byte[testData.length()];
        int bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(testData.length(), bytesRead);

        bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(-1, bytesRead);
    }

    @Test
    void testReadWithLargeBuffer() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 5);
        byte[] buffer = new byte[50];
        int bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(testData.length(), bytesRead);
        Assertions.assertArrayEquals(testData.getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(buffer, 0, bytesRead));
    }

    @Test
    void testReadWithSmallBuffer() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 20);
        byte[] buffer = new byte[4];
        int bytesRead1 = rewindableStream.read(buffer);
        Assertions.assertEquals(4, bytesRead1);
        Assertions.assertArrayEquals("This".getBytes(StandardCharsets.UTF_8), buffer);
        int bytesRead2 = rewindableStream.read(buffer);
        Assertions.assertEquals(4, bytesRead2);
        Assertions.assertArrayEquals(" is ".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void testReadWithExactCacheSize() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, testData.length());
        byte[] buffer = new byte[testData.length()];
        int bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(testData.length(), bytesRead);
        Assertions.assertArrayEquals(testData.getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void testReadWithCacheSmallerThanData() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 5);
        byte[] buffer = new byte[10];
        int bytesRead1 = rewindableStream.read(buffer);
        Assertions.assertEquals(10, bytesRead1);
        Assertions.assertArrayEquals("This is a ".getBytes(StandardCharsets.UTF_8), buffer);
        int bytesRead2 = rewindableStream.read(buffer);
        Assertions.assertEquals(10, bytesRead2);
        Assertions.assertArrayEquals("test input".getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void testReadWithCacheLargerThanData() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 50);
        byte[] buffer = new byte[testData.length()];
        int bytesRead = rewindableStream.read(buffer);
        Assertions.assertEquals(testData.length(), bytesRead);
        Assertions.assertArrayEquals(testData.getBytes(StandardCharsets.UTF_8), buffer);
    }

    @Test
    void testAvailable() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);

        int available = rewindableStream.available();
        Assertions.assertEquals(testData.length(), available);

        byte[] buffer = new byte[10];
        Assertions.assertEquals(10, rewindableStream.read(buffer));
        available = rewindableStream.available();
        Assertions.assertEquals(testData.length() - 10, available);
    }

    @Test
    void testSkip() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);

        long skipped = rewindableStream.skip(5);
        Assertions.assertEquals(5, skipped);

        int nextByte = rewindableStream.read();
        Assertions.assertEquals('i', nextByte);

        skipped = rewindableStream.skip(testData.length());
        Assertions.assertEquals(testData.length() - 6, skipped);

        nextByte = rewindableStream.read();
        Assertions.assertEquals(-1, nextByte);  // End of stream
    }

    @Test
    void testClose() throws IOException {
        String testData = "This is a test input stream";
        InputStream originalStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        BufferedHeadInputStream rewindableStream = new BufferedHeadInputStream(originalStream, 10);
        rewindableStream.close();
        Assertions.assertThrows(IOException.class, rewindableStream::read);
    }
}
