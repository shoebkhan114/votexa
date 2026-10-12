package com.votexa;

import com.votexa.duplicate.Similarity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SimilarityTest {
    @Test
    void identicalRecordsScoreHigh() {
        LocalDate d = LocalDate.of(1995, 3, 14);
        double s = Similarity.score("Rahul Kumar", d, "House 12 Dummy Street", "Rahul Kumar", d, "House 12 Dummy Street");
        assertTrue(s >= 99.0, "score was " + s);
    }

    @Test
    void differentPeopleScoreLow() {
        double s = Similarity.score("Rahul Kumar", LocalDate.of(1995, 3, 14), "House 12", "Meera Nair", LocalDate.of(1980, 1, 1), "Plot 99");
        assertTrue(s < 60.0, "score was " + s);
    }

    @Test
    void typoStillMatchesWhenDobAndAddressEqual() {
        LocalDate d = LocalDate.of(1990, 5, 5);
        double s = Similarity.score("Rahul Kumar", d, "House 12 Dummy Street", "Rahul Kumarr", d, "House 12 Dummy Street");
        assertTrue(s >= 85.0, "score was " + s);
    }
}
