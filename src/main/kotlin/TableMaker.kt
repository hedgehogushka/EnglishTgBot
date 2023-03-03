import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.AppendValuesResponse
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.model.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import com.google.api.services.drive.model.File as DriveFile


object TableMaker {
    private const val APPLICATION_NAME = "English Data"
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */

    private val SCOPES = SheetsScopes.all()
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"

    private val TABLE_ID = ProjectProperties.tableProperties.getProperty("TABLE_ID")
    private val DATAUSER_ID = ProjectProperties.datauserProperties.getProperty("DATAUSER_ID")
    private val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
    private val service = Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME)
        .build()
    private val driveService: Drive = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
        .setApplicationName(APPLICATION_NAME).build()

    @Throws(IOException::class)
    private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
        // Load client secrets.
        val `in` =  TableMaker::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
        val clientSecrets: GoogleClientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        // Build flow and trigger user authorization request.
        val flow: GoogleAuthorizationCodeFlow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    fun givePermission(id: String, email: String) {
        val newPermission = Permission()
        newPermission.role = "writer"
        newPermission.type = "user"
        newPermission.emailAddress = email
        driveService.permissions().create(id, newPermission).execute()
    }
    fun createTableForUser(): String {
        val newTableId = driveService.files().copy(TABLE_ID, DriveFile()).execute().id
        val requests = mutableListOf<Request>()
        requests.add(
            Request()
                .setUpdateSpreadsheetProperties(
                    UpdateSpreadsheetPropertiesRequest()
                        .setProperties(SpreadsheetProperties().setTitle("English Data")).setFields("title"))
        )
        val body = BatchUpdateSpreadsheetRequest().setRequests(requests)
        service.spreadsheets().batchUpdate(newTableId, body).execute()
        return newTableId
    }
    private const val wordRange = "foruser!A2:C"
    private const val dataIdRange = "data!A2:B"
    private const val dataReminderRange = "data!C2:F"
    private const val insertDataOption = "INSERT_ROWS"
    private const val valueInputOption = "USER_ENTERED"

    private fun add(list: List<String>, range: String, tableId: String) {

        val requestBody = ValueRange()
        requestBody.majorDimension = "ROWS"
        requestBody.range = range
        requestBody.setValues(mutableListOf(list) as List<MutableList<Any>>?)
        val request : Sheets.Spreadsheets.Values.Append =
            service.spreadsheets().values().append(tableId, range, requestBody)
        request.valueInputOption = valueInputOption
        request.insertDataOption = insertDataOption
        val response : AppendValuesResponse = request.execute()
        println(response)
    }

    fun addWord(word: String, transcription: String, translate: String, tableId: String) {
        add(mutableListOf(word, transcription, translate), wordRange, tableId)
    }

    fun addUser(newChatId: String, newTableId: String) {
        add(mutableListOf(newChatId, newTableId), dataIdRange, DATAUSER_ID)
    }

    fun addReminder(chatId: String, hours: String, minutes: String, seconds: String) {
        add(mutableListOf(chatId, hours, minutes, seconds), dataReminderRange, DATAUSER_ID)
    }

    fun copyDataFromTable(map: MutableMap<Long, String>) {

        val request = service.spreadsheets().values().get(DATAUSER_ID, dataIdRange)
        val response = request.execute()
        val values = response.getValues() ?: return
        for (row in values) {
            map[row[0].toString().toLong()] = row[1].toString()
        }
    }

    fun copyDataFromReminder(map: MutableMap<Long, LessonReminder>, usercall: Usercall) {
        val request = service.spreadsheets().values().get(DATAUSER_ID, dataReminderRange)
        val response = request.execute()
        val values = response.getValues() ?: return
        for (row in values) {
            val chatId = row[0].toString().toLong()
            val reminder = LessonReminder(StartOfTheLesson(usercall))
            reminder.setTimeForReminder(
                row[1].toString().toInt(),
                row[2].toString().toInt(),
                row[3].toString().toInt(),
                chatId
            )
            if (map.containsKey(chatId)) {
                map[chatId]!!.stopReminder()
            }
            map[chatId] = reminder
        }
    }
    fun takeRandomWord(tableId: String) : String {
        val request = service.spreadsheets().values().get(tableId, dataReminderRange)
        val response = request.execute()
        val values = response.getValues() ?: return ""
        val randRow = values.random()
        return randRow[0].toString() + " " + randRow[2].toString()
    }

}