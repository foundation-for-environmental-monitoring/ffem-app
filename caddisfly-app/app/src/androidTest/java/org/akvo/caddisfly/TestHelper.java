/*
 * Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Caddisfly
 *
 * Akvo Caddisfly is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo Caddisfly is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.caddisfly;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Environment;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.internal.util.Checks;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.akvo.caddisfly.app.CaddisflyApp;
import org.akvo.caddisfly.helper.FileHelper;
import org.akvo.caddisfly.util.FileUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.object.HasToString.hasToString;

class TestHelper {

    private static final HashMap<String, String> stringHashMapEN = new HashMap<>();
    private static final HashMap<String, String> stringHashMapFR = new HashMap<>();
    private static final boolean mTakeScreenshots = false;
    public static HashMap<String, String> currentHashMap;
    public static UiDevice mDevice;
    private static String mCurrentLanguage = "en";
    private static int mCounter;

    private static void addString(String key, String englishText, String frenchText) {
        stringHashMapEN.put(key, englishText);
        stringHashMapFR.put(key, frenchText);
    }

    public static void changeLanguage(String languageCode) {
        mCurrentLanguage = languageCode;

        addString("language", "English", "Français");
        addString("otherLanguage", "Français", "English");
        addString("fluoride", "Fluoride", "Fluorure");
        addString("chlorine", "Free Chlorine", "Chlore libre");
        addString("survey", "Survey", "Enquête");
        addString("electricalConductivity", "Electrical Conductivity", "Conductivité Electrique");
        addString("unnamedDataPoint", "Unnamed data point", "Donnée non nommée");
        addString("createNewDataPoint", "Add Data Point", "CRÉER UN NOUVEAU POINT");
        addString("useExternalSource", "Use External Source", "Utiliser source externe");
        addString("next", "Next", "Suivant");

        if (languageCode.equals("en")) {
            currentHashMap = stringHashMapEN;
        } else {
            currentHashMap = stringHashMapFR;
        }
    }

    public static void takeScreenshot() {
        if (mTakeScreenshots) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                File path = new File(Environment.getExternalStorageDirectory().getPath() +
                        "/Akvo Caddisfly/screenshots/screen-" + mCounter++ + "-" + mCurrentLanguage + ".png");
                mDevice.takeScreenshot(path, 0.5f, 60);
            }
        }
    }

    public static void goToMainScreen() {

        boolean found = false;
        while (!found) {
            try {
                onView(withId(R.id.buttonCalibrate)).check(matches(isDisplayed()));
                found = true;
            } catch (NoMatchingViewException e) {
                Espresso.pressBack();
            }
        }
    }

    public static void clickExternalSourceButton(String buttonText) {
        try {

            mDevice.findObject(new UiSelector().text(currentHashMap.get(buttonText))).click();

            mDevice.waitForWindowUpdate("", 2000);

        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void startApp() {
        // Start from the home screen
        mDevice.pressHome();
        mDevice.waitForWindowUpdate("", 2000);
        UiObject2 allAppsButton = mDevice.findObject(By.desc("Apps"));
        allAppsButton.click();
        mDevice.waitForWindowUpdate("", 2000);

        UiScrollable appViews = new UiScrollable(new UiSelector().scrollable(true));
        appViews.setAsHorizontalList();

        UiObject settingsApp = null;
        try {
            String appName = "Akvo Caddisfly";
            settingsApp = appViews.getChildByText(new UiSelector().className(TextView.class.getName()), appName);
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }
        try {
            if (settingsApp != null) {
                settingsApp.clickAndWaitForNewWindow();
            }
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        mDevice.waitForWindowUpdate("", 2000);

        assertTrue("Unable to detect app", settingsApp != null);
    }

    public static void saveCalibration() {

        File path = FileHelper.getFilesDir(FileHelper.FileType.CALIBRATION,
                CaddisflyApp.getApp().getCurrentTestInfo().getCode());

        FileUtil.saveToFile(path,
                "TestValid", "0.0=255  38  186\n"
                        + "0.5=255  51  129\n"
                        + "1.0=255  59  89\n"
                        + "1.5=255  62  55\n"
                        + "2.0=255  81  34\n");
    }

    public static void saveInvalidCalibration() {
        File path = FileHelper.getFilesDir(FileHelper.FileType.CALIBRATION,
                CaddisflyApp.getApp().getCurrentTestInfo().getCode());

        FileUtil.saveToFile(path,
                "TestInvalid", "0.0=255  88  177\n"
                        + "0.5=255  110  15\n"
                        + "1.0=255  138  137\n"
                        + "1.5=253  174  74\n"
                        + "2.0=253  174  76\n"
                        + "2.5=236  172  81\n"
                        + "3.0=254  169  61\n");
    }

    public static void saveOutOfSequence() {
        File path = FileHelper.getFilesDir(FileHelper.FileType.CALIBRATION,
                CaddisflyApp.getApp().getCurrentTestInfo().getCode());

        FileUtil.saveToFile(path,
                "OutOfSequence", "0.0=255  38  186\n"
                        + "0.5=255  51  129\n"
                        + "1.0=255  62  55\n"
                        + "1.5=255  59  89\n"
                        + "2.0=255  81  34\n");
    }

    public static void saveHighLevelCalibration() {
        File path = FileHelper.getFilesDir(FileHelper.FileType.CALIBRATION,
                CaddisflyApp.getApp().getCurrentTestInfo().getCode());

        FileUtil.saveToFile(path,
                "HighLevelTest", "0.0=255  88  47\n"
                        + "0.5=255  60  37\n"
                        + "1.0=255  35  27\n"
                        + "1.5=253  17  17\n"
                        + "2.0=254  0  0\n");
    }

    public static void saveLowLevelCalibration() {
        File path = FileHelper.getFilesDir(FileHelper.FileType.CALIBRATION,
                CaddisflyApp.getApp().getCurrentTestInfo().getCode());

        FileUtil.saveToFile(path,
                "LowLevelTest", "0.0=255  60  37\n"
                        + "0.5=255  35  27\n"
                        + "1.0=253  17  17\n"
                        + "1.5=254  0  0\n"
                        + "2.0=224  0  0\n");
    }

    public static void gotoSurveyForm() {
        //clickListViewItem("Automated Tests");
        //onView(withText("Automated Tests")).perform(click());
        if (!clickListViewItem(currentHashMap.get("unnamedDataPoint"))) {
            //openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());

            // click on 'Add Note' button
            UiObject addButton = mDevice.findObject(new UiSelector()
                    .descriptionContains(currentHashMap.get("createNewDataPoint")));

            try {
                if (addButton.exists() && addButton.isEnabled()) {
                    addButton.click();
                }
            } catch (UiObjectNotFoundException e) {
                e.printStackTrace();
            }

            // onView(withContentDescription(currentHashMap.get("createNewDataPoint"))).perform(click());
            //clickListViewItem(currentHashMap.get("createNewDataPoint"));
        }
        //clickListViewItem("All Tests");
    }

    public static boolean clickListViewItem(String name) {
        UiScrollable listView = new UiScrollable(new UiSelector());
        listView.setMaxSearchSwipes(10);
        listView.waitForExists(5000);
        UiObject listViewItem;
        try {
            if (listView.scrollTextIntoView(name)) {
                listViewItem = listView.getChildByText(new UiSelector()
                        .className(android.widget.TextView.class.getName()), "" + name + "");
                listViewItem.click();
            } else {
                return false;
            }
        } catch (UiObjectNotFoundException e) {
            return false;
        }

        System.out.println("\"" + name + "\" ListView item was clicked.");
        return true;
    }

    public static void enterDiagnosticMode() {
        for (int i = 0; i < 10; i++) {
            onView(withId(R.id.textVersion)).perform(click());
        }
    }

    public static void leaveDiagnosticMode() {
        goToMainScreen();
        onView(withId(R.id.fabDisableDiagnostics)).perform(click());
    }

    private static Matcher<View> withBackgroundColor(final int color) {
        Checks.checkNotNull(color);
        return new BoundedMatcher<View, Button>(Button.class) {
            @Override
            public boolean matchesSafely(Button button) {
                int buttonColor = ((ColorDrawable) button.getBackground()).getColor();
                return Color.red(color) == Color.red(buttonColor) &&
                        Color.green(color) == Color.green(buttonColor) &&
                        Color.blue(color) == Color.blue(buttonColor);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with background color: " + color);
            }
        };
    }

    private static Matcher<String> isEmpty() {
        return new TypeSafeMatcher<String>() {
            @Override
            public boolean matchesSafely(String target) {
                return target.length() == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is empty");
            }
        };
    }

    public static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void resetLanguage() {
        onView(withId(R.id.actionSettings))
                .perform(click());

        onView(withText(R.string.language))
                .perform(click());

        onData(hasToString(startsWith(currentHashMap.get("language")))).perform(click());
    }

    public static Activity getActivityInstance() {
        final Activity[] activity = new Activity[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                if (resumedActivities.iterator().hasNext()) {
                    activity[0] = (Activity) resumedActivities.iterator().next();
                }
            }
        });
        return activity[0];
    }
}