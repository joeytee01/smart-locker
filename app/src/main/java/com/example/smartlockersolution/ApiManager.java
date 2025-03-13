package com.example.smartlockersolution;

import android.graphics.Bitmap;
import android.util.Base64;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ApiManager {
    private static ApiManager instance;
    private final OkHttpClient client;
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private ApiManager() {
        client = new OkHttpClient();
    }

    // Singleton instance
    public static synchronized ApiManager getInstance() {
        if (instance == null) {
            instance = new ApiManager();
        }
        return instance;
    }
    public static void setInstance(ApiManager newInstance) {
        instance = newInstance;
    }

    // Callback for endpoints returning a JSON array.
    public interface ApiCallback {
        void onSuccess(JSONArray data);
        void onFailure(String errorMessage);
    }

    // Callback for endpoints returning a JSON object.
    public interface ApiObjectCallback {
        void onSuccess(JSONObject data);
        void onFailure(String errorMessage);
    }

    // Overloaded method: without a custom baseUrl, defaults to BASE_URL.
    private void makeGetRequest(String endpoint, String authToken, ApiCallback callback) {
        makeGetRequest(endpoint, authToken, callback, BASE_URL);
    }

    // Overloaded method: with a custom baseUrl parameter.
    private void makeGetRequest(String endpoint, String authToken, ApiCallback callback, String baseUrl) {
        // Use BASE_URL if the provided baseUrl is null or empty.
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = BASE_URL;
        }

        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .addHeader("Authorization", authToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        String jsonData = responseBody.string();
                        JSONObject json = new JSONObject(jsonData);
                        JSONArray data = json.getJSONArray("data");
                        callback.onSuccess(data);
                    } catch (Exception e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Request failed with code: " + response.code());
                }
            }
        });
    }

    // Generic POST request method.
    private void makePostRequest(String endpoint, String authToken, RequestBody requestBody, ApiObjectCallback callback) {
        Request.Builder builder = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody);

        if (authToken != null && !authToken.isEmpty()) {
            builder.addHeader("Authorization", authToken);
        }
        Request request = builder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        String jsonData = responseBody.string();
                        JSONObject json = new JSONObject(jsonData);
                        callback.onSuccess(json);
                    } catch (Exception e) {
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Request failed with code: " + response.code());
                }
            }
        });
    }

    // Generic PUT request method.
    private void makePutRequest(String endpoint, String authToken, RequestBody requestBody, ApiObjectCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(requestBody)
                .addHeader("Authorization", authToken)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    try (ResponseBody responseBody = response.body()){
                        String jsonData = responseBody.string();
                        JSONObject json = new JSONObject(jsonData);
                        callback.onSuccess(json);
                    } catch(Exception e){
                        callback.onFailure("Error parsing response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Request failed with code: " + response.code());
                }
            }
        });
    }


    // Fetch locker availability.
    public void fetchLockerAvailability(String authToken, ApiCallback callback) {
        makeGetRequest("lockers?location_id=1&status=available", authToken, callback);
    }

    // Fetch user profile.
    public void fetchUserProfile(String authToken, ApiCallback callback) {
        makeGetRequest("user/profile", authToken, callback);
    }

    // Fetch case list.
    public void fetchCaseList(String authToken, ApiCallback callback) {
        makeGetRequest("cases", authToken, callback);
    }

    // Fetch TEP codes for a given transaction.
    public void fetchTepCodes(String authToken, String transactionId, ApiCallback callback) {
        makeGetRequest("tep-codes?transaction_id=" + transactionId, authToken, callback);
    }

    public void fetchTepCodes(String authToken, String transactionId, String base_url, ApiCallback callback) {
        makeGetRequest("tep-codes?transaction_id=" + transactionId, authToken, callback, base_url);
    }

    // Call journey API.
    public void callJourney(String authToken, RequestBody requestBody, ApiObjectCallback callback) {
        makePostRequest("journeys", authToken, requestBody, callback);
    }

    // Update transaction with signature and remarks.
    public void updateTransaction(String authToken, String transactionId, RequestBody body, ApiObjectCallback callback) {
        makePutRequest("transactions/" + transactionId, authToken, body, callback);
    }

    // Open a locker (update its status).
    public void openLocker(String authToken, int lockerId, RequestBody body, ApiObjectCallback callback) {
        makePutRequest("lockers/" + lockerId, authToken, body, callback);
    }

    // Fetch emails based on role, returning a JSON object.
    public void fetchEmails(String authToken, String role, ApiObjectCallback callback) {
        String endpoint = "emails?role=" + role;
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Failed with code: " + response.code());
                    return;
                }
                try (ResponseBody responseBody = response.body()) {
                    String jsonData = responseBody.string();
                    JSONObject json;
                    JSONArray emailsArray;
                    try {
                        json = new JSONObject(jsonData);
                        emailsArray = json.optJSONArray("emails");
                        if (emailsArray == null) {
                            emailsArray = new JSONArray();
                        }
                    } catch (JSONException e) {
                        emailsArray = new JSONArray(jsonData);
                        json = new JSONObject();
                    }

                    json.put("emails", emailsArray);
                    callback.onSuccess(json);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }


    // Fetch all lockers.
    public void fetchAllLockers(String authToken, ApiObjectCallback callback) {
        String endpoint = "lockers/all";
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .addHeader("Authorization", authToken)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Failed with code: " + response.code());
                    return;
                }
                try (ResponseBody responseBody = response.body()) {
                    String jsonData = responseBody.string();
                    JSONObject json = new JSONObject(jsonData);
                    callback.onSuccess(json);
                } catch (Exception e) {
                    callback.onFailure("Error parsing response: " + e.getMessage());
                }
            }
        });
    }

    // Login method
    public void performLogin(String role, String action, String divisionPassNo, String password, ApiObjectCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("role", role);
            json.put("action", action);
            json.put("division_pass_no", divisionPassNo);
            json.put("password", password);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            makePostRequest("login", null, body, callback);
        } catch (JSONException e) {
            callback.onFailure("Error creating login request: " + e.getMessage());
        }
    }

    // Enrollment method
    public void performEnrollment(String authToken, String qrCode, String divisionPassNo, String email, ApiObjectCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("qr_code", qrCode);
            json.put("division_pass_no", divisionPassNo);
            json.put("email", email);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            makePostRequest("users/enrolls", authToken, body, callback);
        } catch (JSONException e) {
            callback.onFailure("Error creating enrollment request: " + e.getMessage());
        }
    }

    public void callTepImages(String authToken, String transactionId, String firebaseId, List<PhoneTepBagsActivity.TepBag> tepBags, String Base_Url, ApiObjectCallback callback) {
        try {

            JSONObject payload = new JSONObject();
            payload.put("transaction_id", transactionId);
            payload.put("firebase_id", firebaseId);

            JSONArray dataArray = new JSONArray();
            for (PhoneTepBagsActivity.TepBag bag : tepBags) {
                JSONObject bagJson = new JSONObject();
                bagJson.put("tep_id", bag.number);
                String frontBase64 = (bag.frontPhoto != null) ? bitmapToBase64(bag.frontPhoto) : "";
                String backBase64 = (bag.backPhoto != null) ? bitmapToBase64(bag.backPhoto) : "";
                bagJson.put("front", frontBase64);
                bagJson.put("back", backBase64);
                dataArray.put(bagJson);
            }
            payload.put("data", dataArray);

            // Create the RequestBody
            RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.parse("application/json"));

            Request.Builder builder = new Request.Builder()
                    .url(Base_Url + "teps-images")
                    .post(requestBody);

            // use "X_API_TOKEN" as the header prefix here
            builder.addHeader("Authorization", "X_API_TOKEN " + authToken);
            Request request = builder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("teps-images request failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try (ResponseBody responseBody = response.body()) {
                            String responseData = responseBody.string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            callback.onSuccess(jsonResponse);
                        } catch (Exception e) {
                            callback.onFailure("teps-images response parsing error: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("teps-images request failed with code: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure("Error constructing JSON payload: " + e.getMessage());
        }
    }

    // Helper method to convert a Bitmap to a Base64 String
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }


}
