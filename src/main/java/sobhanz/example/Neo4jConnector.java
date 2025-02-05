package sobhanz.example;

import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Driver;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import java.util.ArrayList;
import java.util.List;
import static org.neo4j.driver.Values.parameters;
import org.json.JSONObject;

public class Neo4jConnector implements AutoCloseable {
    private final Driver driver;

    public Neo4jConnector(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    @Override
    public void close() {
        driver.close();
    }

    // method for reading queries
    public List<Record> runReadQuery(String query, Value parameters) {
        List<Record> results = new ArrayList<>();
        try (Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run(query, parameters);
                while (result.hasNext()) {
                    results.add(result.next());
                }
                return null;
            });
        }
        return results;
    }
}