package com.movtery.zalithlauncher.gdworld

import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCancellationTest {
    @Test fun taskStaysBusyUntilCancellationCleanupFinishes() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cleanup = CompletableDeferred<Unit>()
        val task = Task.runTask(task = {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { cleanup.await() }
            }
        })
        try {
            TaskSystem.submitTask(task)
            withTimeout(5000) { started.await() }
            TaskSystem.cancelTask(task)
            assertTrue(TaskSystem.containsTask(task))
        } finally {
            cleanup.complete(Unit)
            TaskSystem.cancelTask(task)
            withTimeout(5000) { TaskSystem.tasksFlow.first { task !in it } }
        }
    }
}
