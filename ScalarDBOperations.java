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
    

    public static void addMovieToList(int userId, Map<String, String> movieDetails, String listName) throws TransactionException {
        DistributedTransaction tx = manager.start();
        try {
            String moviesTable = "movies";

            Put putMovie = new Put(new Key("movieId", Integer.parseInt(movieDetails.get("movieId"))))
                .forNamespace(NAMESPACE)
                .forTable(moviesTable)
                .withValue("title", movieDetails.get("title"))
                .withValue("release_date", movieDetails.get("release_date"))
                .withValue("director", movieDetails.get("director"))
                .withValue("cast", movieDetails.get("cast"))
                .withValue("synopsis", movieDetails.get("synopsis"))
                .withValue("genres", movieDetails.get("genres"));
            tx.put(putMovie);
    
            Put putList = new Put(new Key("watchlistId", userId))
                .forNamespace(NAMESPACE)
                .forTable(listName)
                .withValue("movie_id", Integer.parseInt(movieDetails.get("movieId")));
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
