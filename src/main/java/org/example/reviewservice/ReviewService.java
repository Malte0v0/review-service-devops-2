package org.example.reviewservice;

import org.example.reviewservice.Client.BookingClient;
import org.example.reviewservice.Client.CustomerClient;
import org.example.reviewservice.Exceptions.CustomerServiceUnavailableException;
import org.example.reviewservice.Exceptions.InvalidReviewDataException;
import org.example.reviewservice.Exceptions.ReviewNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private final ReviewRepository reviewRepository;
    private final CustomerClient customerClient;
    private final BookingClient bookingClient;

    public ReviewService(ReviewRepository reviewRepository, CustomerClient customerClient, BookingClient bookingClient) {
        this.reviewRepository = reviewRepository;
        this.customerClient = customerClient;
        this.bookingClient = bookingClient;
    }

    private ReviewResponseDTO toDTO(ReviewEntity review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setCustomerId(review.getCustomerId());
        dto.setReviewDate(review.getReviewDate());
        dto.setReviewText(review.getReviewText());
        dto.setRoomId(review.getRoomId());
        return dto;
    }

    private ReviewEntity toEntity(ReviewCreateDTO dto) {
        ReviewEntity review = new ReviewEntity();
        review.setCustomerId(dto.getCustomerId());
        review.setReviewDate(dto.getReviewDate());
        review.setReviewText(dto.getReviewText());
        review.setRoomId(dto.getRoomId());
        return review;
    }

    public List<ReviewResponseDTO> findAll() {
        log.info("Finding all reviews");
        return reviewRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ReviewResponseDTO> findByCustomerId(long customerId) {
        log.info("Finding reviews by customer ID {}", customerId);
        List<ReviewEntity> reviews = reviewRepository.findByCustomerId(customerId)
                .orElseThrow(() -> {log.warn("No reviews found by customer ID {}", customerId);
                        return new ReviewNotFoundException("Could not find reviews from customer " + customerId);
                });

        List<ReviewResponseDTO> responseDTOS = new ArrayList<>();

        for (ReviewEntity review : reviews) {
            responseDTOS.add(toDTO(review));
        }

        return responseDTOS;
    }

    public List<ReviewResponseDTO> findByRoomId(long roomId) {
        List<ReviewEntity> reviews = reviewRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ReviewNotFoundException("Could not find reviews of room " + roomId));

        List<ReviewResponseDTO> responseDTOS = new ArrayList<>();

        for (ReviewEntity review : reviews) {
            responseDTOS.add(toDTO(review));
        }

        return responseDTOS;
    }

    public ReviewResponseDTO create(ReviewCreateDTO dto) {
        try {
            if(!bookingClient.roomExists(dto.getRoomId())) {
                throw new InvalidReviewDataException("The room with id " + dto.getRoomId() + " does not exist");
            } else if (!customerClient.customerExists(dto.getCustomerId())) {
                throw new InvalidReviewDataException("The customer with id " + dto.getCustomerId() + " does not exist");
            }
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new CustomerServiceUnavailableException("The service is unavailable. Try again later.");
        }

        ReviewEntity saved = reviewRepository.save(toEntity(dto));

        return toDTO(saved);
    }

    public ReviewResponseDTO update(long id, ReviewCreateDTO dto) {
        ReviewEntity review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review with id " + id + " was not found"));

        review.setReviewText(dto.getReviewText());
        review.setReviewDate(dto.getReviewDate());
        review.setCustomerId(dto.getCustomerId());
        review.setRoomId(dto.getRoomId());

        ReviewEntity saved = reviewRepository.save(review);

        return toDTO(saved);
    }

    public void delete(long id) {
        reviewRepository.deleteById(id);
    }
}
