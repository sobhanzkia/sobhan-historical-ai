package sobhanz.example;

import org.json.JSONObject;

public class NLPProcessor {
    public static void main(String[] args) {
        String userPrompt = "In which century did Plato live?";

        String extractionPrompt = """
                Analyze the following text and extract historical entities (names of people, cities, countries, centuries, major events, occupations).
                Return the information in JSON format. If there is not enough information in the text, return `null`.

                ### Example Output:
                {"intent": "query_person", "entities": {"person": "Plato"}, "filters": {"attribute": "century"}}

                Text: "%s"
                """
                .formatted(userPrompt);

        OllamaClient ollama = new OllamaClient(); // No need to pass a String
        String response = ollama.generate("llama3.2", extractionPrompt);
        System.out.println("Response from Ollama:");
        System.out.println(response);
    }
}
