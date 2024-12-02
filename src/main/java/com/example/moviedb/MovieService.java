package com.example.moviedb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.db.exception.transaction.TransactionException;

import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

public class MovieService {
    private static final String TMDB_API_KEY = "your_api_key";
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

    private ScalarDBOperations scalarDBOperations;

    public MovieService() {
        this.scalarDBOperations = new ScalarDBOperations();
        try {
            scalarDBOperations.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ScalarDB", e);
        }
    }

    public JsonNode fetchMovieDetails(int movieId) throws IOException {
        String url = TMDB_BASE_URL + "/movie/" + movieId + "?api_key=" + TMDB_API_KEY + "&append_to_response=credits";
        String response = Request.Get(url).execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode movieDetails = mapper.readTree(response);

        // Extract genre IDs
        if (movieDetails.has("genres") && movieDetails.get("genres").isArray()) {
            List<Integer> genreIds = new ArrayList<>();
            for (JsonNode genre : movieDetails.get("genres")) {
                genreIds.add(genre.get("id").asInt());
            }
            ((ObjectNode) movieDetails).put("genre_ids", mapper.writeValueAsString(genreIds));
        }

        return movieDetails;
    }


    public JsonNode searchTMDBMovies(String query) throws IOException {
        String url = TMDB_BASE_URL + "/search/movie?api_key=" + TMDB_API_KEY + "&query=" + query;
        String response = Request.Get(url).execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(response);
    }

    public JsonNode searchLocalMovies(String query) throws TransactionException {
        List<Map<String, String>> movies = scalarDBOperations.searchMovies(query);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(movies);
    }

    public void addMovie(String userId, int movieId, String listName) throws Exception {
        // Check if the movie is already in the movies table
        if (!scalarDBOperations.isMovieInMoviesTable(movieId)) {
            JsonNode movieDetails = fetchMovieDetails(movieId);

            ObjectMapper mapper = new ObjectMapper();
            List<Integer> genreIds = mapper.readValue(movieDetails.get("genre_ids").asText(), new TypeReference<List<Integer>>() {});

            Map<String, String> movieDetailsMap = new HashMap<>();
            movieDetailsMap.put("id", String.valueOf(movieDetails.get("id").asInt()));
            movieDetailsMap.put("title", movieDetails.get("title").asText());
            movieDetailsMap.put("release_date", movieDetails.get("release_date").asText());
            movieDetailsMap.put("poster_path", movieDetails.get("poster_path").asText());
            movieDetailsMap.put("genre_ids", String.join(",", genreIds.stream().map(String::valueOf).collect(Collectors.toList())));
            
            scalarDBOperations.addMovieToMoviesTable(movieDetailsMap);
        }

        // Check if the movie is already in the user's list
        if (!scalarDBOperations.isMovieInList(userId, movieId, listName)) {
            scalarDBOperations.addMovieToList(userId, movieId, listName);
        }
    }
        
    
    public void moveMovieFromWatchlistToWatched(String userId, int movieId) throws TransactionException {
        if (scalarDBOperations.isMovieInList(userId, movieId, "watched")) {
            scalarDBOperations.removeMovieFromList(userId, movieId, "watchlist");
        } else {
            scalarDBOperations.moveMovieFromWatchlistToWatched(userId, movieId);
        }
    }

    public void addMovieToList(String userId, int movieId, String listName) throws TransactionException {
        scalarDBOperations.addMovieToList(userId, movieId, listName);
    }

    public void removeMovieFromList(String userId, int movieId, String listName) throws TransactionException {
        scalarDBOperations.removeMovieFromList(userId, movieId, listName);
    }

    public JsonNode listMoviesInUserList(String userId, String listName) throws TransactionException {
        List<Map<String, String>> movies = scalarDBOperations.listMoviesInUserList(userId, listName);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(movies);
    }


}
