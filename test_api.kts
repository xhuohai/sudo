import java.net.URL
import java.net.HttpURLConnection
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.FileInputStream

// Read the preferences_pb file manually or just grep the cookie out of it.
// The easiest way is to use strings/grep on the DataStore file
