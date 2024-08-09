package com.github.owlcs.ontapi;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * An InputStream with cached head.
 */
public class BufferedHeadInputStream extends InputStream {
    private final InputStream source;
    private final byte[] head;
    private final int headEnd;
    private int headPos = 0;
    private boolean closed;

    public BufferedHeadInputStream(InputStream source, int cacheSize) {
        this.source = Objects.requireNonNull(source);
        this.head = new byte[cacheSize];
        try {
            headEnd = source.read(head, 0, head.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to fill cache", e);
        }
    }

    public byte[] head() {
        return head;
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream is already closed");
        }
    }

    @Override
    public void close() throws IOException {
        source.close();
        closed = true;
    }

    @Override
    public int read() throws IOException {
        checkOpen();
        if (headPos < headEnd) {
            return head[headPos++] & 0xFF;
        } else {
            return source.read();
        }
    }

    @Override
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
        checkOpen();
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }

        int bytesRead = 0;
        if (headPos < headEnd) {
            int bytesToReadFromCache = Math.min(len, headEnd - headPos);
            System.arraycopy(head, headPos, b, off, bytesToReadFromCache);
            headPos += bytesToReadFromCache;
            bytesRead += bytesToReadFromCache;
            off += bytesToReadFromCache;
            len -= bytesToReadFromCache;
        }

        if (len > 0) {
            int bytesToReadFromSource = source.read(b, off, len);
            if (bytesToReadFromSource > 0) {
                bytesRead += bytesToReadFromSource;
            }
        }

        return bytesRead == 0 ? -1 : bytesRead;
    }

    @Override
    public int available() throws IOException {
        checkOpen();
        return (headEnd - headPos) + source.available();
    }

    @Override
    public long skip(long n) throws IOException {
        checkOpen();
        long skipped = 0;
        if (headPos < headEnd) {
            int bytesToSkipInCache = (int) Math.min(n, headEnd - headPos);
            headPos += bytesToSkipInCache;
            skipped += bytesToSkipInCache;
            n -= bytesToSkipInCache;
        }
        if (n > 0) {
            skipped += source.skip(n);
        }
        return skipped;
    }
}
