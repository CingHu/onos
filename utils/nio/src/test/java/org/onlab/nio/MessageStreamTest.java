package org.onlab.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests of the message message stream implementation.
 */
public class MessageStreamTest {

    private static final int SIZE = 16;
    private static final TestMessage MESSAGE = new TestMessage(SIZE);

    private static final int BIG_SIZE = 32 * 1024;
    private static final TestMessage BIG_MESSAGE = new TestMessage(BIG_SIZE);

    private TestIOLoop loop;
    private TestByteChannel channel;
    private TestMessageStream stream;
    private TestKey key;

    @Before
    public void setUp() throws IOException {
        loop = new TestIOLoop();
        channel = new TestByteChannel();
        key = new TestKey(channel);
        stream = loop.createStream(channel);
        stream.setKey(key);
    }

    @After
    public void tearDown() {
        loop.shutdown();
        stream.close();
    }

    // Validates the state of the message stream
    private void validate(boolean wp, boolean fr, int read, int written) {
        assertEquals(wp, stream.isWritePending());
        assertEquals(fr, stream.isFlushRequired());
        assertEquals(read, channel.readBytes);
        assertEquals(written, channel.writtenBytes);
    }

    @Test
    public void endOfStream() throws IOException {
        channel.close();
        List<TestMessage> messages = stream.read();
        assertNull(messages);
    }

    @Test
    public void bufferGrowth() throws IOException {
        // Create a stream for big messages and test the growth.
        stream = new TestMessageStream(BIG_SIZE, channel, loop);
        stream.write(BIG_MESSAGE);
        stream.write(BIG_MESSAGE);
        stream.write(BIG_MESSAGE);
        stream.write(BIG_MESSAGE);
        stream.write(BIG_MESSAGE);
    }

    @Test
    public void discardBeforeKey() {
        // Create a stream that does not yet have the key set and discard it.
        stream = loop.createStream(channel);
        assertNull(stream.key());
        stream.close();
        // There is not key, so nothing to check; we just expect no problem.
    }

    @Test
    public void bufferedRead() throws IOException {
        channel.bytesToRead = SIZE + 4;
        List<TestMessage> messages = stream.read();
        assertEquals(1, messages.size());
        validate(false, false, SIZE + 4, 0);

        channel.bytesToRead = SIZE - 4;
        messages = stream.read();
        assertEquals(1, messages.size());
        validate(false, false, SIZE * 2, 0);
    }

    @Test
    public void bufferedWrite() throws IOException {
        validate(false, false, 0, 0);

        // First write is immediate...
        stream.write(MESSAGE);
        validate(false, false, 0, SIZE);

        // Second and third get buffered...
        stream.write(MESSAGE);
        validate(false, true, 0, SIZE);
        stream.write(MESSAGE);
        validate(false, true, 0, SIZE);

        // Reset write, which will flush if needed; the next write is again buffered
        stream.flushIfWriteNotPending();
        validate(false, false, 0, SIZE * 3);
        stream.write(MESSAGE);
        validate(false, true, 0, SIZE * 3);

        // Select reset, which will flush if needed; the next write is again buffered
        stream.flushIfPossible();
        validate(false, false, 0, SIZE * 4);
        stream.write(MESSAGE);
        validate(false, true, 0, SIZE * 4);
        stream.flush();
        validate(false, true, 0, SIZE * 4);
    }

    @Test
    public void bufferedWriteList() throws IOException {
        validate(false, false, 0, 0);

        // First write is immediate...
        List<TestMessage> messages = new ArrayList<>();
        messages.add(MESSAGE);
        messages.add(MESSAGE);
        messages.add(MESSAGE);
        messages.add(MESSAGE);

        stream.write(messages);
        validate(false, false, 0, SIZE * 4);

        stream.write(messages);
        validate(false, true, 0, SIZE * 4);

        stream.flushIfPossible();
        validate(false, false, 0, SIZE * 8);
    }

    @Test
    public void bufferedPartialWrite() throws IOException {
        validate(false, false, 0, 0);

        // First write is immediate...
        stream.write(MESSAGE);
        validate(false, false, 0, SIZE);

        // Tell test channel to accept only half.
        channel.bytesToWrite = SIZE / 2;

        // Second and third get buffered...
        stream.write(MESSAGE);
        validate(false, true, 0, SIZE);
        stream.flushIfPossible();
        validate(true, true, 0, SIZE + SIZE / 2);
    }

    @Test
    public void bufferedPartialWrite2() throws IOException {
        validate(false, false, 0, 0);

        // First write is immediate...
        stream.write(MESSAGE);
        validate(false, false, 0, SIZE);

        // Tell test channel to accept only half.
        channel.bytesToWrite = SIZE / 2;

        // Second and third get buffered...
        stream.write(MESSAGE);
        validate(false, true, 0, SIZE);
        stream.flushIfWriteNotPending();
        validate(true, true, 0, SIZE + SIZE / 2);
    }

    @Test
    public void bufferedReadWrite() throws IOException {
        channel.bytesToRead = SIZE + 4;
        List<TestMessage> messages = stream.read();
        assertEquals(1, messages.size());
        validate(false, false, SIZE + 4, 0);

        stream.write(MESSAGE);
        validate(false, false, SIZE + 4, SIZE);

        channel.bytesToRead = SIZE - 4;
        messages = stream.read();
        assertEquals(1, messages.size());
        validate(false, false, SIZE * 2, SIZE);
    }

    // Fake IO driver loop
    private static class TestIOLoop extends IOLoop<TestMessage, TestMessageStream> {

        public TestIOLoop() throws IOException {
            super(500);
        }

        @Override
        protected TestMessageStream createStream(ByteChannel channel) {
            return new TestMessageStream(SIZE, channel, this);
        }

        @Override
        protected void processMessages(List<TestMessage> messages,
                                       MessageStream<TestMessage> stream) {
        }

    }

    // Byte channel test fixture
    private static class TestByteChannel extends SelectableChannel implements ByteChannel {

        private static final int BUFFER_LENGTH = 1024;
        byte[] bytes = new byte[BUFFER_LENGTH];
        int bytesToWrite = BUFFER_LENGTH;
        int bytesToRead = BUFFER_LENGTH;
        int writtenBytes = 0;
        int readBytes = 0;

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int l = Math.min(dst.remaining(), bytesToRead);
            if (bytesToRead > 0) {
                readBytes += l;
                dst.put(bytes, 0, l);
            }
            return l;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int l = Math.min(src.remaining(), bytesToWrite);
            writtenBytes += l;
            src.get(bytes, 0, l);
            return l;
        }

        @Override
        public Object blockingLock() {
            return null;
        }

        @Override
        public SelectableChannel configureBlocking(boolean arg0) throws IOException {
            return null;
        }

        @Override
        public boolean isBlocking() {
            return false;
        }

        @Override
        public boolean isRegistered() {
            return false;
        }

        @Override
        public SelectionKey keyFor(Selector arg0) {
            return null;
        }

        @Override
        public SelectorProvider provider() {
            return null;
        }

        @Override
        public SelectionKey register(Selector arg0, int arg1, Object arg2)
                throws ClosedChannelException {
            return null;
        }

        @Override
        public int validOps() {
            return 0;
        }

        @Override
        protected void implCloseChannel() throws IOException {
            bytesToRead = -1;
        }

    }

    // Selection key text fixture
    private static class TestKey extends SelectionKey {

        private SelectableChannel channel;

        public TestKey(TestByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public void cancel() {
        }

        @Override
        public SelectableChannel channel() {
            return channel;
        }

        @Override
        public int interestOps() {
            return 0;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            return null;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public int readyOps() {
            return 0;
        }

        @Override
        public Selector selector() {
            return null;
        }
    }

}