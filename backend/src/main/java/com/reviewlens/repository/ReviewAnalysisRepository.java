package com.reviewlens.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.reviewlens.entity.ReviewAnalysisEntity;

public interface ReviewAnalysisRepository
        extends JpaRepository<ReviewAnalysisEntity, Long> {

    @EntityGraph(attributePaths = {"review", "topics"})
    List<ReviewAnalysisEntity> findByReview_ReviewIdIn(
            Collection<String> reviewIds
    );

    @EntityGraph(attributePaths = {"review", "topics"})
    List<ReviewAnalysisEntity> findByContentHashIn(Collection<String> hashes);

    @EntityGraph(attributePaths = {"review", "topics"})
    Optional<ReviewAnalysisEntity> findByReview_ReviewId(String reviewId);

    @EntityGraph(attributePaths = {"review", "topics"})
    @Query("""
        select distinct a from ReviewAnalysisEntity a
        where (:product = '' or lower(a.review.productName) like concat('%', :product, '%'))
          and a.review.analysisStatus <> com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED
          and (:sentiment = '' or lower(a.sentiment) = :sentiment)
          and (:topic = '' or exists (
              select t from ReviewAnalysisEntity matching join matching.topics t
              where matching = a and lower(t) = :topic))
        """)
    List<ReviewAnalysisEntity> findFilteredForDashboard(
            @Param("product") String product,
            @Param("sentiment") String sentiment,
            @Param("topic") String topic);

    @Override
    @EntityGraph(attributePaths = {"review", "topics"})
    List<ReviewAnalysisEntity> findAll();
}
