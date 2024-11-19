package com.example.joke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class JokeController {

    private static final Logger logger = LoggerFactory.getLogger(JokeController.class);

    @Value("${joke.api.url}")
    private String jokeApiUrl;

    @GetMapping("/languages")
    public List<String> getSupportedLanguages() {
        logger.info("Fetching supported languages from JokeAPI.");

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://v2.jokeapi.dev/languages";
        String response = restTemplate.getForObject(url, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(response);
            JsonNode languagesNode = rootNode.path("jokeLanguages");
            List<String> languages = objectMapper.convertValue(languagesNode, List.class);
            logger.debug("Joke languages available from JokeAPI: {}", languages);
            return languages;
        } catch (Exception e) {
            logger.error("Error while fetching supported languages.", e);
            return null;
        }
    }

    @GetMapping("/get-joke")
    public String getJoke(@RequestParam(name = "lang", defaultValue = "en") String lang) {
        logger.info("Received request to fetch a joke in language: {}", lang);

        // Fetch supported languages
        List<String> supportedLanguages = getSupportedLanguages();
        if (supportedLanguages == null || !supportedLanguages.contains(lang)) {
            logger.warn("Invalid language: {}. Falling back to English.", lang);
            lang = "en"; // Default to English if invalid
        }

        // Modify the jokeApiUrl to include the selected language
        String urlWithLang = jokeApiUrl.replace("lang=en", "lang=" + lang);

        RestTemplate restTemplate = new RestTemplate();
        String response;

        try {
            logger.debug("Sending GET request to JokeAPI: {}", urlWithLang);
            response = restTemplate.getForObject(urlWithLang, String.class);
            logger.debug("Received response from JokeAPI: {}", response);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);

            boolean isError = rootNode.path("error").asBoolean();
            if (isError) {
                logger.warn("JokeAPI returned an error response.");
                return "Failed to fetch a joke. Please try again later.";
            }

            String type = rootNode.path("type").asText();
            if ("twopart".equals(type)) {
                String setup = rootNode.path("setup").asText();
                String delivery = rootNode.path("delivery").asText();
                logger.info("Fetched a two-part joke: {} {}", setup, delivery);
                return setup + " " + delivery;
            } else if ("single".equals(type)) {
                String joke = rootNode.path("joke").asText();
                logger.info("Fetched a single-part joke: {}", joke);
                return joke;
            } else {
                logger.error("Unexpected joke format: {}", type);
                return "Unexpected joke format!";
            }
        } catch (Exception e) {
            logger.error("An exception occurred while processing the joke.", e);
            return "An error occurred while processing the joke.";
        }
    }
}