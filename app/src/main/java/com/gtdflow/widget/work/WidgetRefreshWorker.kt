package com.gtdflow.widget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Фоновый пересчёт виджетов (WorkManager). Вся работа делегируется WidgetService;
 * при сбое возвращаем retry — следующая попытка пройдёт по политике WorkManager.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            WidgetService.refresh(applicationContext)
            Result.success()
        } catch (_: Throwable) {
            // Throwable, не Exception: сбой JNI приходит Error'ом (UnsatisfiedLinkError),
            // Exception его не поймал бы и воркер упал бы с некорректным статусом.
            // WidgetService уже показал ошибку в виджетах; здесь — только не рухнуть.
            Result.retry()
        }
}
