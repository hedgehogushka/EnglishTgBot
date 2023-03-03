class StartOfTheLesson (private val usercall: Usercall) : Lesson {
    override fun start(chatId: Long) {
        usercall.callUser(chatId)
    }
}