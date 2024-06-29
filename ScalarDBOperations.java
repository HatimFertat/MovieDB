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
import com.scalar.db.config.DatabaseConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class ScalarDBOperations {
    private static DistributedTransactionManager manager;
    private static String NAMESPACE = "moviedb";


    public static void initialize() {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File("scalardb.properties")));
    
            TransactionFactory factory = TransactionFactory.create(prop);
            manager = factory.getTransactionManager();
        } catch (Exception e) {
            System.err.println("Failed to initialize ScalarDB transaction manager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Initialization of ScalarDB failed.", e);
        }
    }

    //region Check if a movie is present

    public static boolean isMovieInMoviesTable(int movieId) throws TransactionException {
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

    public static boolean isMovieInList(int userId, int movieId, String listName) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getMovie = Get.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofInt("userId", userId))
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
    public static void addMovieToMoviesTable(Map<String, String> movieDetails) throws TransactionException {
        if (isMovieInMoviesTable(Integer.parseInt(movieDetails.get("movieId")))) {
            System.out.println("Movie is already in movies table");
            return;
        }
        DistributedTransaction tx = manager.start();
        try {
            String moviesTable = "movies";
    
            Put putMovie = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(moviesTable)
                .partitionKey(Key.ofInt("movieId", Integer.parseInt(movieDetails.get("movieId"))))
                .textValue("title", movieDetails.get("title"))
                .textValue("release_date", movieDetails.get("release_date"))
                .textValue("director", movieDetails.get("director"))
                .textValue("cast", movieDetails.get("cast"))
                .textValue("synopsis", movieDetails.get("synopsis"))
                .textValue("genres", movieDetails.get("genres"))
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
    
    public static void addMovieToList(int userId, int movieId, String listName) throws TransactionException {
        if (isMovieInList(userId, movieId, listName)) {
            System.out.println("Movie is already in " + listName);
            return;
        }
    
        DistributedTransaction tx = manager.start();
        try {
            Put putList = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofInt("userId", userId))
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
    
    public static void removeMovieFromList(int userId, int movieId, String listName) throws TransactionException {
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
                .partitionKey(Key.ofInt("userId", userId))
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
    
    public static void moveMovieFromWatchlistToWatched(int userId, int movieId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Check if the movie is in the watchlist
            Get getWatchlistMovie = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("watchlist")
                .partitionKey(Key.ofInt("userId", userId))
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
                .partitionKey(Key.ofInt("userId", userId))
                .clusteringKey(Key.ofInt("movieId", movieId))
                .build();
            tx.put(putWatchedMovie);
    
            // Remove the movie from the watchlist
            Delete deleteWatchlistMovie = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("watchlist")
                .partitionKey(Key.ofInt("userId", userId))
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

    public static List<Map<String, String>> searchMovies(String title, String director, String genre) throws TransactionException {
        List<Map<String, String>> movies = new ArrayList<>();
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("movies")
                .all()
                .build();
    
            List<Result> results = tx.scan(scan);
            for (Result result : results) {
                boolean matches = true;
                if (title != null && !result.getText("title").contains(title)) {
                    matches = false;
                }
                if (director != null && !result.getText("director").contains(director)) {
                    matches = false;
                }
                if (genre != null && !result.getText("genres").contains(genre)) {
                    matches = false;
                }
                if (matches) {
                    Map<String, String> movie = new HashMap<>();
                    movie.put("movieId", String.valueOf(result.getInt("movieId")));
                    movie.put("title", result.getText("title"));
                    movie.put("release_date", result.getText("release_date"));
                    movie.put("director", result.getText("director"));
                    movie.put("cast", result.getText("cast"));
                    movie.put("synopsis", result.getText("synopsis"));
                    movie.put("genres", result.getText("genres"));
                    movies.add(movie);
                }
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to search movies: ", e.getMessage());
        }
        return movies;
    }
    
    public static List<Map<String, String>> listMoviesInUserList(int userId, String listName) throws TransactionException {
        List<Map<String, String>> movies = new ArrayList<>();
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofInt("userId", userId))
                .build();
    
            List<Result> results = tx.scan(scan);
            for (Result result : results) {
                Map<String, String> movie = new HashMap<>();
                movie.put("userId", String.valueOf(result.getInt("userId")));
                movie.put("movieId", String.valueOf(result.getInt("movieId")));
                movies.add(movie);
            }
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to list movies in " + listName + ": ", e.getMessage());
        }
        return movies;
    }

    public static Map<String, String> getMovieDetails(int movieId) throws TransactionException {
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
                movieDetails.put("movieId", String.valueOf(result.get().getInt("movieId")));
                movieDetails.put("title", result.get().getText("title"));
                movieDetails.put("release_date", result.get().getText("release_date"));
                movieDetails.put("director", result.get().getText("director"));
                movieDetails.put("cast", result.get().getText("cast"));
                movieDetails.put("synopsis", result.get().getText("synopsis"));
                movieDetails.put("genres", result.get().getText("genres"));
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
    
    //endregion

    //region friends

    public static void sendFriendRequest(int requesterId, int requesteeId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Put putRequest = Put.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .partitionKey(Key.ofInt("requester_id", requesterId))
                .clusteringKey(Key.ofInt("requestee_id", requesteeId))
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
    
    public static void acceptFriendRequest(int requesterId, int requesteeId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Check if the friend request exists and is pending
            Get getRequest = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .partitionKey(Key.ofInt("requester_id", requesterId))
                .clusteringKey(Key.ofInt("requestee_id", requesteeId))
                .build();
    
            Optional<Result> result = tx.get(getRequest);
    
            if (result.isPresent() && "pending".equals(result.get().getValue("status").get().getAsString())) {
                // Add to friends table
                Put putFriend1 = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friends")
                    .partitionKey(Key.ofInt("userId", requesterId))
                    .clusteringKey(Key.ofInt("friendId", requesteeId))
                    .build();
                tx.put(putFriend1);
    
                Put putFriend2 = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friends")
                    .partitionKey(Key.ofInt("userId", requesteeId))
                    .clusteringKey(Key.ofInt("friendId", requesterId))
                    .build();
                tx.put(putFriend2);
    
                // Update friend request status to accepted
                Put updateRequest = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friend_requests")
                    .partitionKey(Key.ofInt("requester_id", requesterId))
                    .clusteringKey(Key.ofInt("requestee_id", requesteeId))
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
    
    public static void declineFriendRequest(int requesterId, int requesteeId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Check if the friend request exists and is pending
            Get getRequest = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .partitionKey(Key.ofInt("requester_id", requesterId))
                .clusteringKey(Key.ofInt("requestee_id", requesteeId))
                .build();
    
            Optional<Result> result = tx.get(getRequest);
    
            if (result.isPresent() && "pending".equals(result.get().getValue("status").get().getAsString())) {
                // Update friend request status to declined
                Put updateRequest = Put.newBuilder()
                    .namespace(NAMESPACE)
                    .table("friend_requests")
                    .partitionKey(Key.ofInt("requester_id", requesterId))
                    .clusteringKey(Key.ofInt("requestee_id", requesteeId))
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

    public static void removeFriend(int userId, int friendId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            // Remove friend from the user's friend list
            Delete deleteFriend1 = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofInt("userId", userId))
                .clusteringKey(Key.ofInt("friendId", friendId))
                .build();
            tx.delete(deleteFriend1);
    
            // Remove friend from the friend's friend list
            Delete deleteFriend2 = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofInt("userId", friendId))
                .clusteringKey(Key.ofInt("friendId", userId))
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

    public static boolean areFriends(int userId, int friendId) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            Get getFriend = Get.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofInt("userId", userId))
                .clusteringKey(Key.ofInt("friendId", friendId))
                .build();
    
            Optional<Result> result = tx.get(getFriend);
            tx.commit();
    
            return result.isPresent();
        } catch (Exception e) {
            tx.rollback();
            throw new TransactionException("Failed to check friendship status: ", e.getMessage());
        }
    }
    
    public static List<Map<String, String>> listAllFriends(int userId) throws TransactionException {
        List<Map<String, String>> friends = new ArrayList<>();
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("friends")
                .partitionKey(Key.ofInt("userId", userId))
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
    
    public static List<Map<String, String>> listFriendRequests(int userId) throws TransactionException {
        List<Map<String, String>> friendRequests = new ArrayList<>();
        DistributedTransaction tx = manager.start();
        try {
            Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("friend_requests")
                .indexKey(Key.ofInt("requestee_id", userId))
                .build();
    
            List<Result> results = tx.scan(scan);
            for (Result result : results) {
                if ("pending".equals(result.getValue("status").get().getAsString())) {
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
