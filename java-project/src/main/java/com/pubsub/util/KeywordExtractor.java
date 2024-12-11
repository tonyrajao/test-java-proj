package com.pubsub.util;

import opennlp.tools.tokenize.SimpleTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for extracting keywords from text content.
 * Optimized for performance in a containerized environment.
 */
@Component
public class KeywordExtractor {
    private static final Logger logger = LoggerFactory.getLogger(KeywordExtractor.class);
    
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_WORD_LENGTH = 50;
    private static final int MAX_KEYWORDS = 5;
    private static final float TITLE_WEIGHT = 2.0f;
    
    private static final Pattern CLEANUP_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s-]");
    private static final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
    
    private static final Set<String> STOP_WORDS = ConcurrentHashMap.newKeySet();
    
    @PostConstruct
    public void init() {
        initializeStopWords();
        logger.info("KeywordExtractor initialized with {} stop words", STOP_WORDS.size());
    }
    
    private void initializeStopWords() {
        // English stop words
        STOP_WORDS.addAll(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "were", "will", "with", "about", "above", "after",
            "again", "all", "am", "any", "been", "before", "being", "below",
            "between", "both", "but", "can", "did", "do", "does", "doing",
            "down", "during", "each", "few", "further", "had", "have", "having",
            "her", "here", "hers", "herself", "him", "himself", "his", "how",
            "i", "if", "into", "me", "most", "my", "myself", "no", "nor", "neither",
            "not", "now", "or", "other", "our", "ours", "ourselves", "out", "own",
            "same", "she", "should", "so", "some", "such", "than", "their", "theirs",
            "them", "themselves", "then", "there", "these", "they", "this", "those",
            "through", "too", "under", "until", "up", "very", "we", "what", "when",
            "where", "which", "while", "who", "whom", "why", "would", "you", "your",
            "yours", "yourself", "yourselves", "new", "how", "more"
        ));

        // French stop words
        STOP_WORDS.addAll(Arrays.asList(
            "le", "la", "les", "un", "une", "des", "du", "de", "et", "est",
            "dans", "en", "sur", "pour", "par", "avec", "ce", "cette", "ces",
            "mais", "ou", "où", "donc", "or", "ni", "car", "au", "aux", "plus",
            "moins", "très", "autre", "autres", "bien", "même", "être", "avoir",
            "tous", "tout", "toute", "toutes", "quel", "quelle", "quels", "quelles",
            "qui", "que", "quoi", "dont", "où", "comment", "pourquoi", "quand"
        ));
    }

    /**
     * Extracts keywords from title and content with improved performance.
     * Title words are given higher weight in the frequency calculation.
     *
     * @param title the content title
     * @param content the main content
     * @return a set of relevant keywords
     */
    public Set<String> extractKeywords(String title, String content) {
        try {
            if (title == null || content == null) {
                logger.warn("Null input received: title={}, content={}", title != null, content != null);
                return Collections.emptySet();
            }

            // Clean and normalize the text
            String cleanTitle = cleanText(title);
            String cleanContent = cleanText(content);

            // Process title and content separately with different weights
            Map<String, Double> weightedFreq = new HashMap<>();
            processText(cleanTitle, weightedFreq, TITLE_WEIGHT);
            processText(cleanContent, weightedFreq, 1.0f);

            // Select top keywords
            return weightedFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_KEYWORDS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        } catch (Exception e) {
            logger.error("Error extracting keywords: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private String cleanText(String text) {
        return CLEANUP_PATTERN.matcher(text.toLowerCase().trim())
            .replaceAll(" ")
            .replaceAll("\\s+", " ");
    }

    private void processText(String text, Map<String, Double> weightedFreq, float weight) {
        String[] tokens = tokenizer.tokenize(text);
        
        for (String token : tokens) {
            if (isValidToken(token)) {
                weightedFreq.merge(token, (double) weight, Double::sum);
            }
        }
    }

    private boolean isValidToken(String token) {
        return token.length() >= MIN_WORD_LENGTH && 
               token.length() <= MAX_WORD_LENGTH && 
               !STOP_WORDS.contains(token);
    }
}