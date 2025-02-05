package sobhanz.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class OllamaClient {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    public OllamaClient() {
        // Constructor بدون ورودی
    }

    public String generate(String model, String prompt) {
        try {
            URL url = new URL(OLLAMA_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject requestData = new JSONObject();
            requestData.put("model", model);
            requestData.put("prompt", prompt);
            requestData.put("stream", false);
            conn.setConnectTimeout(60000); // ۶۰ ثانیه برای timeout اتصال
            conn.setReadTimeout(60000); // ۶۰ ثانیه برای timeout خواندن پاسخ

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // ✅ حذف اطلاعات اضافی و فقط نگه داشتن JSON
            String jsonResponse = response.toString().trim();
            if (!jsonResponse.startsWith("{")) {
                throw new Exception("Invalid JSON response from Ollama: " + jsonResponse);
            }

            JSONObject json = new JSONObject(jsonResponse);
            return json.getString("response");

        } catch (Exception e) {
            e.printStackTrace();
            return "error with conecting to ollama: " + e.getMessage();
        }
    }

}