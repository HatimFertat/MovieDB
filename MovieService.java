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

    public static JsonNode searchMovies(String query) throws IOException, ClientProtocolException {
        String url = TMDB_BASE_URL + "/search/movie?api_key=" + TMDB_API_KEY + "&query=" + query;
        String response = Request.Get(url).execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(response);
    }

    public static void main(String[] args) {
        try {
            ScalarDBOperations.initialize();
            if (args.length == 1) {
                String query = args[0];
                JsonNode searchResults = searchMovies(query);
                System.out.println(searchResults);
            } else if (args.length == 3) {
                int userId = Integer.parseInt(args[0]);
                int movieId = Integer.parseInt(args[1]);
                String listName = args[2];
                JsonNode movieDetails = fetchMovieDetails(movieId);

                Map<String, String> movieDetailsMap = new HashMap<>();
                movieDetailsMap.put("movieId", String.valueOf(movieDetails.get("id").asInt()));
                movieDetailsMap.put("title", movieDetails.get("title").asText());
                movieDetailsMap.put("release_date", movieDetails.get("release_date").asText());
                movieDetailsMap.put("director", movieDetails.get("credits").get("crew").findValuesAsText("job").stream()
                        .filter(job -> job.equals("Director")).findFirst().orElse("N/A"));
                movieDetailsMap.put("cast", String.join(", ", movieDetails.get("credits").get("cast").findValuesAsText("name").subList(0, 5)));
                movieDetailsMap.put("synopsis", movieDetails.get("overview").asText());
                movieDetailsMap.put("genres", String.join(", ", movieDetails.get("genres").findValuesAsText("name")));

                ScalarDBOperations.addMovieToMoviesTable(movieDetailsMap);
                ScalarDBOperations.addMovieToList(userId, Integer.parseInt(movieDetailsMap.get("movieId")), listName);
            }
        } catch (Exception e) {
            System.err.println("Error in MovieService: " + e.getMessage());
        }
    }
}
