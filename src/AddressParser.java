import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.io.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import Models.AddressGeoCode;
import Models.GoogleGeoCode;
import Models.Location;

public class AddressParser {

  public static void main(String[] args) throws Exception {
    try {
      File file = new File("C:\\Users\\Zhan\\Projects\\concurrency-with-retries\\list.txt");
      BufferedReader br = new BufferedReader(new FileReader(file));
      ArrayList<String> listOfAddresses = new ArrayList<String>();
      String st;

      while ((st = br.readLine()) != null) {
        listOfAddresses.add(st);
      }

      int threadCount = listOfAddresses.size();
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      ArrayList<Future<AddressGeoCode>> futureList = new ArrayList<Future<AddressGeoCode>>();
      ArrayList<AddressGeoCode> returnList = new ArrayList<AddressGeoCode>();
      
      for (String s: listOfAddresses) {
        Callable worker = new Caller(s);
        Future<AddressGeoCode> future = executor.submit(worker);
        futureList.add(future);
      }
      for(Future<AddressGeoCode> fut : futureList){
    	  try {
    		  returnList.add(fut.get());
    	  } catch (Exception e) {
              e.printStackTrace();
    	  }
      }
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      String arrayToJson = objectMapper.writeValueAsString(returnList);
      System.out.println(arrayToJson);
      executor.shutdown();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }
  }

  public static class Caller implements Callable<AddressGeoCode> {
    private final String address;

    Caller(String address) {
      this.address = address;
    }

    @Override
    public AddressGeoCode call() throws Exception {
      int count = 0;
      final int maxRetries = 5;
      while (count < maxRetries) {
        try {
          return geocodeAddress(this.address);
        } catch (Exception e) {
          System.out.println("Retrying..");
          if ( ++count >= maxRetries) {
        	  AddressGeoCode addressGeocode = new AddressGeoCode(this.address, "NOT_FOUND", new Location("N/A", "N/A"));
        	  return addressGeocode;
          }
        }
      }
      throw new Exception("Error");
    }

    public static AddressGeoCode geocodeAddress(String s) throws Exception {
      String url = "https://maps.googleapis.com/maps/api/geocode/json";
      StringBuilder urlParams = new StringBuilder("?address=");
      urlParams.append(URLEncoder.encode(s,"UTF-8"));
      urlParams.append("&key=");
      urlParams.append(URLEncoder.encode("Enter API KEY","UTF-8"));
      URL siteURL = new URL(url + urlParams.toString());
			HttpURLConnection con = (HttpURLConnection) siteURL.openConnection();

	  con.setRequestMethod("POST");
      con.setRequestProperty("Accept-Language", "UTF-8");
      con.setDoOutput(true);
	  DataOutputStream outputStreamWriter = new DataOutputStream(con.getOutputStream());
      outputStreamWriter.flush();

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while((inputLine = in.readLine()) != null) {
        response.append(inputLine);
        response.append("\n");
      }
      in.close();

      ObjectMapper mapper = new ObjectMapper();
      GoogleGeoCode geocode = mapper.readValue(response.toString(), GoogleGeoCode.class);
      AddressGeoCode addressGeocode;
      if (geocode.getStatus().equals("OVER_DAILY_LIMIT")) {
    	  throw new Exception("OVER DAILY LIMIT");
      } else if (geocode.getStatus().equals("ZERO_RESULTS")) {
    	  addressGeocode = new AddressGeoCode(s, "NOT_FOUND", new Location("N/A", "N/A"));
      } else {
    	  String lat = geocode.getResults()[0].getGeometry().getLocation().getLat();
    	  String lng = geocode.getResults()[0].getGeometry().getLocation().getLng();
          addressGeocode = new AddressGeoCode(s, geocode.getStatus(), new Location(lat, lng));
      }
      return addressGeocode;
    }
  }

}

