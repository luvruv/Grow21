package com.example.grow21;

import android.content.Context;

import com.example.grow21.models.QuestionModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionLoader {

    private QuestionLoader() {
        // Utility class, no instances
    }

    /**
     * Reads questions.json from assets and returns all questions.
     */
    public static List<QuestionModel> loadAllQuestions(Context context) {
        String jsonString = readJsonFromAssets(context, "questions.json");
        if (jsonString == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<QuestionModel>>() {}.getType();
        List<QuestionModel> questions = gson.fromJson(jsonString, listType);
        return questions != null ? questions : new ArrayList<>();
    }

    /**
     * Loads questions filtered by a specific category.
     */
    public static List<QuestionModel> loadQuestionsByCategory(Context context, String category) {
        List<QuestionModel> allQuestions = loadAllQuestions(context);
        List<QuestionModel> filtered = new ArrayList<>();
        for (QuestionModel q : allQuestions) {
            if (q.getCategory() != null && q.getCategory().equalsIgnoreCase(category)) {
                filtered.add(q);
            }
        }
        Collections.shuffle(filtered);
        return filtered;
    }

    /**
     * Loads all questions and shuffles them.
     */
    public static List<QuestionModel> loadShuffledQuestions(Context context) {
        List<QuestionModel> questions = loadAllQuestions(context);
        Collections.shuffle(questions);
        return questions;
    }

    /**
     * Loads questions filtered by type (e.g., "tap", "drag_match", "motor").
     */
    public static List<QuestionModel> loadQuestionsByType(Context context, String type) {
        List<QuestionModel> allQuestions = loadAllQuestions(context);
        List<QuestionModel> filtered = new ArrayList<>();
        for (QuestionModel q : allQuestions) {
            if (q.getType() != null && q.getType().equalsIgnoreCase(type)) {
                filtered.add(q);
            }
        }
        Collections.shuffle(filtered);
        return filtered;
    }

    /**
     * Loads questions filtered by both type and category.
     */
    public static List<QuestionModel> loadQuestionsByTypeAndCategory(Context context, String type, String category) {
        List<QuestionModel> allQuestions = loadAllQuestions(context);
        List<QuestionModel> filtered = new ArrayList<>();
        for (QuestionModel q : allQuestions) {
            if (q.getType() != null && q.getType().equalsIgnoreCase(type)
                && q.getCategory() != null && q.getCategory().equalsIgnoreCase(category)) {
                filtered.add(q);
            }
        }
        Collections.shuffle(filtered);
        return filtered;
    }

    /**
     * Reads a JSON file from the assets folder.
     */
    private static String readJsonFromAssets(Context context, String fileName) {
        String json = null;
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }
}
