package sobhanz.example;

import java.util.Scanner;

public class HistoricalAIAgent {
    public static void main(String[] args) {
        try (Neo4jConnector neo4j = new Neo4jConnector("bolt://localhost:7687", "neo4j", "new_password")) {
            OllamaClient ollama = new OllamaClient();
            QueryHandler queryHandler = new QueryHandler(neo4j, ollama);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\n=== Historical AI Agent ===");
                System.out.println("Type your historical query or 'exit' to quit:");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...");
                    break;
                }

                queryHandler.processUserQuery(input);
            }
        }
    }
}
