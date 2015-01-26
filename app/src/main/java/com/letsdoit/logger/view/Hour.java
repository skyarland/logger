package com.letsdoit.logger.view;

import com.letsdoit.logger.data.dao.ActivityInterval;

/**
 * Created by Andrey on 1/17/2015.
 */
public class Hour {
    private ActivityInterval firstHalfHour;
    private ActivityInterval secondHalfHour;

    public Hour(ActivityInterval firstHalfHour, ActivityInterval secondHalfHour) {
        this.firstHalfHour = firstHalfHour;
        this.secondHalfHour = secondHalfHour;
    }

    public ActivityInterval getFirstHalfHour() {
        return firstHalfHour;
    }

    public ActivityInterval getSecondHalfHour() {
        return secondHalfHour;
    }
}
