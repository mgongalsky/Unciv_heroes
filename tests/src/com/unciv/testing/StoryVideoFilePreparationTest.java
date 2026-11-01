package com.unciv.testing;

import com.badlogic.gdx.files.FileHandle;
import com.unciv.ui.story.StoryVideoFilePreparation;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import static org.junit.Assert.*;

/** Проверяет инфраструктурный адаптер кэша на потоках в памяти без GL и файловой системы. */
public class StoryVideoFilePreparationTest {
    private static class Source extends FileHandle {
        final byte[] bytes;
        final long reportedLength;
        int reads;
        int closes;
        Source(byte[] bytes, long reportedLength) {
            super("intro.webm");
            this.bytes = bytes;
            this.reportedLength = reportedLength;
        }
        @Override public long length() { return reportedLength; }
        @Override public InputStream read() {
            reads++;
            return new ByteArrayInputStream(bytes) {
                @Override public void close() { closes++; }
            };
        }
    }

    /** Файл на границе лимита читается один раз, а декодер получает независимые потоки из кэша. */
    @Test public void cachesAtLimitAndReopensWithoutSourceReads() {
        byte[] bytes = new byte[] {1, 2, 3, 4};
        Source source = new Source(bytes, bytes.length);
        FileHandle cached = StoryVideoFilePreparation.INSTANCE.prepare(source, 4);
        assertNotSame(source, cached);
        bytes[0] = 9;
        assertArrayEquals(new byte[] {1, 2, 3, 4}, cached.readBytes());
        assertArrayEquals(new byte[] {1, 2, 3, 4}, cached.readBytes());
        assertTrue(cached.exists());
        assertEquals(4L, cached.length());
        assertEquals(source.path(), cached.path());
        assertEquals(1, source.reads);
        assertEquals(1, source.closes);
    }

    /** Известный большой файл остаётся потоковым без предварительного чтения. */
    @Test public void largeKnownFileStaysStreaming() {
        Source source = new Source(new byte[] {1}, 5);
        assertSame(source, StoryVideoFilePreparation.INSTANCE.prepare(source, 4));
        assertEquals(0, source.reads);
    }

    /** Неизвестная длина не обходит лимит; частично прочитанный поток закрывается. */
    @Test public void unknownLengthCannotExceedCacheLimit() {
        Source source = new Source(new byte[] {1, 2, 3, 4, 5}, 0);
        assertSame(source, StoryVideoFilePreparation.INSTANCE.prepare(source, 4));
        assertEquals(1, source.reads);
        assertEquals(1, source.closes);
    }

    /** Ошибка чтения передаётся вызывающему коду, но поток всё равно закрывается. */
    @Test public void readFailureClosesInput() {
        final int[] closes = {0};
        FileHandle source = new FileHandle("broken.webm") {
            @Override public long length() { return 0; }
            @Override public InputStream read() {
                return new InputStream() {
                    @Override public int read() { throw new IllegalStateException("read failed"); }
                    @Override public void close() { closes[0]++; }
                };
            }
        };
        try {
            StoryVideoFilePreparation.INSTANCE.prepare(source, 4);
            fail("Read failure must propagate");
        } catch (IllegalStateException expected) {
            assertEquals("read failed", expected.getMessage());
        }
        assertEquals(1, closes[0]);
    }
}
