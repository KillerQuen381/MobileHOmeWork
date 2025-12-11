package com.example.mobilehomework

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskRepository(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "tasks_list"

    fun saveTasks(tasks: List<TaskItem>) {
        val json = gson.toJson(tasks)
        prefs.edit().putString(key, json).apply()
    }

    fun loadTasks(): List<TaskItem> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<TaskItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

