package com.redislabs.exam.hangman.player;

import com.redislabs.exam.hangman.model.ServerResponse;
import com.redislabs.exam.hangman.server.HangmanServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class HangmanPlayer {

    private static Set<String> dictionary;
    private static Character mostCommonCharInDictionary;
    private static Set<Character> alreadyGuessedChars;
    private static final int allowedNumberOfAttempts = 7;

    private static HangmanServer server = new HangmanServer();

    /**
     * This is the entry point of your Hangman Player
     * To start a new game call server.startNewGame()
     */
    public static void main(String[] args) throws Exception {

        // Start new game
        ServerResponse serverResponse = server.startNewGame();

        // hangman-word length
        Integer wordLength = serverResponse.getHangman().length();

        // hangman-game token
        String token = serverResponse.getToken();

        // set stats
        InitStats("dictionary.txt", wordLength);

        // start guessing
        StartGuessing(token);
    }

    // Player start guessing characters
    private static void StartGuessing(String token) throws Exception {

        int numOfGuesses = 0;
        ServerResponse serverResponse = null;

        while (serverResponse == null || serverResponse.getFailedAttempts() < allowedNumberOfAttempts) {

            // Player guess
            serverResponse = server.guess(token, mostCommonCharInDictionary);

            alreadyGuessedChars.add(mostCommonCharInDictionary);

            numOfGuesses++;

            // When game ends
            if (serverResponse.isGameEnded() || serverResponse.getFailedAttempts() >= allowedNumberOfAttempts) {

                if (serverResponse.isGameWon()) {
                    System.out.println("We have a winner!");
                }
                else {
                    System.out.println("You Lost");
                }
                System.out.println("Hangman: " + serverResponse.getHangman());
                System.out.println("Number of attempts:" + numOfGuesses);
                break;
            }

            // update stats after guess
            UpdateStatsAfterGuess(serverResponse.getCorrect(), serverResponse.getHangman());
        }
    }

    // update stats
    private static void UpdateStatsAfterGuess(Boolean isCorrectGuess, String hangman) {

        Boolean isPossibleWord = true;
        Set<String> removedWords = ConcurrentHashMap.newKeySet();
        ConcurrentHashMap<Character, Integer> charStatistics = new ConcurrentHashMap<>();

        // find non-possible words to remove from dictionary
        dictionary.stream().parallel().forEach(word -> FindWordsToRemoveFromDictionary(word, isCorrectGuess, hangman, removedWords));

        // remove all non possible from dictionary
        removedWords.stream().parallel().forEach(word -> dictionary.remove(word));

        // Set char stats for every possible word in dictionary
        dictionary.stream().parallel().forEach(word -> SetCharacterStatsForPossibleWord(word, charStatistics));

        // set most common char
        mostCommonCharInDictionary = GetMostCommonCharInDictionary(charStatistics);
    }

    // check if given word is non-possible and we need to remove it from dictionary
    private static void FindWordsToRemoveFromDictionary(String word, Boolean isCorrectGuess,
                                                        String hangman, Set<String> removedWords) {

        boolean isPossibleWord = true;

        boolean isWordContainsChar = word.chars().anyMatch(x -> x == (int) mostCommonCharInDictionary);

        // remove words from dictionary that
        // 1. doesn't contain the char that was guesses correctly
        // 2. contain the char that was guesses correctly
        if (isCorrectGuess && !isWordContainsChar || !isCorrectGuess && isWordContainsChar) {
            isPossibleWord = false;
        }
        else {

            // get list of indexes of the char that was guesses correctly in hangman-word
            List<Integer> charIndexesInHangman = getCharIndexesInHangman(hangman);

            // validate dictionary word contain char in the same indexes as hangman-word does
            isPossibleWord = IsPossibleWord(word, charIndexesInHangman);
        }

        // add non-possible word to words needed to be removed from dictionary
        if (!isPossibleWord) {
            removedWords.add(word);
        }
    }

    // validate possible word if contain char in all indexes the as hangman-word does
    private static Boolean IsPossibleWord(String word, List<Integer> charIndexesInHangman) {

        char[] wordAsCharArr = word.toCharArray();

        return charIndexesInHangman
                .stream()
                .parallel()
                .noneMatch(index -> word.length() < index + 1 || wordAsCharArr[index] != mostCommonCharInDictionary);
    }

    // get specific char's list of indexes in hangman-word
    private static List<Integer> getCharIndexesInHangman(String hangman) {

        List<Integer> charIndexesInHangman;

        char[] hangmanAsCharArr = hangman.toCharArray();

        charIndexesInHangman = IntStream
                .range(0, hangmanAsCharArr.length)
                .filter(i -> hangmanAsCharArr[i] == mostCommonCharInDictionary)
                .boxed()
                .collect(Collectors.toList());

        return charIndexesInHangman;
    }

    // initialize stats
    private static void InitStats(String fileName, Integer wordLength)
            throws URISyntaxException, FileNotFoundException {

        // Get dictionary file from resources
        URL file = getDictionaryFromResources(fileName);
        Scanner scanner = new Scanner(new File(file.toURI()));

        dictionary = ConcurrentHashMap.newKeySet();
        alreadyGuessedChars = new TreeSet<>();
        ConcurrentHashMap<Character, Integer> charStatistics = new ConcurrentHashMap<>();

        while (scanner.hasNext()) {

            String word = scanner.next();

            if (word.length() == wordLength){

                // Add possible word to dictionary
                dictionary.add(word.toLowerCase());

                // Set char stats for possible word
                SetCharacterStatsForPossibleWord(word, charStatistics);
            }
        }

        // set most common char
        mostCommonCharInDictionary = GetMostCommonCharInDictionary(charStatistics);
    }

    // Get dictionary file from resources
    private static URL getDictionaryFromResources(String fileName)
            throws IllegalArgumentException {
        ClassLoader classLoader = HangmanPlayer.class.getClassLoader();
        URL file = classLoader.getResource(fileName);
        if (file == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        }
        return file;
    }

    // set char stats map for possible word
    private static void SetCharacterStatsForPossibleWord(String word, ConcurrentHashMap<Character,
            Integer> charStatistics) {
        word.chars().parallel().forEach(c -> SetCharStats((char) c, charStatistics));
    }

    // Set possible word's char stats
    private static void SetCharStats(Character c, ConcurrentHashMap<Character, Integer> charStatistics) {

        if (!alreadyGuessedChars.contains(c)) {

            if (charStatistics.containsKey(c)) {
                charStatistics.put(c, charStatistics.get(c) + 1);
            } else {
                charStatistics.put(c, 1);
            }
        }
    }

    // get most common char in dictionary
    private static Character GetMostCommonCharInDictionary(ConcurrentHashMap<Character, Integer> charStatistics) {
        return Collections.max(charStatistics.entrySet(), Map.Entry.comparingByValue()).getKey();
    }
}
