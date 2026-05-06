package com.disciplineyou.service;

import com.disciplineyou.dao.GoalDAO;
import com.disciplineyou.model.Goal;

import java.util.List;

/**
 * Service that auto-shifts overdue daily goals on app startup.
 */
public class GoalShiftService {

    private final GoalDAO goalDAO = new GoalDAO();

    /**
     * Shifts all overdue PENDING/SHIFTED daily goals to today.
     * Returns the list of goals that were shifted (for UI notification).
     */
    public List<Goal> shiftOverdueGoals() {
        return goalDAO.autoShiftOverdueGoals();
    }
}
