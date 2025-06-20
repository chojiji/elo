package com.example.elo.service;

import com.example.elo.DTO.EloData;
import com.example.elo.entity.CategoryDataRelation;
import com.example.elo.repository.CategoryDataRelationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class EloGetService {
    private CategoryDataRelationRepository categoryDataRelationRepository;
    private static final int K_FACTOR = 32;

    @Autowired
    public void EloGetService(CategoryDataRelationRepository categoryDataRelationRepository){
        this.categoryDataRelationRepository=categoryDataRelationRepository;
    }


    @Retryable(
            value = {OptimisticLockException.class, OptimisticLockingFailureException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 1000)
    )
    @Transactional
    public void calculateEloAndUpdate(String groupName, String categoryName,
                                      String imageName1Param, String imageName2Param,
                                      Collection<Boolean> votes) {

        String firstImageNameInternal;
        String secondImageNameInternal;
        boolean paramsSwapped = false;

        if (imageName1Param.compareTo(imageName2Param) < 0) {
            firstImageNameInternal = imageName1Param;
            secondImageNameInternal = imageName2Param;
        } else if (imageName1Param.compareTo(imageName2Param) > 0) {
            firstImageNameInternal = imageName2Param;
            secondImageNameInternal = imageName1Param;
            paramsSwapped = true;
        } else {
            System.err.println("Error: Image names are identical: " + imageName1Param);
            return;
        }

        try {
            CategoryDataRelation relation1 = categoryDataRelationRepository
                    .findRelationByNames(groupName, categoryName, firstImageNameInternal)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("Relation not found for group: %s, category: %s, image: %s",
                                    groupName, categoryName, firstImageNameInternal)));

            CategoryDataRelation relation2 = categoryDataRelationRepository
                    .findRelationByNames(groupName, categoryName, secondImageNameInternal)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("Relation not found for group: %s, category: %s, image: %s",
                                    groupName, categoryName, secondImageNameInternal)));

            int elo1 = relation1.getElo();
            int elo2 = relation2.getElo();

            for (Boolean voteForParam1Won : votes) {
                boolean firstInternalImageWon;
                if (paramsSwapped) {
                    firstInternalImageWon = !voteForParam1Won;
                } else {
                    firstInternalImageWon = voteForParam1Won;
                }

                double expectedScore1 = calculateExpectedScore(elo1, elo2);
                double expectedScore2 = calculateExpectedScore(elo2, elo1);

                double actualScore1;
                double actualScore2;

                if (firstInternalImageWon) {
                    actualScore1 = 1.0;
                    actualScore2 = 0.0;
                } else {
                    actualScore1 = 0.0;
                    actualScore2 = 1.0;
                }

                elo1 = (int) Math.round(elo1 + K_FACTOR * (actualScore1 - expectedScore1));
                elo2 = (int) Math.round(elo2 + K_FACTOR * (actualScore2 - expectedScore2));
            }

            relation1.setElo(elo1);
            relation2.setElo(elo2);

            categoryDataRelationRepository.save(relation1);
            categoryDataRelationRepository.save(relation2);

        } catch (EntityNotFoundException e) {
            System.err.println("Entity not found during ELO update: " + e.getMessage());
            throw e;
        }
    }
    public  List<Object[]>getResult(String groupName, String categoryName){
        return categoryDataRelationRepository.findRawDataNameAndEloByGroupAndCategory(groupName,categoryName);
    }

    private double calculateExpectedScore(int eloPlayer1, int eloPlayer2) {
        return 1.0 / (1.0 + Math.pow(10.0, (double) (eloPlayer2 - eloPlayer1) / 400.0));
    }
}
