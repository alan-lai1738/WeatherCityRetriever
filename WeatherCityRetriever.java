/*
 * Alan Lai
 * 10/2021
 * Weather and Nearby Cities API Project
 * 
 * Java program that takes in a name of a city and outputs the weather and nearby cities of that city.
 * 
 * Given a city, the program outputs to console:
 * - weather description, tempera ture, humidity, wind speed, and cloudiness of the city.
 * - a list of multiple cities near that city.
 * 
 * This program uses two RESTful APIs for its information:
 * OpenWeatherMap Weather API
 * GeoBytes GetNearbyCities API
 * 
 * This program deserializes JSON with JSONArray, JSONObject, and GSON.
 * 
 * Referenced Libraries used:
 * gson-2.8.8.jar
 * java-json.jar
 * 
 * In cases of failure, this program uses exponential back-off to reattempt connection.
 * HOWEVER!: The GeoBytes API is dependent on longitude/latitude from the Weather API so
 * it can only run IF the Weather API is up. In the other case, the weather API can run
 * even if the GeoBytes NearbyCities API is down.
 * 
 * How to use:
 * javac WeatherCityRetriever.java
 * java WeatherCityRetriever <cityName>
 * GETTING ERRORS On Local IDE? Put The two JAR files into Referenced Libraries.
 */
// Imports: HTTPURLConnection classes, URL Class
import java.net.HttpURLConnection;
import java.net.URL;
 
// Imports: String/Object Readers/Syntax Helpers/Converters
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

// Imports JSON Object Helpers
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.*;


class WeatherCityRetriever {
    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        StringBuilder cityName = new StringBuilder();
        if(args.length <= 0) {
            System.out.println("Error: Invalid Input. Please use as: java MyCity <city name>");
            System.exit(-1);
        }
        if(args.length > 1) {
            for(int i = 0; i < args.length; i++) {
                if(i != args.length -1) {
                    cityName.append(args[i] + "");
                } else {
                    cityName.append(args[i]);
                }
            }
        } else {
            cityName.append(args[0]);
        }
        JSONObject jsonWeatherObj = getWeatherJSON(cityName.toString());
        // JSONObject jsonWeatherObj = getWeatherJSON("Los Angeles");
        //System.out.println(jsonWeatherObj.toString());
        
        // Failure case: if json WeatherObj is null it will not run the rest.
        if(!(jsonWeatherObj==null)) {
            printWeatherOutput(jsonWeatherObj);
            JsonElement nearbyCities = getNearbyCities("http://getnearbycities.geobytes.com/GetNearbyCities?radius=100&longitude=" + jsonWeatherObj.getJSONObject("coord").get("lon") + "&latitude=" + jsonWeatherObj.getJSONObject("coord").get("lat"));
            printNearbyCities(nearbyCities);
        }
        
    
    }

    /* getWeatherJSON()
     * Parameters: cityName (Name of City we want weather for)
     * Returns: JSONObject of weather information in the city, cityName.
     */
    public static JSONObject getWeatherJSON(String cityName) throws IOException, JSONException {
        // https://dzone.com/articles/how-to-parse-json-data-from-a-rest-api-using-simpl
        try {
            // Set up HTTP Connection.
            String url = "https://api.openweathermap.org/data/2.5/weather?q="+cityName+"&units=imperial&appid=328eacbd0f6a8722d4582f97891d0389";
            URL urlObj =  new URL(url);

            // Open REST API connection, use GET to retrieve data
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Getting the response code.
            int responseCode = connection.getResponseCode();

            // Backoff/Retry Logic Variables
            int maxAttempts = 5;
            int currentAttempt = 1;
            int stoppageTime = 250;


            // INVALID RESPONSE: 0-199. Print this out to console.
            if (responseCode < 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            // Exponential Backoff: In cases of 429/500+ response codes, we run a retry logic to reattempt to connect to the API.
            } else if(responseCode > 499 && responseCode < 600 ||  responseCode == 429) {
                System.out.println("System is attempting exponential backoff..");
                do {
                    connection = (HttpURLConnection) urlObj.openConnection();
                    System.out.println("Retrying connection: Attempt " + currentAttempt);
                    // We wait for {stoppageTime} amount of time to reattempt connection.
                    Thread.sleep(stoppageTime);
                    // Every time we try and fail, we increase wait time to retry.
                    stoppageTime*=2;
                    // Incrememnt attempts up to 5 times.
                    ++currentAttempt;
                    // Update response code every retry attempt.
                    responseCode = connection.getResponseCode();
                } while(currentAttempt <= maxAttempts && responseCode > 499 && responseCode < 600 || responseCode == 429);
                
                // In case where responseCode is still the same after 5 attempts, were going to say our system is down and retries failed.
                if(responseCode > 499 && responseCode < 600 || responseCode == 429){
                    System.out.println("Backoff unsuccessful after 5 attempts. Please try again later. Exiting");
                    System.exit(0);
                }
            }

            // InputStream takes in JSON Data from the API.
            InputStream istream;
            // The data is then opened from our API through inputstream from our URL.
            istream = new URL(url).openStream();
            // Next, our BufferedReader takes in istream data.
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream, Charset.forName("UTF-8")));
            // We then turn our BR data and convert it into a string.
            String json = readString(reader);
            // Build a JSON Object that takes a string
            JSONObject jsonObj = new JSONObject(json);
            // Close Stream (good practice)
            istream.close();
            // Return final result.
            return jsonObj;
        } catch(Exception e) {
            // In cases where there is an Invalid City Name or input, we display message to console.
            System.out.println(cityName + " is an Invalid input. Please use a city name.");
        }
        return null;
    }
    /* readString()
     * Parameters: toRead (BufferedReader Data filled with our JSON information.)
     * Returns: JSON Information as String
     */
    private static String readString(Reader toRead) throws IOException {
        // We use a string builder since this is a large string of data.
        StringBuilder sb = new StringBuilder();
        int num;
        // Iterate through data in the BufferedReader, and add it to the SB.
        while ((num = toRead.read()) != -1) {
            sb.append((char) num);
        }
        // Use StringBuilder's toString method to return a string.
        return sb.toString();
    }

    /* getNearbyCities()
     * Parameters: URL (Since we take longitude and latitude from our Weather JSON, it's put into the string and into our URL of this second API.)
     * Returns: JSON Information of nearby cities as JsonElement
     */
    private static JsonElement getNearbyCities(String url) throws IOException, InterruptedException {
        // Set up HTTP Connection.
        URL urlObj =  new URL(url);

        // Open REST API connection, use GET to retrieve data
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        // Getting the response code.
        int responseCode = connection.getResponseCode();        

         // Backoff/Retry Logic Variables
         int maxAttempts = 5;
         int currentAttempt = 1;
         int stoppageTime = 250;


         // INVALID RESPONSE: 0-199. Print this out to console.
         if (responseCode < 200) {
             throw new RuntimeException("HttpResponseCode: " + responseCode);
         // Exponential Backoff: In cases of 429/500+ response codes, we run a retry logic to reattempt to connect to the API.
         } else if(responseCode > 499 && responseCode < 600 ||  responseCode == 429) {
             System.out.println("System is attempting exponential backoff..");
             do {
                 connection = (HttpURLConnection) urlObj.openConnection();
                 System.out.println("Retrying connection: Attempt " + currentAttempt);
                 // We wait for {stoppageTime} amount of time to reattempt connection.
                 Thread.sleep(stoppageTime);
                 // Every time we try and fail, we increase wait time to retry.
                 stoppageTime*=2;
                 // Incrememnt attempts up to 5 times.
                 ++currentAttempt;
                 // Update response code every retry attempt.
                 responseCode = connection.getResponseCode();
             } while(currentAttempt <= maxAttempts && responseCode > 499 && responseCode < 600 || responseCode == 429);
             
             // In case where responseCode is still the same after 5 attempts, were going to say our system is down and retries failed.
             if(responseCode > 499 && responseCode < 600 || responseCode == 429){
                System.out.println("Backoff unsuccessful after 5 attempts. Please try again later. Exiting");
                System.exit(0);
            }
         }
        // We make if here if our Response code is valid! Run as usual.

        // InputStream takes in JSON Data from the API.
        InputStream istream;
        // The data is then opened from our API through inputstream from our URL.
        istream = new URL(url).openStream();
        try {
            // Next, our BufferedReader takes in istream data.
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream, Charset.forName("UTF-8")));
            // We then parse out BR data into a JsonElement and return.
            return JsonParser.parseReader(reader);
        } finally {
            // Close istream for good practice/closing dangling instreams
            istream.close();
        }
    }

    /* printNearbyCities()
     * Parameters: JsonElement that contains information of nearby cities
     * Prints out elements of Nearby Cities Element to console for output.
     */
    private static void printNearbyCities(JsonElement citiesElement) {
        JsonArray citiesAsJsonArray = citiesElement.getAsJsonArray();
        // BREAK CASE: Invalid Array Size / No Nearby Cities found
        if(citiesAsJsonArray.size() <= 1) {
            return;
        }
        System.out.println("|_______________________[Nearby Cities]_______________________|");
        int i = 0;
        for(JsonElement city : citiesAsJsonArray){
            if(i > 0){
                String[] output = city.toString().split(",");
                System.out.println(output[1].substring(1, output[1].length()-1) + ", " + output[2].substring(1, output[2].length()-1));
            }
            i++;
        }
    }
    
    /* printWeatherOutput()
     * Parameters: JSONObject that contains our Weather data.
     * Prints out description, tempearture, humidity, wind speed, and cloudiness of location.
     */
    private static void printWeatherOutput(JSONObject json) throws JSONException {
        // Prints out name of city with country if found, otherwise just city name
        try {
            System.out.println("|_______________________[Weather in " + json.get("name") + ", " + json.getJSONObject("sys").get("country") + "]_______________________|");
        } catch(JSONException e) {
            System.out.println("|_______________________[Weather in " + json.get("name") +  "]_______________________|");
        }
        JSONArray weatherData = json.getJSONArray("weather");
        System.out.println("    Currently, the weather is " + weatherData.getJSONObject(0).get("main") + " (" + weatherData.getJSONObject(0).get("description") + ").");
        System.out.println("    The current temperature is  " + json.getJSONObject("main").get("temp") + "°F," + " but it feels like " + json.getJSONObject("main").get("feels_like") + "°F.");
        System.out.println("    The Humidity is " + json.getJSONObject("main").get("humidity") + "%.");
        System.out.println("    Currently, the wind is " + json.getJSONObject("wind").get("speed") + " MPH.");
        System.out.println("    The Cloudiness is " + json.getJSONObject("clouds").get("all") + "%.");
    }

}
