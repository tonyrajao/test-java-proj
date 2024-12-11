package com.pubsub.repository;

import com.pubsub.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, String> {
    List<Content> findByPublisherId(String publisherId);
    List<Content> findByKeywordsContaining(String keyword);
    void deleteByPublisherId(String publisherId);
}
