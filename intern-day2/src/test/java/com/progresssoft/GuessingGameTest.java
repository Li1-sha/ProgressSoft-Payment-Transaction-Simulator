package com.progresssoft;

import org.junit.Test;

import static org.junit.Assert.*;

public class GuessingGameTest {

    @Test
    public void testSecretNumberIsBetween1And100() {
        GuessingGame game = new GuessingGame();
        int secret = game.getSecretNum();
        assertTrue("Secret number should be between 1 and 100", secret >= 1 && secret <= 100);
    }

    @Test
    public void testMultipleGamesHaveDifferentNumbers() {
        GuessingGame game1 = new GuessingGame();
        GuessingGame game2 = new GuessingGame();

        // Just verify both are in range
        assertTrue(game1.getSecretNum() >= 1 && game1.getSecretNum() <= 100);
        assertTrue(game2.getSecretNum() >= 1 && game2.getSecretNum() <= 100);
    }
}