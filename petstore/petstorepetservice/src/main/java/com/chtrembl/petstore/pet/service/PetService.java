package com.chtrembl.petstore.pet.service;

import com.chtrembl.petstore.pet.model.Pet;
import com.chtrembl.petstore.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    public List<Pet> findPetsByStatus(List<String> statusStrings) {
        log.info("Finding pets with status: {}", statusStrings);

        // Convert string status values to Status enum (handles both "available" and "AVAILABLE")
        List<Pet.Status> statuses = statusStrings.stream()
                .map(this::parseStatus)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (statuses.isEmpty()) {
            log.warn("No valid status values found in: {}", statusStrings);
            return List.of();
        }

        return petRepository.findByStatusIn(statuses);
    }

    public Optional<Pet> findPetById(Long petId) {
        log.info("Finding pet with id: {}", petId);
        return petRepository.findById(petId);
    }

    public List<Pet> getAllPets() {
        log.info("Getting all pets");
        return petRepository.findAll();
    }

    public int getPetCount() {
        return (int) petRepository.count();
    }

    /**
     * Parse status string to Status enum.
     * Supports both lowercase ("available") and uppercase ("AVAILABLE") formats.
     */
    private Optional<Pet.Status> parseStatus(String statusString) {
        if (statusString == null || statusString.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Pet.Status.valueOfIgnoreCase(statusString));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: '{}'. Expected: available, pending, sold (case-insensitive)", statusString);
            return Optional.empty();
        }
    }
}