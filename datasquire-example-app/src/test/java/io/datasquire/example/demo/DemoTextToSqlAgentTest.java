package io.datasquire.example.demo;

import io.datasquire.core.agent.SubAgentRequest;
import io.datasquire.core.agent.SubAgentResponse;
import io.datasquire.core.agent.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class DemoTextToSqlAgentTest {

    private DemoTextToSqlAgent agent;

    @BeforeEach
    void setUp() {
        agent = new DemoTextToSqlAgent();
    }

    @Test
    void getName_returnsDemoName() {
        assertThat(agent.getName()).isEqualTo("demo-text-to-sql");
    }

    @Test
    void getDescription_isNotBlank() {
        assertThat(agent.getDescription()).isNotBlank();
    }

    @Test
    void getCapabilities_isNotEmpty() {
        assertThat(agent.getCapabilities()).isNotEmpty();
    }

    @Test
    void getKeywords_containsExpectedTerms() {
        assertThat(agent.getKeywords()).contains("film", "actor", "rental", "revenue");
    }

    @Test
    void getExampleQueries_isNotEmpty() {
        assertThat(agent.getExampleQueries()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void isHealthy_returnsTrue() {
        assertThat(agent.isHealthy()).isTrue();
    }

    // --- Exact match scenarios ---

    @Test
    void process_filmCount_returnsSuccessWithTable() throws Exception {
        SubAgentResponse response = processQuery("How many films are in the database?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("1,000 films");
        assertThat(response.response()).contains("SELECT COUNT(*)");
        assertThat(response.artifacts()).contains("sql");
    }

    @Test
    void process_topGrossingFilms_returnsRankedTable() throws Exception {
        SubAgentResponse response = processQuery("Show me the top 5 highest-grossing films");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("Telegraph Voyage");
        assertThat(response.response()).contains("$231.73");
        assertThat(response.response()).contains("total_revenue");
    }

    @Test
    void process_actorsMostFilms_returnsJoinResult() throws Exception {
        SubAgentResponse response = processQuery("Which actors have appeared in the most films?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("Gina Degeneres");
        assertThat(response.response()).contains("42");
    }

    @Test
    void process_categoriesCount_returnsGroupedData() throws Exception {
        SubAgentResponse response = processQuery("What categories have the most films?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("Sports");
        assertThat(response.response()).contains("74");
    }

    @Test
    void process_customersMoreThan30_returnsHavingClause() throws Exception {
        SubAgentResponse response = processQuery("Show me customers who rented more than 30 films");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("Eleanor Hunt");
        assertThat(response.response()).contains("HAVING");
    }

    @Test
    void process_avgRentalDuration_returnsDecimalValues() throws Exception {
        SubAgentResponse response = processQuery("What is the average rental duration by category?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("5.65");
        assertThat(response.response()).contains("AVG");
    }

    @Test
    void process_filmsLongerThan3Hours_returnsFilteredResults() throws Exception {
        SubAgentResponse response = processQuery("Find films that are longer than 3 hours");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("185");
        assertThat(response.response()).contains("WHERE length > 180");
    }

    @Test
    void process_staffMembers_returnsSimpleSelect() throws Exception {
        SubAgentResponse response = processQuery("Who are the staff members?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("Mike Hillyer");
        assertThat(response.response()).contains("Jon Stephens");
    }

    @Test
    void process_revenueByStore_returnsMultiJoin() throws Exception {
        SubAgentResponse response = processQuery("What's the total revenue by store?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("$33,726.77");
        assertThat(response.response()).contains("Woodridge, Australia");
    }

    @Test
    void process_marySmithRentals_returnsCustomerHistory() throws Exception {
        SubAgentResponse response = processQuery("Show me the rental history for customer Mary Smith");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("Swarm Gold");
        assertThat(response.response()).contains("Mary Smith");
    }

    // --- Fuzzy matching ---

    @Test
    void process_fuzzyMatch_filmCountVariant() throws Exception {
        SubAgentResponse response = processQuery("total number of films in the database");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("1,000 films");
    }

    @Test
    void process_fuzzyMatch_revenueVariant() throws Exception {
        SubAgentResponse response = processQuery("highest earning films at the top");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("total_revenue");
    }

    // --- Out-of-scope / rejection ---

    @Test
    void process_outOfScope_returnsFailure() throws Exception {
        SubAgentResponse response = processQuery("What is the weather today?");

        assertThat(response.success()).isFalse();
        assertThat(response.errorMessage()).contains("DVD rental database");
    }

    @Test
    void process_completelyUnrelated_returnsFailure() throws Exception {
        SubAgentResponse response = processQuery("Tell me a joke");

        assertThat(response.success()).isFalse();
        assertThat(response.errorMessage()).contains("rephrase");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "x"})
    void process_tooVagueOrEmpty_returnsFailure(String query) throws Exception {
        SubAgentResponse response = processQuery(query);

        assertThat(response.success()).isFalse();
    }

    // --- Case insensitivity ---

    @Test
    void process_caseInsensitive_matchesRegardless() throws Exception {
        SubAgentResponse response = processQuery("HOW MANY FILMS ARE IN THE DATABASE?");

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("1,000 films");
    }

    // --- Async contract ---

    @Test
    void process_returnsCompletedFuture() {
        SubAgentRequest request = new SubAgentRequest(
                "How many films?", List.of(),
                new RequestContext("default", "test", Map.of()), Map.of());

        var future = agent.process(request);
        assertThat(future).isCompleted();
    }

    // --- Internal scoring ---

    @Test
    void findResponse_higherScoreWins() {
        // "top highest grossing films" should match revenue entry (4 keywords)
        // not the category entry (1 keyword: "films")
        SubAgentResponse response = agent.findResponse("top highest grossing films");
        assertThat(response.response()).contains("Telegraph Voyage");
    }

    @Test
    void findResponse_minimumThreshold_rejectsLowScore() {
        // Single keyword "the" shouldn't trigger a match since none of the
        // entry keyword sets contain just "the"
        SubAgentResponse response = agent.findResponse("hello world");
        assertThat(response.success()).isFalse();
    }

    // --- Helper ---

    private SubAgentResponse processQuery(String query) throws ExecutionException, InterruptedException {
        SubAgentRequest request = new SubAgentRequest(
                query, List.of(),
                new RequestContext("default", "test", Map.of()), Map.of());
        return agent.process(request).get();
    }
}
