package com.reviewlens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.reviewlens.entity.Review;
import com.reviewlens.entity.AnalysisStatus;
import com.reviewlens.dto.ReviewAnalysis;
import com.reviewlens.entity.ReviewAnalysisEntity;
import com.reviewlens.repository.ReviewAnalysisRepository;
import com.reviewlens.repository.ReviewRepository;
import com.reviewlens.service.GeminiService;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:reviewlens_test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ReviewlensApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GeminiService geminiService;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private ReviewAnalysisRepository analysisRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void analysisStatusUsesPortableVarcharMappingWithoutDdlDefaultFragment() throws Exception {
		Column column = Review.class.getDeclaredField("analysisStatus")
				.getAnnotation(Column.class);
		assertEquals("analysis_status", column.name());
		assertEquals(30, column.length());
		assertEquals("", column.columnDefinition());
		assertEquals(AnalysisStatus.PENDING, new Review().getAnalysisStatus());
	}

	@Test
	void failedUploadCanBeRetriedWithoutDuplicatingReviewOrAnalysis() throws Exception {
		when(geminiService.analyzeBatch(anyList()))
				.thenThrow(new IllegalStateException("mock provider failure"))
				.thenReturn(List.of(new ReviewAnalysis("positive", List.of("quality"),
						"Works well.", false)));
		MockMultipartFile csv = retryCsv();

		mockMvc.perform(multipart("/api/reviews/upload").file(csv))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.csvProcessing.reviews[0].analysisStatus")
						.value("analysis_failed"));
		assertEquals(true, reviewRepository.existsById("retry-safe-review"));
		assertEquals(false, analysisRepository
				.findByReview_ReviewId("retry-safe-review").isPresent());

		mockMvc.perform(multipart("/api/reviews/upload").file(retryCsv()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.csvProcessing.reviews[0].analysisStatus")
						.value("completed"));
		assertEquals(true, reviewRepository.existsById("retry-safe-review"));
		assertEquals(true, analysisRepository
				.findByReview_ReviewId("retry-safe-review").isPresent());
		verify(geminiService, org.mockito.Mockito.times(2)).analyzeBatch(anyList());
	}

	private MockMultipartFile retryCsv() {
		return new MockMultipartFile("file", "retry.csv", "text/csv",
				("review_id,product,rating,date,review_text\n"
						+ "retry-safe-review,Retry Device,5,2025-02-03,Works well\n")
						.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	void retryingSameCsvReusesSuccessfulBatchAndOnlyRetriesFailedReviews()
			throws Exception {
		List<Integer> analyzedBatchSizes = new ArrayList<>();
		when(geminiService.analyzeBatch(anyList())).thenAnswer(invocation -> {
			List<Review> batch = invocation.getArgument(0);
			analyzedBatchSizes.add(batch.size());
			if (analyzedBatchSizes.size() == 2) {
				throw new IllegalStateException("mock transient failure");
			}
			return batch.stream().map(ignored -> new ReviewAnalysis(
					"positive", List.of("quality"), "Works well.", false)).toList();
		});

		mockMvc.perform(multipart("/api/reviews/upload").file(sixReviewCsv()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.csvProcessing.reviews[0].analysisStatus")
						.value("completed"))
				.andExpect(jsonPath("$.csvProcessing.reviews[5].analysisStatus")
						.value("analysis_failed"));
		mockMvc.perform(multipart("/api/reviews/upload").file(sixReviewCsv()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.csvProcessing.reviews[0].analysisStatus")
						.value("completed"))
				.andExpect(jsonPath("$.csvProcessing.reviews[5].analysisStatus")
						.value("completed"));

		assertEquals(List.of(5, 1, 1), analyzedBatchSizes);
		List<String> ids = java.util.stream.IntStream.range(0, 6)
				.mapToObj(i -> "retry-batch-review-" + i).toList();
		for (String id : ids) {
			assertEquals(true, reviewRepository.existsById(id));
			assertEquals(true, analysisRepository.findByReview_ReviewId(id).isPresent());
		}
		assertEquals(6, analysisRepository.findByReview_ReviewIdIn(ids).size());
	}

	private MockMultipartFile sixReviewCsv() {
		StringBuilder csv = new StringBuilder(
				"review_id,product,rating,date,review_text\n");
		for (int i = 0; i < 6; i++) {
			csv.append("retry-batch-review-").append(i)
					.append(",Retry Batch Device,5,2025-02-03,Works well number ")
					.append(i).append('\n');
		}
		return new MockMultipartFile("file", "retry-batch.csv", "text/csv",
				csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	@Transactional
	void repositoriesFilterReviewsAndReusePersistedContentHashes() {
		Review negative = new Review("integration-1", "Camera A", 2,
				"Slow delivery", null);
		Review positive = new Review("integration-2", "Camera B", 5,
				"Excellent quality", null);
		reviewRepository.saveAll(List.of(negative, positive));
		ReviewAnalysisEntity analysis = new ReviewAnalysisEntity(negative,
				"negative", List.of("delivery"), "Delivery was slow.", true);
		analysis.setContentHash("a".repeat(64));
		analysisRepository.save(analysis);
		analysisRepository.save(new ReviewAnalysisEntity(positive,
				"positive", List.of("quality"), "Works well.", false));

		var filteredPage = reviewRepository.findFiltered(
				"camera", "negative", "delivery", PageRequest.of(0, 10));
		assertEquals(1, filteredPage.getTotalElements());
		assertEquals(1L, reviewRepository.countFiltered(
				"camera", "negative", "delivery"));
		assertEquals(1, analysisRepository.findFilteredForDashboard(
				"camera", "negative", "delivery").size());
		assertEquals(1, analysisRepository.findByContentHashIn(
				List.of("a".repeat(64))).size());
	}

	@Test
	@Transactional
	void dashboardAndReviewListingSupportEveryOptionalFilterCombination() throws Exception {
		long baselineAll = reviewRepository.countFiltered("", "", "");
		long baselineNegative = reviewRepository.countFiltered("", "negative", "");
		long baselineDelivery = reviewRepository.countFiltered("", "", "delivery");

		Review negative = new Review("filter-regression-negative",
				"ReviewLens Filter Device", 2, "Slow delivery", null);
		negative.setAnalysisStatus(AnalysisStatus.COMPLETED);
		Review positive = new Review("filter-regression-positive",
				"ReviewLens Filter Device", 5, "Excellent quality", null);
		positive.setAnalysisStatus(AnalysisStatus.COMPLETED);
		Review other = new Review("filter-regression-other",
				"ReviewLens Other Product", 3, "Average package", null);
		other.setAnalysisStatus(AnalysisStatus.COMPLETED);
		reviewRepository.saveAll(List.of(negative, positive, other));
		analysisRepository.saveAll(List.of(
				new ReviewAnalysisEntity(negative, "negative", List.of("delivery"),
						"Delivery was slow.", true),
				new ReviewAnalysisEntity(positive, "positive", List.of("quality"),
						"Quality was good.", false),
				new ReviewAnalysisEntity(other, "neutral", List.of("packaging"),
						"Packaging was average.", false)));

		assertFilterCase(Map.of(), baselineAll + 3);
		assertFilterCase(Map.of("product", "REVIEWLENS FILTER DEVICE"), 2);
		assertFilterCase(Map.of("sentiment", "NEGATIVE"), baselineNegative + 1);
		assertFilterCase(Map.of("topic", "DELIVERY"), baselineDelivery + 1);
		assertFilterCase(Map.of("product", "ReviewLens Filter Device",
				"sentiment", "negative", "topic", "delivery"), 1);
	}

	private void assertFilterCase(Map<String, String> filters, long expectedTotal)
			throws Exception {
		var dashboard = get("/api/reviews/dashboard");
		var reviews = get("/api/reviews").param("page", "0").param("size", "100");
		filters.forEach((name, value) -> {
			dashboard.param(name, value);
			reviews.param(name, value);
		});

		mockMvc.perform(dashboard)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReviews").value(expectedTotal));
		mockMvc.perform(reviews)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(expectedTotal));
	}

	@Test
	void httpApisIntegrateUploadFiltersDetailsRepliesAndCorsWithoutGemini() throws Exception {
		when(geminiService.analyzeBatch(anyList())).thenReturn(List.of(
				new ReviewAnalysis("negative", List.of("delivery"),
						"Delivery was late.", true)));
		when(geminiService.generateSuggestedReply(any(Review.class)))
				.thenReturn("Sorry your delivery was late. We appreciate the feedback.");
		MockMultipartFile csv = new MockMultipartFile("file", "reviews.csv",
				"text/csv", ("review_id,product,rating,date,review_text\n"
						+ "http-api-review,Desk Lamp,2,2025-02-03,Delivery was late\n")
						.getBytes(java.nio.charset.StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/reviews/upload").file(csv))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.savedReviews").value(1))
				.andExpect(jsonPath("$.csvProcessing.validReviews").value(1))
				.andExpect(jsonPath("$.csvProcessing.reviews[0].analysisStatus")
						.value("completed"))
				.andExpect(jsonPath("$.analyses[0].needs_reply").value(true));

		mockMvc.perform(get("/api/reviews/dashboard")
				.param("product", "Desk Lamp")
				.param("sentiment", "negative")
				.param("topic", "delivery"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReviews").value(1))
				.andExpect(jsonPath("$.totalAnalyzedReviews").value(1))
				.andExpect(jsonPath("$.reviewsNeedingReply").value(1));

		mockMvc.perform(get("/api/reviews")
				.param("page", "0").param("size", "10")
				.param("product", "Desk Lamp")
				.param("sentiment", "negative").param("topic", "delivery"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].reviewId").value("http-api-review"))
				.andExpect(jsonPath("$.content[0].analysis.topics[0]").value("delivery"));

		mockMvc.perform(get("/api/reviews/http-api-review"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.analysisStatus").value("completed"))
				.andExpect(jsonPath("$.analysis.needs_reply").value(true));

		mockMvc.perform(post("/api/reviews/http-api-review/suggested-reply"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewId").value("http-api-review"))
				.andExpect(jsonPath("$.suggestedReply").value(
						"Sorry your delivery was late. We appreciate the feedback."));

		mockMvc.perform(options("/api/reviews/dashboard")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin",
						"http://localhost:5173"));

		verify(geminiService).analyzeBatch(anyList());
		verify(geminiService).generateSuggestedReply(any(Review.class));
	}

}
