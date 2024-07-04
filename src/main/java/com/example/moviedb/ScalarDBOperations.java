package com.example.moviedb;

import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Put;
import com.scalar.db.api.Delete;
import com.scalar.db.api.Result;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.db.service.TransactionFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class ScalarDBOperations {

    private DistributedTransactionManager manager;
    private String NAMESPACE = "moviedb";


    public void initialize() throws Exception{
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File("src/main/resources/scalardb.properties")));
    
            TransactionFactory factory = TransactionFactory.create(prop);
            manager = factory.getTransactionManager();
        } catch (Exception e) {
            System.err.println("Failed to initialize ScalarDB transaction manager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Initialization of ScalarDB failed.", e);
        }
    }

    //region Check if a movie is present
    public boolean isUserInUsersTable(String userId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getUser = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("users")
                .partitionKey(Key.ofText("userId", userId))
                .build();

            Optional<Result> result = tx.get(getUser);
            tx.commit();

            return result.isPresent();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to check if user is in users table: ", e.getMessage());
        }
    }
    

    public boolean isMovieInMoviesTable(int movieId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getMovie = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("movies")
                .partitionKey(Key.ofInt("movieId", movieId))
                .build();

            Optional<Result> result = tx.get(getMovie);
            tx.commit();

            return result.isPresent();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to check if movie is in movies table: ", e.getMessage());
        }
    }

    public boolean isMovieInList(String userId, int movieId, String listName) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getMovie = Get.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();

            Optional<Result> result = tx.get(getMovie);
            tx.commit();

            return result.isPresent();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to check if movie is in " + listName + ": ", e.getMessage());
        }
    }
    
    //endregion

    //region add and remove movies
    public void addMovieToMoviesTable(Map<String, String> movieDetails) throws TransactionException {
        if (isMovieInMoviesTable(Integer.parseInt(movieDetails.get("id")))) {
            System.out.println("Movie is already in movies table");
            return;
        }
        DistributedTransaction tx = manager.start();
        try {
            String moviesTable = "movies";
    
            Put putMovie = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(moviesTable)
                .partitionKey(Key.ofInt("movieId", Integer.parseInt(movieDetails.get("id"))))
                .textValue("title", movieDetails.get("title"))
                .textValue("release_date", movieDetails.get("release_date"))
                .textValue("poster_path", movieDetails.get("poster_path"))
                .textValue("genre_ids", movieDetails.get("genre_ids"))
                .build();
            tx.put(putMovie);
    
            tx.commit();
            System.out.println("Movie added to movies table successfully");
    
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();  // Print the stack trace to understand the exception better
            System.out.println("Failed to add movie to movies table: " + e.getMessage());
        }
    }
    
    public void addMovieToList(String userId, int movieId, String listName) throws TransactionException {
        if (isMovieInList(userId, movieId, listName)) {
            System.out.println("Movie is already in " + listName);
            return;
        }
    
        DistributedTransaction tx = manager.start();
        try {
            Put putList = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();
            tx.put(putList);
    
            tx.commit();
            System.out.println("Movie added to " + listName + " successfully");
    
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();  // Print the stack trace to understand the exception better
            System.out.println("Failed to add movie to " + listName + ": " + e.getMessage());
        }
    }
    
    public void removeMovieFromList(String userId, int movieId, String listName) throws TransactionException {
        if (!isMovieInList(userId, movieId, listName)) {
            System.out.println("Unable to remove a movie that is not in" + listName);
            return;
        }
        DistributedTransaction tx = manager.start();
        try {
            // Build the delete request
            Delete deleteMovie = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();
    
            // Execute the delete request
            tx.delete(deleteMovie);
            tx.commit();
            System.out.println("Movie removed from " + listName + " successfully");
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();  // Print the stack trace to understand the exception better
            System.out.println("Failed to remove movie from " + listName + ": " + e.getMessage());
        }
    }
    
    public void moveMovieFromWatchlistToWatched(String userId, int movieId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Check if the movie is in the watchlist
            Get getWatchlistMovie = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("watchlist")
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();
            Optional<Result> watchlistResult = tx.get(getWatchlistMovie);
    
            if (!watchlistResult.isPresent()) {
                tx.rollback();
                System.out.println("Movie is not in the watchlist.");
                return;
            }
    
            // Add the movie to the watched list
            Put putWatchedMovie = Put.newBuilder()
                .namespace(NAMESPACE)
                .table("watched")
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();
            tx.put(putWatchedMovie);
    
            // Remove the movie from the watchlist
            Delete deleteWatchlistMovie = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("watchlist")
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();
            tx.delete(deleteWatchlistMovie);
    
            // Commit the transaction
            tx.commit();
            System.out.println("Movie moved from watchlist to watched successfully");
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            System.out.println("Failed to move movie from watchlist to watched: " + e.getMessage());
        }
    }
    
    //endregion
        
    //region search

    public List<Map<String, String>> searchMovies(String query) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("movies")
                .all()
                .build();
    
            List<Result> results = tx.scan(scan);
            tx.commit();
    
            List<Map<String, String>> movies = results.stream()
                .filter(result -> result.getText("title").toLowerCase().contains(query.toLowerCase()))
                .map(result -> {
                    Map<String, String> movieDetails = new HashMap<>();
                    movieDetails.put("id", String.valueOf(result.getInt("movieId")));
                    movieDetails.put("title", result.getText("title"));
                    movieDetails.put("release_date", result.getText("release_date"));
                    movieDetails.put("poster_path", result.getText("poster_path"));
                    movieDetails.put("genre_ids", result.getText("genre_ids"));
                    return movieDetails;
                })
                .collect(Collectors.toList());
    
            return movies;
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to search movies: ", e.getMessage());
        }
    }

    
    public List<Map<String, String>> listMoviesInUserList(String userId, String listName) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Retrieve the list of movie IDs from the user's watchlist or watched list
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofText("userId", userId))
                .build();
    
            List<Result> results = tx.scan(scan);
            tx.commit();
    
            List<Map<String, String>> movies = new ArrayList<>();
            for (Result result : results) {
                int movieId = result.getInt("movieId");
                Map<String, String> movieDetails = getMovieDetails(movieId);
                if (movieDetails != null) {
                    movies.add(movieDetails);
                }
            }
            return movies;
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to list movies in user list: ", e.getMessage());
        }
    }
    
    
    

    public Map<String, String> getMovieDetails(int movieId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getMovie = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("movies")
                .partitionKey(Key.ofInt("movieId", movieId))
                .build();
    
            Optional<Result> result = tx.get(getMovie);
            tx.commit();
    
            if (result.isPresent()) {
                Map<String, String> movieDetails = new HashMap<>();
                movieDetails.put("id", String.valueOf(result.get().getInt("movieId")));
                movieDetails.put("title", result.get().getText("title"));
                movieDetails.put("release_date", result.get().getText("release_date"));
                movieDetails.put("poster_path", result.get().getText("poster_path"));
                movieDetails.put("genre_ids", result.get().getText("genre_ids"));
                return movieDetails;
            } else {
                System.out.println("Movie not found");
                return null;
            }
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to get movie details: ", e.getMessage());
        }
    }


    public Map<String, String> getUserDetails(String userId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getUser = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("users")
                .partitionKey(Key.ofText("userId", userId))
                .build();
    
            Optional<Result> result = tx.get(getUser);
            tx.commit();
    
            if (result.isPresent()) {
                Result userResult = result.get();
                Map<String, String> userDetails = new HashMap<>();
                userDetails.put("userId", userResult.getText("userId"));
                userDetails.put("email", userResult.getText("email"));
                userDetails.put("password", userResult.getText("password"));
                // Add any other fields you have in your users table
                return userDetails;
            } else {
                return null;
            }
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to get user details: ", e.getMessage());
        }
    }
    
    //endregion

    //region friends

    public void addUserToUsersTable(Map<String, String> userDetails) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            String usersTable = "users";
    
            Put putUser = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(usersTable)
                .partitionKey(Key.ofText("userId", userDetails.get("userId")))
                .textValue("password", userDetails.get("password"))
                .textValue("email", userDetails.get("email"))
                .build();
            tx.put(putUser);
    
            tx.commit();
            System.out.println("User added to users table successfully");
    
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();  // Print the stack trace to understand the exception better
            System.out.println("Failed to add user to users table: " + e.getMessage());
        }
    }
    

    public void sendFriendRequest(String requesterId, String requesteeId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Put putRequest = Put.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .partitionKey(Key.ofText("requester_id", requesterId))
                .clusteringKey(Key.ofText("requestee_id", requesteeId))
                .textValue("status", "pending")
                .build();
    
            tx.put(putRequest);
            tx.commit();
            System.out.println("Friend request sent successfully");
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            System.out.println("Failed to send friend request: " + e.getMessage());
        }
    }
    
    public void acceptFriendRequest(String requesterId, String requesteeId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Check if the friend request exists and is pending
            Get getRequest = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .partitionKey(Key.ofText("requester_id", requesterId))
                .clusteringKey(Key.ofText("requestee_id", requesteeId))
                .build();
    
            Optional<Result> result = tx.get(getRequest);
    
            if (result.isPresent() && "pending".equals(result.get().getText("status"))) {
                // Add to friends tabl
                Put putFriend1 = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friends")
                    .partitionKey(Key.ofText("userId", requesterId))
                    .clusteringKey(Key.ofText("friendId", requesteeId))
                    .build();
                tx.put(putFriend1);
    
                Put putFriend2 = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friends")
                    .partitionKey(Key.ofText("userId", requesteeId))
                    .clusteringKey(Key.ofText("friendId", requesterId))
                    .build();
                tx.put(putFriend2);
    
                // Update friend request status to accepted
                Put updateRequest = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friend_requests")
                    .partitionKey(Key.ofText("requester_id", requesterId))
                    .clusteringKey(Key.ofText("requestee_id", requesteeId))
                    .textValue("status", "accepted")
                    .build();
                tx.put(updateRequest);
    
                tx.commit();
                System.out.println("Friend request accepted successfully");
            } else {
                tx.rollback();
                System.out.println("No pending friend request found to accept");
            }
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            System.out.println("Failed to accept friend request: " + e.getMessage());
        }
    }
    
    public void declineFriendRequest(String requesterId, String requesteeId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Check if the friend request exists and is pending
            Get getRequest = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .partitionKey(Key.ofText("requester_id", requesterId))
                .clusteringKey(Key.ofText("requestee_id", requesteeId))
                .build();
    
            Optional<Result> result = tx.get(getRequest);
    
            if (result.isPresent() && "pending".equals(result.get().getText("status"))) {
                // Update friend request status to declined
                Put updateRequest = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friend_requests")
                    .partitionKey(Key.ofText("requester_id", requesterId))
                    .clusteringKey(Key.ofText("requestee_id", requesteeId))
                    .textValue("status", "declined")
                    .build();
                tx.put(updateRequest);
    
                tx.commit();
                System.out.println("Friend request declined successfully");
            } else {
                tx.rollback();
                System.out.println("No pending friend request found to decline");
            }
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            System.out.println("Failed to decline friend request: " + e.getMessage());
        }
    }

    public void removeFriend(String userId, String friendId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Remove friend from the user's friend list
            Delete deleteFriend1 = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofText("friendId", friendId))
                .build();
            tx.delete(deleteFriend1);
    
            // Remove friend from the friend's friend list
            Delete deleteFriend2 = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofText("userId", friendId))
                .clusteringKey(Key.ofText("friendId", userId))
                .build();
            tx.delete(deleteFriend2);
    
            tx.commit();
            System.out.println("Friend removed successfully");
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            System.out.println("Failed to remove friend: " + e.getMessage());
        }
    }

    public boolean areFriends(String userId, String friendId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getFriend = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofText("userId", userId))
                .clusteringKey(Key.ofText("friendId", friendId))
                .build();
    
            Optional<Result> result = tx.get(getFriend);
            tx.commit();
    
            return result.isPresent();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to check friendship status: ", e.getMessage());
        }
    }
    
    public List<Map<String, String>> listAllFriends(String userId) throws TransactionException {
        List<Map<String, String>> friends = new ArrayList<>();
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofText("userId", userId))
                .build();
    
            List<Result> results = tx.scan(scan);
            for (Result result : results) {
                Map<String, String> friend = new HashMap<>();
                friend.put("userId", String.valueOf(result.getInt("userId")));
                friend.put("friendId", String.valueOf(result.getInt("movieId")));
                friends.add(friend);
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to list friends: ", e.getMessage());
        }
        return friends;
    }
    
    public List<Map<String, String>> listFriendRequests(String userId) throws TransactionException {
        List<Map<String, String>> friendRequests = new ArrayList<>();
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .indexKey(Key.ofText("requestee_id", userId))
                .build();
    
            List<Result> results = tx.scan(scan);
            for (Result result : results) {
                if ("pending".equals(result.getText("status"))) {
                    Map<String, String> request = new HashMap<>();
                    request.put("requester_id", String.valueOf(result.getInt("requester_id")));
                    request.put("requestee_id", String.valueOf(result.getInt("requestee_id")));
                    request.put("status", result.getText("status"));
                    friendRequests.add(request);
                }
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to list friend requests: ", e.getMessage());
        }
        return friendRequests;
    }
    
    //endregion


}
