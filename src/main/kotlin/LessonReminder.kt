import java.time.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class LessonReminder(lesson: Lesson) {
    private val reminder = Executors.newScheduledThreadPool(1)
    private val lesson: Lesson

    init {
        this.lesson = lesson
    }

    fun setTimeForReminder(hour: Int, min: Int, sec: Int, chatId: Long) {
        val taskWrapper = Runnable {
            lesson.start(chatId)
            setTimeForReminder(hour, min, sec, chatId)
        }
        val delay = makeDelay(hour, min, sec)
        reminder.schedule(taskWrapper, delay, TimeUnit.SECONDS)
    }

    private fun makeDelay(hour: Int, min: Int, sec: Int): Long {
        val now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
        var nextRemind = now.withHour(hour).withMinute(min).withSecond(sec)
        if (now > nextRemind) {
            nextRemind = nextRemind.plusDays(1)
        }
        return Duration.between(now, nextRemind).seconds
    }

    fun stopReminder() {
        reminder.shutdown()
    }
}