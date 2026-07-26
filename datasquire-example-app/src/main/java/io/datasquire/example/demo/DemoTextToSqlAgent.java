package io.datasquire.example.demo;

import io.datasquire.core.agent.AgentCapability;
import io.datasquire.core.agent.SubAgent;
import io.datasquire.core.agent.SubAgentRequest;
import io.datasquire.core.agent.SubAgentResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Demo agent returning pre-recorded responses. Activated with spring.profiles.active=demo. */
@Component
@Profile("demo")
public class DemoTextToSqlAgent implements SubAgent {

    private static final String NAME = "demo-text-to-sql";
    private static final String OUT_OF_SCOPE =
            "I can only answer questions about the DVD rental database. " +
            "This includes films, actors, customers, rentals, payments, and store information. " +
            "Please rephrase your question to relate to the available data.";

    private final List<DemoEntry> entries = buildEntries();

    @Override public String getName() { return NAME; }

    @Override
    public String getDescription() {
        return "Demo agent with pre-recorded SQL responses for the DVD rental database";
    }

    @Override
    public Set<AgentCapability> getCapabilities() {
        return Set.of(
            new AgentCapability("aggregation", "COUNT/SUM/AVG/GROUP BY", Set.of("count", "sum", "average", "total", "group")),
            new AgentCapability("filtering", "Filter and search data", Set.of("filter", "find", "search", "where")),
            new AgentCapability("joins", "Multi-table queries", Set.of("join", "across", "relationship")));
    }

    @Override
    public Set<String> getKeywords() {
        return Set.of("film", "actor", "customer", "rental", "payment", "category",
                "store", "staff", "revenue", "duration", "count", "top");
    }

    @Override
    public List<String> getExampleQueries() {
        return List.of("How many films are in the database?",
                "Show me the top 5 highest-grossing films",
                "Which actors have appeared in the most films?");
    }

    @Override
    public CompletableFuture<SubAgentResponse> process(SubAgentRequest request) {
        return CompletableFuture.completedFuture(findResponse(request.query()));
    }

    SubAgentResponse findResponse(String query) {
        String normalized = query.toLowerCase().trim();
        int bestScore = 0;
        DemoEntry bestEntry = null;
        for (DemoEntry entry : entries) {
            int score = entry.score(normalized);
            if (score > bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }
        if (bestEntry == null || bestScore < 2) {
            return SubAgentResponse.failure(NAME, OUT_OF_SCOPE);
        }
        return SubAgentResponse.success(NAME, bestEntry.response(), Set.of("sql"));
    }

    private static List<DemoEntry> buildEntries() {
        return List.of(
            entry(Set.of("how many", "count", "films", "database", "total films"),
                """
                **Query:** `SELECT COUNT(*) AS film_count FROM film;`
                | film_count |
                |-----------|
                | 1000 |

                There are **1,000 films** in the DVD rental database."""),

            entry(Set.of("top", "highest", "grossing", "revenue", "films", "earning"),
                """
                **Query:** `SELECT f.title, SUM(p.amount) AS total_revenue FROM film f JOIN inventory i ON f.film_id = i.film_id JOIN rental r ON i.inventory_id = r.inventory_id JOIN payment p ON r.rental_id = p.rental_id GROUP BY f.title ORDER BY total_revenue DESC LIMIT 5;`
                | title | total_revenue |
                |-------|--------------|
                | Telegraph Voyage | $231.73 |
                | Wife Turn | $223.69 |
                | Zorro Ark | $214.69 |
                | Goodfellas Salute | $209.69 |
                | Saturday Lambs | $204.72 |"""),

            entry(Set.of("actors", "most films", "appeared", "prolific"),
                """
                **Query:** `SELECT a.first_name || ' ' || a.last_name AS actor_name, COUNT(fa.film_id) AS film_count FROM actor a JOIN film_actor fa ON a.actor_id = fa.actor_id GROUP BY a.actor_id, a.first_name, a.last_name ORDER BY film_count DESC LIMIT 5;`
                | actor_name | film_count |
                |-----------|-----------|
                | Gina Degeneres | 42 |
                | Walter Torn | 41 |
                | Mary Keitel | 40 |
                | Matthew Carrey | 39 |
                | Sandra Kilmer | 37 |"""),

            entry(Set.of("categories", "most films", "genre", "category"),
                """
                **Query:** `SELECT c.name AS category, COUNT(fc.film_id) AS film_count FROM category c JOIN film_category fc ON c.category_id = fc.category_id GROUP BY c.name ORDER BY film_count DESC;`
                | category | film_count |
                |----------|-----------|
                | Sports | 74 |
                | Foreign | 73 |
                | Family | 69 |
                | Documentary | 68 |
                | Animation | 66 |"""),

            entry(Set.of("customers", "rented", "more than", "30", "frequent"),
                """
                **Query:** `SELECT c.first_name || ' ' || c.last_name AS customer_name, COUNT(r.rental_id) AS rental_count FROM customer c JOIN rental r ON c.customer_id = r.customer_id GROUP BY c.customer_id, c.first_name, c.last_name HAVING COUNT(r.rental_id) > 30 ORDER BY rental_count DESC;`
                | customer_name | rental_count |
                |--------------|-------------|
                | Eleanor Hunt | 46 |
                | Karl Seal | 45 |
                | Marcia Dean | 42 |
                | Clara Shaw | 42 |
                | Tammy Sanders | 41 |"""),

            entry(Set.of("average", "rental duration", "category", "avg"),
                """
                **Query:** `SELECT c.name AS category, ROUND(AVG(f.rental_duration), 2) AS avg_duration_days FROM category c JOIN film_category fc ON c.category_id = fc.category_id JOIN film f ON fc.film_id = f.film_id GROUP BY c.name ORDER BY avg_duration_days DESC;`
                | category | avg_duration_days |
                |----------|------------------|
                | Sports | 5.65 |
                | Games | 5.53 |
                | Foreign | 5.41 |
                | Drama | 5.15 |
                | Comedy | 4.89 |"""),

            entry(Set.of("longer", "3 hours", "180", "long films", "duration"),
                """
                **Query:** `SELECT title, length FROM film WHERE length > 180 ORDER BY length DESC;`
                | title | length |
                |-------|--------|
                | Chicago North | 185 |
                | Control Anthem | 185 |
                | Darn Forrester | 185 |
                | Gangs Pride | 185 |
                | Home Pity | 185 |

                There are **10 films** longer than 3 hours (180 min), all at 185 minutes."""),

            entry(Set.of("staff", "members", "employees", "who are"),
                """
                **Query:** `SELECT first_name || ' ' || last_name AS staff_name, email, store_id FROM staff;`
                | staff_name | email | store_id |
                |-----------|-------|---------|
                | Mike Hillyer | Mike.Hillyer@sakilastaff.com | 1 |
                | Jon Stephens | Jon.Stephens@sakilastaff.com | 2 |"""),

            entry(Set.of("total revenue", "store", "by store", "each store"),
                """
                **Query:** `SELECT s.store_id, ci.city || ', ' || co.country AS location, SUM(p.amount) AS total_revenue FROM store s JOIN address a ON s.address_id = a.address_id JOIN city ci ON a.city_id = ci.city_id JOIN country co ON ci.country_id = co.country_id JOIN staff st ON s.store_id = st.store_id JOIN payment p ON st.staff_id = p.staff_id GROUP BY s.store_id, ci.city, co.country ORDER BY total_revenue DESC;`
                | store_id | location | total_revenue |
                |---------|----------|--------------|
                | 2 | Woodridge, Australia | $33,726.77 |
                | 1 | Lethbridge, Canada | $33,689.74 |"""),

            entry(Set.of("rental history", "mary smith", "customer history"),
                """
                **Query:** `SELECT f.title, r.rental_date, r.return_date FROM customer c JOIN rental r ON c.customer_id = r.customer_id JOIN inventory i ON r.inventory_id = i.inventory_id JOIN film f ON i.film_id = f.film_id WHERE c.first_name = 'Mary' AND c.last_name = 'Smith' ORDER BY r.rental_date DESC LIMIT 10;`
                | title | rental_date | return_date |
                |-------|------------|------------|
                | Swarm Gold | 2005-08-22 20:44:35 | 2005-08-28 17:36:35 |
                | Dogma Family | 2005-08-22 19:08:49 | 2005-08-24 00:47:49 |
                | Masked Bubble | 2005-08-21 23:07:36 | 2005-08-23 20:10:36 |
                | Necklace Outbreak | 2005-08-19 10:16:04 | 2005-08-25 14:36:04 |
                | Whale Bikini | 2005-08-18 22:56:24 | 2005-08-21 18:41:24 |""")
        );
    }

    private static DemoEntry entry(Set<String> keywords, String response) {
        return new DemoEntry(keywords, response);
    }

    record DemoEntry(Set<String> keywords, String response) {
        int score(String query) {
            int count = 0;
            for (String keyword : keywords) {
                if (query.contains(keyword)) count++;
            }
            return count;
        }
    }
}
