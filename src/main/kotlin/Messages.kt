interface Messages {
    companion object {
        const val START =
            "Привет, этот бот был сделан для помощи в запоминании английских слов. Для того, чтобы ознакомиться с его " +
                    "функционалом, напишите /help"
        val HELP =
            """
                /start - запустить бота
                /help - вызвать список команд 
                /set_time - установить напоминание о времени начала занятий
                /make_table - создать гугл табличку со словарем
                /add_words  - добавить слова в следующих сообщениях в словарь
                /end_adding_words - прекратить добавлять слова
                /repeat - включить режим повторения
                /end_repeat - выключить режим повторения
            """.trimIndent()
        const val INVALID_TEXT = "Была введена неверная команда. Если хотите ознакомиться со списком команд - напишите /help"
        const val INVALID_EMAIL = "Был введен неверный адрес. Пожалуйста, введите его снова"
        const val TABLE_WAS_NOT_MADE = "Таблица ещё не была создана. Если вы хотите создать вашу персональную таблицу - введите " +
                "/make_table"
        const val ADD_WORD_MOD = "Добавляйте слова, пока не лопнете. Если хотите закончить - напишите /end_adding_word"
        const val INVALID_WORD = "Было введено неверное слово. Если вы хотите посмотреть образец добавления - напишите /add_word"
        const val MAKE_TABLE = "Пожалуйста, введите электронный адрес вашей рабочей почты. Она нужна для создания таблички"
        const val SET_TIME = "Пожалуйта, введите время начала ваших занятий в виде hh:mm."
        const val INVALID_TIME = "Введено некоректное время. Попробуйте ввести его еще раз в формате hh:mm в допустимом диапазоне."
        const val REMINDER = "Время начала занятий!"
        const val END_REPEAT = "Режим повторения был выключен."
        const val CORRECT_TRANSLATE = "Верно!"
        fun IncorrectTranslate(word: String) = "Видимо, вы ошиблись. Правильный перевод данного слова - $word."
        fun ValidEmail(tableId: String) = "Вот твоя табличка: https://docs.google.com/spreadsheets/d/$tableId/edit#gid=0"
        fun TableExist(tableId: String) = "Ваша табличка уже была создана ранее. Вот" +
                " ссылка на неё: https://docs.google.com/spreadsheets/d/$tableId/edit#gid=0"
        fun NewWord(word: String, transcription: String, translate: String) =
            """
                Вы успешно добавили следующее слово: 
                Слово - $word
                Транскрипция - $transcription
                Перевод - $translate
            """.trimIndent()
        fun Translate(word: String) = "Как переводится слово $word?"
    }
}