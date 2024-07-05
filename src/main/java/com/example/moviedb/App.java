package com.example.moviedb;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

public class App {
    private static MovieService movieService = new MovieService();
    private static UserService userService = new UserService();

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/public", Location.CLASSPATH);
        }).start(7000);

        // Define routes
        app.get("/", ctx -> ctx.render("views/login.html"));
        app.get("/register", ctx -> ctx.render("views/register.html"));
        app.get("/profile", ctx -> ctx.render("views/profile.html"));
        app.get("/watchlist", ctx -> ctx.render("views/watchlist.html"));
        app.get("/watched", ctx -> ctx.render("views/watched.html"));
        app.get("/search", ctx -> ctx.render("views/search.html"));
        app.get("/friends", ctx -> ctx.render("views/friends.html"));

        // API routes
        app.post("/api/login", App::handleLogin);
        app.post("/api/register", App::handleRegister);
        app.post("/api/addMovie", App::handleAddMovie);
        app.delete("/api/deleteMovie", App::handleDeleteMovie);
        app.get("/api/searchMovies", App::handleSearchLocalMovies); 
        app.get("/api/searchTMDBMovies", App::handleSearchTMDBMovies);
        app.get("/api/listwatchlist", App::handleListWatchlist);
        app.get("/api/listwatched", App::handleListWatched);
        app.post("/api/moveToWatched", App::handleMoveToWatched);
        app.get("/api/userProfile", App::handleUserProfile);

        app.get("/api/searchUsers", App::handleSearchUsers);
        app.post("/api/sendFriendRequest", App::handleSendFriendRequest);
        app.post("/api/acceptFriendRequest", App::handleAcceptFriendRequest);
        app.post("/api/declineFriendRequest", App::handleDeclineFriendRequest);
        app.post("/api/removeFriendRequest", App::handleRemoveFriendRequest);
        app.post("/api/removeFriend", App::handleRemoveFriend);
        app.get("/api/listFriends", App::handleListFriends);
        app.get("/api/listFriendRequests", App::handleListFriendRequests);
    }

    private static class RegisterRequest {
        public String userId;
        public String password;
        public String email;
    }

    private static class LoginRequest {
        public String userId;
        public String password;
    }

    private static class addMovieRequest {
        public String userId;
        public String movieId;
        public String listName;
    }
    
    private static class FriendRequest {
        public String requesterId;
        public String requesteeId;
    }
    private static class Friendship {
        public String userId;
        public String friendId;
    }

    //region user login
    private static void handleLogin(Context ctx) {
        LoginRequest loginRequest = ctx.bodyAsClass(LoginRequest.class);

        String userId = loginRequest.userId;
        String password = loginRequest.password;

        System.out.println("Logging in with userId: " + userId);

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            ctx.status(400).result("Password is required");
            return;
        }

        boolean success = userService.loginUser(userId, password);
        if (success) {
            ctx.status(200).result("Login successful");
        } else {
            ctx.status(401).result("Invalid user ID or password");
        }
    }

    private static void handleRegister(Context ctx) {
        RegisterRequest registerRequest = ctx.bodyAsClass(RegisterRequest.class);

        String userId = registerRequest.userId;
        String password = registerRequest.password;
        String email = registerRequest.email;

        System.out.println("Registering with userId: " + userId + ", email: " + email);

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }
        if (password == null || password.length() < 8) {
            ctx.status(400).result("Password must be at least 8 characters long");
            return;
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            ctx.status(400).result("Invalid email format");
            return;
        }

        boolean success = userService.registerUser(userId, password, email);
        if (success) {
            ctx.status(201).result("Registration successful");
        } else {
            ctx.status(400).result("User already exists or invalid input");
        }
    }

    private static void handleUserProfile(Context ctx) {
        String userId = ctx.queryParam("userId");

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }

        try {
            var userProfile = userService.getUserProfile(userId);
            ctx.json(userProfile);
        } catch (Exception e) {
            ctx.status(500).result("Failed to get user profile");
        }
    }

    //endregion

    //region add/remove movies

    private static void handleAddMovie(Context ctx) {
        addMovieRequest addMovieRequest = ctx.bodyAsClass(addMovieRequest.class);

        String userId = addMovieRequest.userId;
        String movieIdStr = addMovieRequest.movieId;
        String listName = addMovieRequest.listName;


        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }
        if (movieIdStr == null || movieIdStr.trim().isEmpty()) {
            ctx.status(400).result("Movie ID is required");
            return;
        }
        if (listName == null || listName.trim().isEmpty()) {
            ctx.status(400).result("List name is required");
            return;
        }

        try {
            int movieId = Integer.parseInt(movieIdStr);
            movieService.addMovie(userId, movieId, listName);
            ctx.status(200).result("Movie added successfully");
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid movie ID format");
        } catch (Exception e) {
            ctx.status(500).result("Failed to add movie");
        }
    }

    private static void handleDeleteMovie(Context ctx) {
        addMovieRequest addMovieRequest = ctx.bodyAsClass(addMovieRequest.class);

        String userId = addMovieRequest.userId;
        String movieIdStr = addMovieRequest.movieId;
        String listName = addMovieRequest.listName;
        
        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }
        if (movieIdStr == null || movieIdStr.trim().isEmpty()) {
            ctx.status(400).result("Movie ID is required");
            return;
        }
        if (listName == null || listName.trim().isEmpty()) {
            ctx.status(400).result("List type is required");
            return;
        }
    
        try {
            int movieId = Integer.parseInt(movieIdStr);
            movieService.removeMovieFromList(userId, movieId, listName);
            ctx.status(200).result("Movie deleted successfully");
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid movie ID format");
        } catch (Exception e) {
            ctx.status(500).result("Failed to delete movie");
        }
    }

    //endregion

    //region search & Lists

    private static void handleSearchLocalMovies(Context ctx) {
        String query = ctx.queryParam("query");
    
        System.out.println("Search local query: " + query);
    
        // Input validation
        if (query == null || query.trim().isEmpty()) {
            ctx.status(400).result("Search query is required");
            return;
        }
    
        try {
            JsonNode localResults = movieService.searchLocalMovies(query);
    
            // Ensure results are in the expected format
            if (localResults == null || !localResults.isArray()) {
                ctx.status(500).result("Invalid response format from local search");
                return;
            }
    
            ctx.json(localResults);
        } catch (Exception e) {
            e.printStackTrace(); // Log the stack trace for debugging
            ctx.status(500).result("Failed to search local movies");
        }
    }
    
    private static void handleSearchTMDBMovies(Context ctx) {
        String query = ctx.queryParam("query");
    
        System.out.println("Search TMDB query: " + query);
    
        // Input validation
        if (query == null || query.trim().isEmpty()) {
            ctx.status(400).result("Search query is required");
            return;
        }
    
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            JsonNode tmdbResults = movieService.searchTMDBMovies(encodedQuery);
    
            // Extract only the first 20 results
            JsonNode results = tmdbResults.get("results");
            if (results == null || !results.isArray()) {
                ctx.status(500).result("Invalid response format from TMDB");
                return;
            }
    
            List<JsonNode> limitedResults = new ArrayList<>();
            for (int i = 0; i < Math.min(20, results.size()); i++) {
                limitedResults.add(results.get(i));
            }
    
            ctx.json(limitedResults.toArray());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Failed to search TMDB movies");
        }
    }

    private static void handleListWatchlist(Context ctx) {
        String userId = ctx.queryParam("userId");

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }

        try {
            JsonNode watchlist = movieService.listMoviesInUserList(userId, "watchlist");
            ctx.json(watchlist);
        } catch (Exception e) {
            e.printStackTrace(); // Log the stack trace for debugging
            ctx.status(500).result("Failed to list watchlist");
        }
    }

    private static void handleListWatched(Context ctx) {
        String userId = ctx.queryParam("userId");

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }

        try {
            var watched = movieService.listMoviesInUserList(userId, "watched");
            ctx.json(watched);
        } catch (Exception e) {
            ctx.status(500).result("Failed to list watched");
        }
    }

    private static void handleMoveToWatched(Context ctx) {
        addMovieRequest addMovieRequest = ctx.bodyAsClass(addMovieRequest.class);

        String userId = addMovieRequest.userId;
        String movieIdStr = addMovieRequest.movieId;

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }
        if (movieIdStr == null || movieIdStr.trim().isEmpty()) {
            ctx.status(400).result("Movie ID is required");
            return;
        }

        try {
            int movieId = Integer.parseInt(movieIdStr);
            movieService.moveMovieFromWatchlistToWatched(userId, movieId);
            ctx.status(200).result("Movie moved to watched list successfully");
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid movie ID format");
        } catch (Exception e) {
            ctx.status(500).result("Failed to move movie to watched list");
        }
    }

    //endregion
    
    //region

    private static void handleSearchUsers(Context ctx) {
        String query = ctx.queryParam("query");
        if (query == null || query.trim().isEmpty()) {
            ctx.status(400).result("Search query is required");
            return;
        }

        try {
            JsonNode users = userService.searchUsers(query);
            ctx.json(users);
        } catch (Exception e) {
            ctx.status(500).result("Failed to search users");
        }
    }

    private static void handleSendFriendRequest(Context ctx) {
        FriendRequest friendRequest = ctx.bodyAsClass(FriendRequest.class);

        String requesterId = friendRequest.requesterId;
        String requesteeId = friendRequest.requesteeId;

        if (requesterId == null || requesterId.trim().isEmpty() || requesteeId == null || requesteeId.trim().isEmpty()) {
            ctx.status(400).result("Requester ID and Requestee ID are required");
            return;
        }

        try {
            userService.sendFriendRequest(requesterId, requesteeId);
            ctx.status(200).result("Friend request sent");
        } catch (Exception e) {
            ctx.status(500).result("Failed to send friend request");
        }
    }

    private static void handleAcceptFriendRequest(Context ctx) {
        FriendRequest friendRequest = ctx.bodyAsClass(FriendRequest.class);

        String requesterId = friendRequest.requesterId;
        String requesteeId = friendRequest.requesteeId;

        if (requesterId == null || requesterId.trim().isEmpty() || requesteeId == null || requesteeId.trim().isEmpty()) {
            ctx.status(400).result("Requester ID and Requestee ID are required");
            return;
        }

        try {
            userService.acceptFriendRequest(requesterId, requesteeId);
            ctx.status(200).result("Friend request accepted");
        } catch (Exception e) {
            ctx.status(500).result("Failed to accept friend request");
        }
    }

    private static void handleDeclineFriendRequest(Context ctx) {
        FriendRequest friendRequest = ctx.bodyAsClass(FriendRequest.class);

        String requesterId = friendRequest.requesterId;
        String requesteeId = friendRequest.requesteeId;

        if (requesterId == null || requesterId.trim().isEmpty() || requesteeId == null || requesteeId.trim().isEmpty()) {
            ctx.status(400).result("Requester ID and Requestee ID are required");
            return;
        }

        try {
            userService.declineFriendRequest(requesterId, requesteeId);
            ctx.status(200).result("Friend request declined");
        } catch (Exception e) {
            ctx.status(500).result("Failed to decline friend request");
        }
    }

    private static void handleRemoveFriendRequest(Context ctx) {
        FriendRequest friendRequest = ctx.bodyAsClass(FriendRequest.class);

        String requesterId = friendRequest.requesterId;
        String requesteeId = friendRequest.requesteeId;

        if (requesterId == null || requesterId.trim().isEmpty() || requesteeId == null || requesteeId.trim().isEmpty()) {
            ctx.status(400).result("User ID and Friend ID are required");
            return;
        }

        try {
            userService.removeFriendRequest(requesterId, requesteeId);
            ctx.status(200).result("Friend request removed");
        } catch (Exception e) {
            ctx.status(500).result("Failed to remove friend");
        }
    }

    private static void handleRemoveFriend(Context ctx) {
        Friendship friendship = ctx.bodyAsClass(Friendship.class);

        String userId = friendship.userId;
        String friendId = friendship.friendId;

        if (userId == null || userId.trim().isEmpty() || friendId == null || friendId.trim().isEmpty()) {
            ctx.status(400).result("User ID and Friend ID are required");
            return;
        }

        try {
            userService.removeFriend(userId, friendId);
            ctx.status(200).result("Friend removed");
        } catch (Exception e) {
            ctx.status(500).result("Failed to remove friend");
        }
    }

    private static void handleListFriends(Context ctx) {
        String userId = ctx.queryParam("userId");
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }

        try {
            JsonNode friends = userService.listAllFriends(userId);
            ctx.json(friends);
        } catch (Exception e) {
            ctx.status(500).result("Failed to list friends");
        }
    }

    private static void handleListFriendRequests(Context ctx) {
        String userId = ctx.queryParam("userId");
        if (userId == null || userId.trim().isEmpty()) {
            ctx.status(400).result("User ID is required");
            return;
        }

        try {
            JsonNode friendRequests = userService.listFriendRequests(userId);
            ctx.json(friendRequests);
        } catch (Exception e) {
            System.out.println(e);
            ctx.status(500).result("Failed to list friend requests");
        }
    }

    //endregion
}
