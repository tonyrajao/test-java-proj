package com.pubsub.repository;

import com.pubsub.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    List<Subscription> findBySubscriberId(String subscriberId);
    
    @Query("SELECT s FROM Subscription s JOIN s.keywords k WHERE s.subscriberId = :subscriberId AND k = :keyword")
    Optional<Subscription> findBySubscriberIdAndKeyword(@Param("subscriberId") String subscriberId, @Param("keyword") String keyword);

    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.subscriberId = :subscriberId")
    void deleteBySubscriberId(@Param("subscriberId") String subscriberId);
}
