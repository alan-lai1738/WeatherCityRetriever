
# Weather/City API Program Description, Design, Usage
## Description
Weather/Nearby City Finder is Java program that takes in a name of a city and outputs the weather and nearby cities of that city. Given a city, the program outputs weather description, temperature, humidity, wind speed, cloudiness of the city, and a list of multiple cities near that city. This program uses OpenWeatherMap Weather API and GeoBytes GetNearbyCities API for its information. This program deserializes JSON with JSONArray, JSONObject, and GSON. In cases of failure, this program uses exponential back-off to reattempt connection. The GeoBytes API is dependent on longitude/latitude from the Weather API so it can only run IF the Weather API is up. In the other case, the weather API can run even if the GeoBytes NearbyCities API is down.
## Program Design
![image](https://user-images.githubusercontent.com/70036749/139504728-5c630ba3-9e56-432a-9992-06f0116b740b.png)
This program has multiple methods that run in one single class. It will be based on static methods that return data as needed. First the user input is taken from console when run. Next, the city name from user input is used to get JSON data from Weather API, then that data will be used to print output and it’s longitude/latitude is for getting nearby cities from API #2. 
##API Retry Logic
![image](https://user-images.githubusercontent.com/70036749/139504740-84d09fa7-6745-49f5-8994-442c6220b5f4.png)
The Retry logic is based on response code from the APIs. Our retry case is on 429/499-599 error codes. Every reattempt will happen after stoppageTime amount of rest. The program will retry up to five times with the stoppageTime doubling every time. After 5 failed attempts, it will stop and output error to console.

## Usage
Unzip com to have required libraries.
Once that's done:
To compile type: javac WeatherCityRetriever.java
To run type: java WeatherCityRetriever <city name>
