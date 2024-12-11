package com.pubsub.service;

import com.pubsub.model.Subscription;
import com.pubsub.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Subscription createSubscription(String subscriberId, String keyword) {
        Optional<Subscription> existingSubscription = subscriptionRepository.findBySubscriberIdAndKeyword(subscriberId, keyword);
        
        if (existingSubscription.isPresent()) {
            return existingSubscription.get();
        }

        List<Subscription> userSubscriptions = subscriptionRepository.findBySubscriberId(subscriberId);
        Subscription subscription;
        
        if (!userSubscriptions.isEmpty()) {
            subscription = userSubscriptions.get(0);
            subscription.addKeyword(keyword);
        } else {
            subscription = new Subscription();
            subscription.setSubscriberId(subscriberId);
            Set<String> keywords = new HashSet<>();
            keywords.add(keyword);
            subscription.setKeywords(keywords);
            subscription.setActive(true);
        }

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public boolean removeSubscription(String subscriberId, String keyword) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findBySubscriberIdAndKeyword(subscriberId, keyword);
        
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.removeKeyword(keyword);
            
            if (subscription.getKeywords().isEmpty()) {
                // If no keywords left, delete the entire subscription
                subscriptionRepository.delete(subscription);
            } else {
                subscriptionRepository.save(subscription);
            }
            return true;
        }
        return false;
    }

    @Transactional
    public void deleteAllSubscriptions(String subscriberId) {
        subscriptionRepository.deleteBySubscriberId(subscriberId);
    }

    public List<Subscription> getSubscriptionsBySubscriberId(String subscriberId) {
        return subscriptionRepository.findBySubscriberId(subscriberId);
    }

    public Set<String> getSubscriberKeywords(String subscriberId) {
        List<Subscription> subscriptions = subscriptionRepository.findBySubscriberId(subscriberId);
        Set<String> keywords = new HashSet<>();
        for (Subscription subscription : subscriptions) {
            keywords.addAll(subscription.getKeywords());
        }
        return keywords;
    }
}
