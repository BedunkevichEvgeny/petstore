package com.chtrembl.petstore.pet.repository;

import com.chtrembl.petstore.pet.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * Find pets by status enum values.
     * Use Status enum directly for type-safe queries.
     */
    List<Pet> findByStatusIn(List<Pet.Status> statuses);
}
