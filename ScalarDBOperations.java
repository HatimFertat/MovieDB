import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
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
import java.util.HashMap;
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

    public static void addMovieToList(int userId, Map<String, String> movieDetails, String listName) throws TransactionException {
        if (isMovieInList(userId, Integer.parseInt(movieDetails.get("movieId")), listName)) {
            System.out.println("Movie is already in " + listName);
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
    
            Put putList = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(listName)
                .partitionKey(Key.ofInt("userId", userId))
                .clusteringKey(Key.ofInt("movieId", Integer.parseInt(movieDetails.get("movieId"))))
                .build();
            tx.put(putList);
    
            tx.commit();
            System.out.println("Movie added to " + listName + " successfully");
    
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
            System.out.println("Failed to add movie to " + listName + ": " + e.getMessage());
        }
    }
}
