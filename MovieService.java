import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.exception.transaction.TransactionException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.ClientProtocolException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MovieService {
    private static final String TMDB_API_KEY = "81e872ea74c4fe76eed2dd856b47223d";
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

    public static JsonNode fetchMovieDetails(int movieId) throws IOException, ClientProtocolException {
        String url = TMDB_BASE_URL + "/movie/" + movieId + "?api_key=" + TMDB_API_KEY + "&append_to_response=credits";
        String response = Request.Get(url).execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(response);
    }

    public static void main(String[] args) {
        try {
            ScalarDBOperations.initialize();
            int movieId = 550;  // Example movie ID
            int userId = 1;     // Example user ID
            JsonNode movieDetails = fetchMovieDetails(movieId);

            Map<String, String> movieDetailsMap = new HashMap<>();
            movieDetailsMap.put("movieId", String.valueOf(movieDetails.get("id").asInt()));
            movieDetailsMap.put("title", movieDetails.get("title").asText());
            movieDetailsMap.put("release_date", movieDetails.get("release_date").asText());
            movieDetailsMap.put("director", movieDetails.get("credits").get("crew").findValuesAsText("name").stream()
                    .filter(role -> role.equals("Director")).findFirst().orElse("N/A"));
            movieDetailsMap.put("cast", String.join(", ", movieDetails.get("credits").get("cast").findValuesAsText("name").subList(0, 5)));
            movieDetailsMap.put("synopsis", movieDetails.get("overview").asText());
            movieDetailsMap.put("genres", String.join(", ", movieDetails.get("genres").findValuesAsText("name")));

            ScalarDBOperations.addMovieToList(userId, movieDetailsMap, "watchlist");
            // ScalarDBOperations.addMovieToList(userId, movieDetailsMap, "watched");
        } catch (Exception e) {
            System.err.println("Error in MovieService: ");
        }
    }
}