
package com.reviewlens.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.reviewlens.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    @Query("""
        select r from Review r
        where (:product = '' or lower(r.productName) like concat('%', :product, '%'))
          and (:sentiment = '' or exists (
              select a.id from ReviewAnalysisEntity a
              where a.review = r
                and a.review.analysisStatus <> com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED
                and lower(a.sentiment) = :sentiment))
          and (:topic = '' or exists (
              select a2.id from ReviewAnalysisEntity a2 join a2.topics t
              where a2.review = r
                and a2.review.analysisStatus <> com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED
                and lower(t) = :topic))
        """)
    Page<Review> findFiltered(@Param("product") String product,
                              @Param("sentiment") String sentiment,
                              @Param("topic") String topic,
                              Pageable pageable);

    @Query("""
        select count(r) from Review r
        where (:product = '' or lower(r.productName) like concat('%', :product, '%'))
          and (:sentiment = '' or exists (
              select a.id from ReviewAnalysisEntity a
              where a.review = r
                and a.review.analysisStatus <> com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED
                and lower(a.sentiment) = :sentiment))
          and (:topic = '' or exists (
              select a2.id from ReviewAnalysisEntity a2 join a2.topics t
              where a2.review = r
                and a2.review.analysisStatus <> com.reviewlens.entity.AnalysisStatus.ANALYSIS_FAILED
                and lower(t) = :topic))
        """)
    long countFiltered(@Param("product") String product,
                       @Param("sentiment") String sentiment,
                       @Param("topic") String topic);

}
