package com.unciv.testing;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.unciv.ui.story.StoryVideoPlayer;
import com.unciv.ui.story.StoryVideoSession;
import com.unciv.ui.story.StoryVideoStatus;
import org.junit.Test;

import static org.junit.Assert.*;

/** Проверяет освобождение ресурса и безопасные переходы без GL и настоящего декодера. */
public class StoryVideoSessionTest {
    private static class Player implements StoryVideoPlayer {
        int updates, disposals, pauses, resumes;
        boolean decodeFails, disposeFails, pauseFails;
        StoryVideoStatus status = StoryVideoStatus.Playing;
        @Override public Actor getActor() { throw new AssertionError("Session must not access UI"); }
        @Override public StoryVideoStatus update(float delta) {
            updates++;
            if (decodeFails) throw new IllegalStateException("decode failure");
            return status;
        }
        @Override public void pause() {
            pauses++;
            if (pauseFails) throw new IllegalStateException("pause failure");
        }
        @Override public void resume() { resumes++; }
        @Override public void dispose() {
            disposals++;
            if (disposeFails) throw new IllegalStateException("dispose failure");
        }
    }

    @Test public void unsupportedPlatformUsesFallbackWithoutRetry() {
        int[] attempts = {0};
        StoryVideoSession session = new StoryVideoSession(() -> { attempts[0]++; return null; });
        session.start();
        session.start();
        session.update(1f);
        assertEquals(StoryVideoSession.State.Fallback, session.getState());
        assertEquals(1, attempts[0]);
    }

    @Test public void factoryFailureUsesFallback() {
        StoryVideoSession session = new StoryVideoSession(() -> { throw new IllegalStateException("missing asset"); });
        session.start();
        assertEquals(StoryVideoSession.State.Fallback, session.getState());
        assertNull(session.getPlayer());
    }

    @Test public void completionReleasesOnceAndStopsPolling() {
        Player player = new Player();
        StoryVideoSession session = new StoryVideoSession(() -> player);
        session.start();
        session.update(0.1f);
        assertEquals(StoryVideoSession.State.Playing, session.getState());
        player.status = StoryVideoStatus.Completed;
        session.update(0.1f);
        assertEquals(StoryVideoSession.State.Completed, session.getState());
        assertNull(session.getPlayer());
        session.update(1f);
        session.skip();
        session.dispose();
        session.dispose();
        assertEquals(2, player.updates);
        assertEquals(1, player.disposals);
    }

    @Test public void decodingAndCleanupFailuresStillAllowFallback() {
        Player player = new Player();
        player.decodeFails = true;
        player.disposeFails = true;
        StoryVideoSession session = new StoryVideoSession(() -> player);
        session.start();
        session.update(0.1f);
        assertEquals(StoryVideoSession.State.Fallback, session.getState());
        session.dispose();
        assertEquals(1, player.disposals);
    }

    @Test public void reportedFailureAndPauseFailureReleasePlayer() {
        for (boolean pauseFailure : new boolean[] {false, true}) {
            Player player = new Player();
            player.pauseFails = pauseFailure;
            player.status = StoryVideoStatus.Failed;
            StoryVideoSession session = new StoryVideoSession(() -> player);
            session.start();
            if (pauseFailure) session.pause(); else session.update(0.1f);
            assertEquals(StoryVideoSession.State.Fallback, session.getState());
            session.resume();
            session.dispose();
            assertEquals(0, player.resumes);
            assertEquals(1, player.disposals);
        }
    }

    @Test public void skipCompletesButDisposalOnlyCloses() {
        Player skippedPlayer = new Player();
        StoryVideoSession skipped = new StoryVideoSession(() -> skippedPlayer);
        skipped.start();
        skipped.pause();
        skipped.resume();
        skipped.skip();
        assertEquals(StoryVideoSession.State.Completed, skipped.getState());
        skipped.dispose();
        assertEquals(1, skippedPlayer.pauses);
        assertEquals(1, skippedPlayer.resumes);
        assertEquals(1, skippedPlayer.disposals);

        Player closedPlayer = new Player();
        StoryVideoSession closed = new StoryVideoSession(() -> closedPlayer);
        closed.start();
        closed.dispose();
        closed.start();
        closed.skip();
        assertEquals(StoryVideoSession.State.Closed, closed.getState());
        assertEquals(1, closedPlayer.disposals);
    }
}
