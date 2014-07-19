package com.letsdoit.logger;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.google.common.collect.Lists;
import com.letsdoit.logger.data.activity.ActivityFragment;
import com.letsdoit.logger.data.activity.ActivityInterval;
import com.letsdoit.logger.view.DisplayBlock;
import com.letsdoit.logger.view.HourAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.mockito.Matchers;

import java.util.List;
import java.util.regex.Matcher;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

}