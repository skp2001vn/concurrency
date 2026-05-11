package org.example.profileaggregator;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Business logic: assembles a user-facing profile snapshot from independent
 * profile, recent-order, and recommendation services.
 *
 * <p>Technique: fans out blocking service calls with
 * {@link CompletableFuture#supplyAsync(java.util.function.Supplier, Executor)}
 * because the lookups are independent but their results must be combined.
 * {@link CompletableFuture#allOf} provides a readable fan-in point and keeps the
 * API asynchronous for callers.
 */
public class ProfileAggregator {

    private final UserService userService;
    private final OrderService orderService;
    private final RecommendationService recommendationService;
    private final Executor executor;

    /**
     * Creates an aggregator that runs independent profile lookups on the supplied executor.
     *
     * @param userService source for the base user profile
     * @param orderService source for recent orders
     * @param recommendationService source for recommendations
     * @param executor executor used to run the three lookups
     */
    public ProfileAggregator(
            UserService userService,
            OrderService orderService,
            RecommendationService recommendationService,
            Executor executor) {
        this.userService = Objects.requireNonNull(userService, "userService");
        this.orderService = Objects.requireNonNull(orderService, "orderService");
        this.recommendationService =
                Objects.requireNonNull(recommendationService, "recommendationService");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Starts the profile, order, and recommendation lookups and returns a future for the combined
     * snapshot.
     *
     * @param userId the user whose profile should be assembled
     * @return a future that completes with the aggregated profile snapshot
     */
    public CompletableFuture<ProfileSnapshot> aggregate(String userId) {
        Objects.requireNonNull(userId, "userId");

        CompletableFuture<UserProfile> userFuture =
                CompletableFuture.supplyAsync(() -> userService.fetchProfile(userId), executor);
        CompletableFuture<List<Order>> orderFuture =
                CompletableFuture.supplyAsync(
                        () -> orderService.fetchRecentOrders(userId), executor);
        CompletableFuture<List<Recommendation>> recommendationFuture =
                CompletableFuture.supplyAsync(
                        () -> recommendationService.fetchRecommendations(userId), executor);

        return CompletableFuture.allOf(userFuture, orderFuture, recommendationFuture)
                .thenApply(ignored -> new ProfileSnapshot(
                        userFuture.join(),
                        orderFuture.join(),
                        recommendationFuture.join()));
    }

    /**
     * Synchronously fetches the core user profile for a user.
     */
    @FunctionalInterface
    public interface UserService {

        /**
         * Returns the basic profile data for a user.
         *
         * @param userId the requested user
         * @return the user's base profile data
         */
        UserProfile fetchProfile(String userId);
    }

    /**
     * Synchronously fetches recent orders for a user.
     */
    @FunctionalInterface
    public interface OrderService {

        /**
         * Returns the recent orders for a user.
         *
         * @param userId the requested user
         * @return recent order summaries
         */
        List<Order> fetchRecentOrders(String userId);
    }

    /**
     * Synchronously fetches recommendations for a user.
     */
    @FunctionalInterface
    public interface RecommendationService {

        /**
         * Returns the recommendation list for a user.
         *
         * @param userId the requested user
         * @return recommended items or offers
         */
        List<Recommendation> fetchRecommendations(String userId);
    }

    /**
     * Immutable profile page data assembled from the three independent sources.
     *
     * @param user the base user profile
     * @param recentOrders recent orders for the user
     * @param recommendations recommended items for the user
     */
    public record ProfileSnapshot(
            UserProfile user,
            List<Order> recentOrders,
            List<Recommendation> recommendations) {

        /**
         * Creates an immutable profile snapshot.
         *
         * @param user the base user profile
         * @param recentOrders recent orders for the user
         * @param recommendations recommended items for the user
         */
        public ProfileSnapshot {
            user = Objects.requireNonNull(user, "user");
            recentOrders = List.copyOf(Objects.requireNonNull(recentOrders, "recentOrders"));
            recommendations =
                    List.copyOf(Objects.requireNonNull(recommendations, "recommendations"));
        }
    }

    /**
     * Basic user information shown in the profile.
     *
     * @param userId the user identifier
     * @param displayName the user-facing name
     * @param email the primary contact email
     */
    public record UserProfile(String userId, String displayName, String email) {

        /**
         * Creates basic user profile data.
         *
         * @param userId the user identifier
         * @param displayName the user-facing name
         * @param email the primary contact email
         */
        public UserProfile {
            userId = Objects.requireNonNull(userId, "userId");
            displayName = Objects.requireNonNull(displayName, "displayName");
            email = Objects.requireNonNull(email, "email");
        }
    }

    /**
     * Lightweight order data shown in the aggregated profile view.
     *
     * @param orderId the order identifier
     * @param description a short order summary
     */
    public record Order(String orderId, String description) {

        /**
         * Creates an order summary.
         *
         * @param orderId the order identifier
         * @param description a short order summary
         */
        public Order {
            orderId = Objects.requireNonNull(orderId, "orderId");
            description = Objects.requireNonNull(description, "description");
        }
    }

    /**
     * Recommendation entry shown alongside the user profile.
     *
     * @param itemId the recommended item identifier
     * @param reason a short explanation for the recommendation
     */
    public record Recommendation(String itemId, String reason) {

        /**
         * Creates a recommendation entry.
         *
         * @param itemId the recommended item identifier
         * @param reason a short explanation for the recommendation
         */
        public Recommendation {
            itemId = Objects.requireNonNull(itemId, "itemId");
            reason = Objects.requireNonNull(reason, "reason");
        }
    }
}
