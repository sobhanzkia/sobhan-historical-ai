package sobhanz.example;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import static org.neo4j.driver.Values.parameters;
import org.json.JSONObject;
import org.neo4j.driver.types.Node;
import java.util.List;

public class QueryHandler {
    private final Neo4jConnector neo4j;
    private final OllamaClient ollama;

    public QueryHandler(Neo4jConnector neo4j, OllamaClient ollama) {
        this.neo4j = neo4j;
        this.ollama = ollama;
    }

    // to process user input
    public void processUserQuery(String input) {
        // examples of input for ollama
        String extractionPrompt = """
                    Analyze the following historical query and extract the relevant entities.
                    Return ONLY a valid JSON object with NO extra text, explanations, or code.

                    If the query is about a person, return:
                    {
                      "intent": "query_person",
                      "entities": {"person": "Plato"}
                    }

                    If the query is about a city, return:
                    {
                      "intent": "query_city",
                      "entities": {"city": "Athens"}
                    }

                    If the query is about a country, return:
                    {
                      "intent": "query_country",
                      "entities": {"country": "Greece"}
                    }

                    If the query is about a continent, return:
                    {
                      "intent": "query_continent",
                      "entities": {"continent": "Europe"}
                    }

                    If the query is about an occupation, return:
                    {
                      "intent": "query_occupation",
                      "entities": {"occupation": "Philosopher"}
                    }

                    If the query is about an industry, return:
                    {
                      "intent": "query_industry",
                      "entities": {"industry": "Philosophy"}
                    }

                    If the query is about a century, return:
                    {
                      "intent": "query_century",
                      "entities": {"century": "5th"}
                    }

                    If the query asks about people who lived at the same time as another person, return:
                    {
                      "intent": "query_people_same_time",
                      "entities": {"person": "Plato"}
                    }

                    If the query asks about people who had the same occupation as another person, return:
                    {
                      "intent": "query_people_same_occupation",
                      "entities": {"person": "Aristotle"}
                    }

                    If the query asks about people who lived in the same city and century as another person, return:
                    {
                      "intent": "query_people_same_time_in_city",
                      "entities": {"person": "Aristotle", "city": "Stageira"}
                    }

                    If the query asks about occupations in a city during a specific century, return:
                    {
                      "intent": "query_occupations_by_city_and_century",
                      "entities": {"city": "Rome", "century": "5th"}
                    }

                    If the query asks about occupations in an industry during a specific century, return:
                    {
                      "intent": "query_occupations_in_industry_by_century",
                      "entities": {"industry": "Military", "century": "5th"}
                    }

                    User input: "%s"
                """.formatted(input);

        // for ollama raw response
        String response = ollama.generate("llama3.2", extractionPrompt);
        // System.out.println("\n[Ollama Raw Response]: " + response);

        // cleaning JSON
        response = cleanJsonString(response);

        if (!response.trim().startsWith("{")) {
            System.out.println("[Error] Ollama returned non-JSON output. Ignoring response.");
            return;
        }

        try {
            JSONObject extractedData = new JSONObject(response);
            // System.out.println("\n[Extracted JSON Data]: " + extractedData.toString(2));

            if (extractedData.isNull("intent")) {
                System.out.println("[Error] No intent detected in response.");
                return;
            }

            // for choosing the topic of query
            switch (extractedData.getString("intent")) {
                case "query_person":
                    String personName = getSafeString(extractedData.getJSONObject("entities"), "person");
                    getPersonInfo(personName);
                    break;
                case "query_city":
                    String city = getSafeString(extractedData.getJSONObject("entities"), "city");
                    getCityInfo(city);
                    break;
                case "query_country":
                    String country = getSafeString(extractedData.getJSONObject("entities"), "country");
                    getCountryInfo(country);
                    break;
                case "query_continent":
                    String continent = getSafeString(extractedData.getJSONObject("entities"), "continent");
                    getContinentInfo(continent);
                    break;
                case "query_occupation":
                    String occupation = getSafeString(extractedData.getJSONObject("entities"), "occupation");
                    getOccupationInfo(occupation);
                    break;
                case "query_industry":
                    String industry = getSafeString(extractedData.getJSONObject("entities"), "industry");
                    getIndustryInfo(industry);
                    break;
                case "query_century":
                    String century = getSafeString(extractedData.getJSONObject("entities"), "century");
                    getPeopleByCentury(century);
                    break;
                case "query_people_same_time":
                    String personSameTime = getSafeString(extractedData.getJSONObject("entities"), "person");
                    getPeopleWhoLivedAtTheSameTime(personSameTime);
                    break;
                case "query_people_same_occupation":
                    String personSameOccupation = getSafeString(extractedData.getJSONObject("entities"), "person");
                    getPeopleWithSameOccupation(personSameOccupation);
                    break;
                case "query_people_same_time_in_city":
                    String personSameTimeInCity = getSafeString(extractedData.getJSONObject("entities"), "person");
                    String cityForSameTime = getSafeString(extractedData.getJSONObject("entities"), "city");
                    getPeopleWhoLivedInCityAtTheSameTime(personSameTimeInCity, cityForSameTime);
                    break;
                case "query_occupations_by_city_and_century":
                    String cityForOccupations = getSafeString(extractedData.getJSONObject("entities"), "city");
                    String centuryForOccupations = getSafeString(extractedData.getJSONObject("entities"), "century");
                    getOccupationsByCityAndCentury(cityForOccupations, centuryForOccupations);
                    break;
                case "query_occupations_in_industry_by_century":
                    String industryForOccupations = getSafeString(extractedData.getJSONObject("entities"), "industry");
                    String centuryForIndustry = getSafeString(extractedData.getJSONObject("entities"), "century");
                    getOccupationsByIndustryAndCentury(industryForOccupations, centuryForIndustry);
                    break;
                default:
                    System.out.println("[Error] Sorry I didn't understand your request.");
            }

        } catch (Exception e) {
            System.out.println("[Error] JSON Parsing failed: " + e.getMessage());
            System.out.println("[Debug] Ollama Response: " + response);
        }
    }

    // sometimes JSON is not beautiful
    private String cleanJsonString(String json) {
        json = json.replaceAll("\\s+\"", "\"");
        json = json.replaceAll("\"\\s+:", "\":");
        json = json.replaceAll(":\\s+", ":");
        return json;
    }

    // JSONObject
    private String getSafeString(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return "Unknown";
        }
        Object value = json.get(key);
        if (value instanceof Number) {
            return String.format("%.2f", ((Number) value).doubleValue());
        }
        return value.toString();
    }

    private void printIfExists(String label, String value) {
        if (!value.equals("Unknown")) {
            System.out.println(label + ": " + value);
        }
    }

    // via the BORN_IN relationship.
    public void getPeopleByCentury(String century) {
        String query = "MATCH (p:Person)-[:BORN_IN]->(centuryNode:Century {century: $century}) " +
                "RETURN p.full_name AS person, p.occupation AS occupation, p.city AS city, p.birth_year AS birthYear";
        List<Record> results = neo4j.runReadQuery(query, parameters("century", century));

        if (results.isEmpty()) {
            System.out.println("\nNo historical data found for century: " + century);
        } else {
            System.out.println("\nPeople born in Century " + century + ":");
            for (Record record : results) {
                // Printing a complete sentence
                System.out.println(" - " + record.get("person").asString() +
                        " (Occupation: " + record.get("occupation").asString("Unknown") +
                        ", City: " + record.get("city").asString("Unknown") +
                        ", Birth Year: " + record.get("birthYear").asString("Unknown") + ")");
            }
        }
    }

    // for whole info of a person
    public void getPersonInfo(String name) {
        String query = "MATCH (p:Person {full_name: $name}) " +
                "OPTIONAL MATCH (p)-[:BORN_IN_CITY]->(c:City) " +
                "OPTIONAL MATCH (p)-[:BORN_IN]->(cent:Century) " +
                "OPTIONAL MATCH (p)-[:WORKS_AS]->(o:Occupation) " +
                "RETURN p, c.name AS city, cent.century AS century, o.name AS occupation";
        List<Record> results = neo4j.runReadQuery(query, parameters("name", name));

        if (results.isEmpty()) {
            System.out.println("\n[Error] No person found with name: " + name);
        } else {
            System.out.println("\n======= Person Information =======");
            for (Record record : results) {
                Node personNode = record.get("p").asNode();
                System.out.println("Full Name: " + getSafeString(personNode, "full_name"));
                System.out.println("Gender: " + getSafeString(personNode, "sex"));
                System.out.println("Birth Year: " + getSafeString(personNode, "birth_year"));
                System.out.println("City: " + record.get("city").asString("Unknown"));
                System.out.println("Century: " + record.get("century").asString("Unknown"));
                System.out.println("Occupation: " + record.get("occupation").asString("Unknown"));
                System.out.println("Country: " + getSafeString(personNode, "country"));
                System.out.println("Continent: " + getSafeString(personNode, "continent"));
                System.out.println("Popularity Index: " + getSafeString(personNode, "historical_popularity_index"));
                System.out.println("Average Views: " + getSafeString(personNode, "average_views"));
                System.out.println("Total Page Views: " + getSafeString(personNode, "page_views"));
                String latitude = getSafeString(personNode, "latitude");
                String longitude = getSafeString(personNode, "longitude");
                if (!latitude.equals("Unknown") && !longitude.equals("Unknown")) {
                    System.out.println("Location (Lat, Long): " + latitude + ", " + longitude);
                }
                System.out.println("---------------------------------");
            }
        }
    }

    // city informaion
    public void getCityInfo(String city) {
        String query = "MATCH (c:City {name: $city}) " +
                "OPTIONAL MATCH (c)<-[:BORN_IN_CITY]-(p:Person) " +
                "OPTIONAL MATCH (p)-[:WORKS_AS]->(o:Occupation) " +
                "OPTIONAL MATCH (c)-[:LOCATED_IN]->(co:Country) " +
                "OPTIONAL MATCH (co)-[:LOCATED_IN]->(con:Continent) " +
                "RETURN c.name AS city, co.name AS country, con.name AS continent, " +
                "collect({person: p.full_name, century: p.birth_year, occupation: o.name}) AS residents";
        List<Record> results = neo4j.runReadQuery(query, parameters("city", city));
        if (results.isEmpty()) {
            System.out.println("\nNo historical data found for city: " + city);
        } else {
            Record record = results.get(0);
            System.out.println("\nCity: " + record.get("city").asString());
            System.out.println("Country: " + record.get("country").asString("Unknown"));
            System.out.println("Continent: " + record.get("continent").asString("Unknown"));
            System.out.println("Residents:");
            for (Object obj : record.get("residents").asList()) {
                JSONObject resident = new JSONObject((java.util.Map<?, ?>) obj);
                System.out.println(" - " + resident.optString("person", "Unknown") +
                        " (Birth Year: " + resident.optString("century", "Unknown") +
                        ", Occupation: " + resident.optString("occupation", "Unknown") + ")");
            }
        }
    }

    // country information
    public void getCountryInfo(String country) {
        String query = "MATCH (co:Country {name: $country}) " +
                "OPTIONAL MATCH (co)<-[:LOCATED_IN]-(c:City) " +
                "OPTIONAL MATCH (c)<-[:BORN_IN_CITY]-(p:Person) " +
                "RETURN co.name AS country, c.name AS city, collect(DISTINCT p.full_name) AS persons";
        List<Record> results = neo4j.runReadQuery(query, parameters("country", country));

        if (results.isEmpty()) {
            System.out.println("\nNo historical data found for country: " + country);
        } else {
            String countryName = results.get(0).get("country").asString();
            System.out.println("\nCountry: " + countryName);

            for (Record record : results) {
                if (record.get("city").isNull()) {
                    continue;
                }
                String cityName = record.get("city").asString();
                List<Object> personsList = record.get("persons").asList();

                StringBuilder personsStr = new StringBuilder();
                for (Object person : personsList) {
                    if (personsStr.length() > 0) {
                        personsStr.append(", ");
                    }
                    personsStr.append(person.toString());
                }

                if (personsStr.length() == 0) {
                    personsStr.append("No notable persons");
                }

                System.out.println("City: " + cityName + " (Notable Persons: " + personsStr.toString() + ")");
            }
        }
    }

    // continent information
    public void getContinentInfo(String continent) {
        String query = "MATCH (con:Continent {name: $continent}) " +
                "OPTIONAL MATCH (con)<-[:LOCATED_IN]-(co:Country) " +
                "RETURN con.name AS continent, collect(DISTINCT co.name) AS countries " +
                "ORDER BY co.name";

        List<Record> results = neo4j.runReadQuery(query, parameters("continent", continent));

        if (results.isEmpty()) {
            System.out.println("\nNo historical data found for continent: " + continent);
        } else {

            Record record = results.get(0);
            String contName = record.get("continent").asString("Unknown");
            System.out.println("\nContinent: " + contName);

            List<Object> countriesList = record.get("countries").asList();

            StringBuilder countriesStr = new StringBuilder();
            for (Object country : countriesList) {
                if (countriesStr.length() > 0) {
                    countriesStr.append(", ");
                }
                countriesStr.append(country.toString());
            }

            if (countriesStr.length() == 0) {
                countriesStr.append("No countries");
            }

            System.out.println("Countries: " + countriesStr.toString());
        }
    }

    // occupation
    public void getOccupationInfo(String occupation) {
        String query = "MATCH (p:Person)-[:WORKS_AS]->(o:Occupation {name: $occupation}) " +
                "OPTIONAL MATCH (p)-[:IN_INDUSTRY]->(i:Industry) " +
                "OPTIONAL MATCH (p)-[:BORN_IN]->(century:Century) " +
                "RETURN o.name AS occupation, head(collect(DISTINCT i.name)) AS industry, " +
                "collect({person: p.full_name, century: century.century, city: p.city}) AS details";
        List<Record> results = neo4j.runReadQuery(query, parameters("occupation", occupation));
        if (results.isEmpty()) {
            System.out.println("\nNo historical figures found with occupation: " + occupation);
        } else {
            Record record = results.get(0);
            System.out.println("\nOccupation: " + record.get("occupation").asString());
            System.out.println("Industry: " + record.get("industry").asString("Unknown"));
            System.out.println("Figures:");
            for (Object obj : record.get("details").asList()) {
                JSONObject detail = new JSONObject((java.util.Map<?, ?>) obj);
                System.out.println(" - " + detail.optString("person", "Unknown") +
                        " (Century: " + detail.optString("century", "Unknown") +
                        ", City: " + detail.optString("city", "Unknown") + ")");
            }
        }
    }

    // industry
    public void getIndustryInfo(String industry) {
        String query = "MATCH (p:Person)-[:IN_INDUSTRY]->(i:Industry {name: $industry}) " +
                "OPTIONAL MATCH (p)-[:WORKS_AS]->(o:Occupation) " +
                "OPTIONAL MATCH (p)-[:BORN_IN]->(century:Century) " +
                "RETURN i.name AS industry, collect({person: p.full_name, century: century.century, occupation: o.name}) AS details";
        List<Record> results = neo4j.runReadQuery(query, parameters("industry", industry));
        if (results.isEmpty()) {
            System.out.println("\nNo historical figures found in industry: " + industry);
        } else {
            Record record = results.get(0);
            System.out.println("\nIndustry: " + record.get("industry").asString());
            System.out.println("Figures:");
            for (Object obj : record.get("details").asList()) {
                JSONObject detail = new JSONObject((java.util.Map<?, ?>) obj);
                System.out.println(" - " + detail.optString("person", "Unknown") +
                        " (Century: " + detail.optString("century", "Unknown") +
                        ", Occupation: " + detail.optString("occupation", "Unknown") + ")");
            }
        }
    }

    public void getPeopleWhoLivedAtTheSameTime(String personName) {
        String query = "MATCH (p1:Person {full_name: $name})-[:BORN_IN]->(century:Century) " +
                "MATCH (p2:Person)-[:BORN_IN]->(century) " +
                "WHERE p1.full_name <> p2.full_name " +
                "RETURN DISTINCT p2.full_name AS person";
        List<Record> results = neo4j.runReadQuery(query, parameters("name", personName));
        if (results.isEmpty()) {
            System.out.println("\nNo one lived at the same time as " + personName);
        } else {
            System.out.println("\nPeople who lived at the same time as " + personName + ":");
            for (Record record : results) {
                System.out.println(" - " + record.get("person").asString());
            }
        }
    }

    public void getPeopleWithSameOccupation(String personName) {
        String query = "MATCH (p1:Person {full_name: $name})-[:WORKS_AS]->(o:Occupation) " +
                "MATCH (p2:Person)-[:WORKS_AS]->(o) " +
                "WHERE p1.full_name <> p2.full_name " +
                "RETURN DISTINCT p2.full_name AS person";
        List<Record> results = neo4j.runReadQuery(query, parameters("name", personName));
        if (results.isEmpty()) {
            System.out.println("\nNo one had the same occupation as " + personName);
        } else {
            System.out.println("\nPeople with the same occupation as " + personName + ":");
            for (Record record : results) {
                System.out.println(" - " + record.get("person").asString());
            }
        }
    }

    public void getPeopleWhoLivedInCityAtTheSameTime(String personName, String city) {
        String query = "MATCH (p1:Person {full_name: $name})-[:BORN_IN_CITY]->(c:City {name: $city}) " +
                "MATCH (p1)-[:BORN_IN]->(century:Century) " +
                "MATCH (p2:Person)-[:BORN_IN_CITY]->(c) " +
                "MATCH (p2)-[:BORN_IN]->(century) " +
                "WHERE p1.full_name <> p2.full_name " +
                "RETURN DISTINCT p2.full_name AS person";
        List<Record> results = neo4j.runReadQuery(query, parameters("name", personName, "city", city));
        if (results.isEmpty()) {
            System.out.println("\nNo one lived at the same time as " + personName + " in " + city);
        } else {
            System.out.println("\nPeople who lived at the same time as " + personName + " in " + city + ":");
            for (Record record : results) {
                System.out.println(" - " + record.get("person").asString());
            }
        }
    }

    public void getOccupationsByCityAndCentury(String city, String century) {
        String query = "MATCH (p:Person)-[:BORN_IN_CITY]->(c:City {name: $city}) " +
                "MATCH (p)-[:BORN_IN]->(centuryNode:Century {century: $century}) " +
                "MATCH (p)-[:WORKS_AS]->(o:Occupation) " +
                "RETURN DISTINCT o.name AS occupation";

        List<Record> results = neo4j.runReadQuery(query, parameters("city", city, "century", century));

        if (results.isEmpty()) {
            System.out.println("\nNo occupations found for city: " + city + " in century: " + century);
        } else {
            System.out.println("\nOccupations in " + city + " during " + century + " century:");
            for (Record record : results) {
                System.out.println(" - " + record.get("occupation").asString());
            }
        }
    }

    public void getOccupationsByIndustryAndCentury(String industry, String century) {
        String query = "MATCH (p:Person)-[:IN_INDUSTRY]->(i:Industry {name: $industry}) " +
                "MATCH (p)-[:BORN_IN]->(centuryNode:Century {century: $century}) " +
                "MATCH (p)-[:WORKS_AS]->(o:Occupation) " +
                "RETURN DISTINCT o.name AS occupation";
        List<Record> results = neo4j.runReadQuery(query, parameters("industry", industry, "century", century));
        if (results.isEmpty()) {
            System.out.println(
                    "\nNo occupations found in the " + industry + " industry during the " + century + " century.");
        } else {
            System.out.println("\nOccupations in the " + industry + " industry during the " + century + " century:");
            for (Record record : results) {
                System.out.println(" - " + record.get("occupation").asString());
            }
        }
    }

    private String getSafeString(Node node, String key) {
        if (!node.containsKey(key))
            return "Unknown";
        Value value = node.get(key);
        if (value.isNull())
            return "Unknown";
        if (value.type().name().equals("INTEGER") || value.type().name().equals("FLOAT")) {
            return String.valueOf(value.asNumber());
        }
        String text = value.asString().trim();
        return text.isEmpty() ? "Unknown" : text;
    }
}
