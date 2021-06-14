package com.redislabs.exam.hangman.model;

public class ServerResponse {

    private String token;
    private String hangman;
    private Boolean correct;
    private int failedAttempts;
    private boolean gameEnded;
    private boolean gameWon;
    private String message;

    public ServerResponse() {

    }

    public String getToken() {
        return token;
    }

    public String getHangman() {
        return hangman;
    }

    public Boolean isCorrect() {
        return correct;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public String getMessage() {
        return message;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    @Override
    public String toString() {
        return "ServerResponse{" +
                "token='" + token + '\'' +
                ", hangman='" + hangman + '\'' +
                ", correct=" + correct +
                ", failedAttempts=" + failedAttempts +
                ", gameEnded=" + gameEnded +
                ", gameWon=" + gameWon +
                ", message='" + message + '\'' +
                '}';
    }
}
