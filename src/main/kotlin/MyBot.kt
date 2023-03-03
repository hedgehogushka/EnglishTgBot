import com.google.api.client.googleapis.json.GoogleJsonResponseException
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.lang.StringBuilder


class MyBot : TelegramLongPollingBot(), Usercall {


    private var botRestarted = false
    private val reminderMap = mutableMapOf<Long, LessonReminder>()
    private val translateMap = mutableMapOf<Long, String>()
    private var phase = mutableMapOf<Long, Phases>()
    private var tableMap = mutableMapOf<Long, String>()
    private var VALID_EMAIL = "^[A-Za-z](.*)([@])(.+)(\\.)(.+)"
    private var VALID_WORD = "^[ ]*[A-Za-z]+[ ]*([\\[][A-Za-z]+[\\]])?[ ]*[\\-\\—\\–\\−]?[ ]*[А-Яа-я]+"
    private var VALID_TIME = "[0-2][0-9]([:])[0-6][0-9]"


    override fun callUser(chatId: Long) {
        makeMessage(Messages.REMINDER, chatId)
    }

    private fun makeMessage(text: String, chatId: Long) {
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = text
        execute(sendMessage)
    }

    private fun workWithWord(text: String): List<String> {
        val wordMap = List<StringBuilder>(3) { StringBuilder() }

        var step = 0;
        for(i in text) {
            if (i == ' ') continue;
            if (i == '[') {
                step = 1;
                continue;
            }
            if (i == ']' || i == '-'){
                step = 2;
                continue;
            }
            wordMap[step].append(i)
        }
        if (step == 1) {
            return listOf(wordMap[0].toString().lowercase(), "", wordMap[1].toString().lowercase());
        }
        return listOf(wordMap[0].toString().lowercase(), wordMap[1].toString().lowercase(), wordMap[2].toString().lowercase());

    }
    private fun isValidEmail(text: String) = VALID_EMAIL.toRegex().matches(text)

    private fun isValidWord(text: String) = VALID_WORD.toRegex().matches(text)

    private fun isValidTime(text: String) = VALID_TIME.toRegex().matches(text)

    override fun getBotUsername() = "TGHedgehogushkasBot"

    override fun getBotToken() = ProjectProperties.mainProperties.getProperty("MAIN_ID")

    override fun onUpdateReceived(update: Update) {

        if (!botRestarted){
            botRestarted = true
            TableMaker.copyDataFromTable(tableMap)
            TableMaker.copyDataFromReminder(reminderMap, this)
        }
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getChatId() != null) {
            val chatId = update.getMessage().chatId
            if (!phase.containsKey(chatId)) {
                phase[chatId] = Phases.BASE
            }
            val text = update.getMessage().text
            when (text) {
                "/start" -> {
                    makeMessage(Messages.START, chatId)
                    phase[chatId] = Phases.BASE
                }
                "/help" -> {
                    makeMessage(Messages.HELP, chatId)
                    phase[chatId] = Phases.BASE
                }
                "/make_table" -> {
                    if (tableMap.containsKey(chatId)) {
                        makeMessage(Messages.TableExist(tableMap[chatId]!!), chatId)
                        phase[chatId] = Phases.BASE
                    }
                    else {
                        makeMessage(Messages.MAKE_TABLE, chatId)
                        phase[chatId] = Phases.MAKING_TABLE
                    }
                }
                "/add_words" -> {
                    makeMessage(Messages.ADD_WORD_MOD, chatId)
                    phase[chatId] = Phases.ADDING_WORDS
                }
                "/end_adding_words" -> {
                    makeMessage(Messages.START, chatId)
                    phase[chatId] = Phases.BASE
                }
                "/set_time" -> {
                    makeMessage(Messages.SET_TIME, chatId)
                    phase[chatId] = Phases.SET_TIME
                }
                "/repeat" -> {
                    if (!tableMap.containsKey(chatId)) {
                        makeMessage(Messages.TABLE_WAS_NOT_MADE, chatId)
                    }
                    else {
                        phase[chatId] = Phases.QUESTION
                        val userId = tableMap[chatId].toString()
                        val checkword: String = TableMaker.takeRandomWord(userId)
                        val word = checkword.split(" ")[0]
                        val translate = checkword.split(" ")[1]
                        translateMap[chatId] = translate
                        makeMessage(Messages.Translate(word), chatId)
                    }
                }
                "/end_repeat" -> {
                    makeMessage(Messages.END_REPEAT, chatId)
                    phase[chatId] = Phases.BASE
                }
                else -> {
                    when (phase[chatId]) {
                        Phases.BASE -> makeMessage(Messages.INVALID_TEXT, chatId)
                        Phases.MAKING_TABLE -> {
                            if (!isValidEmail(text)) {
                                makeMessage(Messages.INVALID_EMAIL, chatId)
                            }
                            else {
                                val userId = TableMaker.createTableForUser()
                                TableMaker.addUser(userId, chatId.toString())
                                tableMap[chatId] = userId

                                TableMaker.givePermission(tableMap[chatId]!!, text)
                                makeMessage(Messages.ValidEmail(tableMap[chatId]!!), chatId)
                                phase[chatId] = Phases.BASE

                            }
                        }
                        Phases.ADDING_WORDS -> {
                            if (!tableMap.containsKey(chatId)) {
                                makeMessage(Messages.TABLE_WAS_NOT_MADE, chatId)
                            }
                            else  if (!isValidWord(text)) {
                                makeMessage(Messages.INVALID_WORD, chatId)
                            }
                            else {
                                val newWord = workWithWord(text)
                                makeMessage(Messages.NewWord(newWord[0], newWord[1], newWord[2]), chatId);
                                val userId = tableMap[chatId].toString();
                                TableMaker.addWord(newWord[0], newWord[1], newWord[2], userId);
                            }
                        }
                        Phases.SET_TIME -> {
                            if (!isValidTime(text)) {
                                makeMessage(Messages.INVALID_TIME, chatId)
                            }
                            else {
                                val hours = text.split(":")[0].toInt();
                                if (hours > 23) {
                                    makeMessage(Messages.INVALID_TIME, chatId)
                                } else {
                                    val minutes = text.split(":")[1].toInt()
                                    if (reminderMap.containsKey(chatId)) {
                                        reminderMap[chatId]!!.stopReminder()
                                    }
                                    reminderMap[chatId] = LessonReminder(StartOfTheLesson(this))
                                    reminderMap[chatId]!!.setTimeForReminder(hours, minutes, 0, chatId)
                                    TableMaker.addReminder(chatId.toString(), hours.toString(), minutes.toString(), "0")
                                }
                            }
                        }
                        Phases.QUESTION -> {
                            if (text.lowercase().equals(translateMap[chatId]!!)) {
                                makeMessage(Messages.CORRECT_TRANSLATE, chatId)
                            } else {
                                makeMessage(Messages.IncorrectTranslate(translateMap[chatId]!!), chatId)
                                val userId = tableMap[chatId].toString()
                                val checkword: String = TableMaker.takeRandomWord(userId)
                                val word = checkword.split(" ")[0]
                                val translate = checkword.split(" ")[1]
                                translateMap[chatId] = translate
                                makeMessage(Messages.Translate(word), chatId)
                            }

                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }
}