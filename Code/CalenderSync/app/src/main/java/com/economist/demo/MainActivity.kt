package com.economist.demo

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CalendarDirectInsertScreen()
            }
        }
    }
}

@Composable
fun CalendarDirectInsertScreen() {
    val context = LocalContext.current
    val activity = context.findActivity()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                requestCalendarPermissions(activity)
            }
        ) {
            Text("Request Calendar Permission")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                listGoogleCalendars(context)
            }
        ) {
            Text("List Google Calendars")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                insertEventToGoogleEventsCalendar(context)
            }
        ) {
            Text("Add Event to Google Events Calendar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                queryInsertedEvents(context)
            }
        ) {
            Text("Query Inserted Events")
        }
    }
}

fun listGoogleCalendars(context: Context) {
    val contentResolver = context.contentResolver

    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE
    )

    val cursor = contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
        arrayOf("com.google"),
        null
    )

    if (cursor != null && cursor.moveToFirst()) {
        do {
            val calId = cursor.getLong(0)
            val displayName = cursor.getString(1)
            val accountName = cursor.getString(2)
            val accountType = cursor.getString(3)

            Log.d("CalendarList", "ID=$calId, Name=$displayName, Account=$accountName, Type=$accountType")
        } while (cursor.moveToNext())

        cursor.close()
    } else {
        Log.d("CalendarList", "No Google calendars found.")
    }
}

fun insertEventToGoogleEventsCalendar(context: Context) {
    val contentResolver = context.contentResolver

    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE
    )

    val cursor = contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
        arrayOf("com.google"),
        null
    )

    var calendarId: Long? = null
    var calendarName: String? = null
    var accountName: String? = null

    if (cursor != null && cursor.moveToFirst()) {
        do {
            val calId = cursor.getLong(0)
            val displayName = cursor.getString(1)
            val accName = cursor.getString(2)

            Log.d("CalendarList", "ID=$calId, Name=$displayName, Account=$accName")

            // ✅ Main Google Calendar → when displayName == accountName
            if (displayName == accName) {
                calendarId = calId
                calendarName = displayName
                accountName = accName
                Log.d("CalendarInsert", "Selected calendarId=$calendarId, Name=$calendarName")
                break
            }

        } while (cursor.moveToNext())

        cursor.close()
    }

    if (calendarId == null || accountName == null) {
        Toast.makeText(context, "Could not find main Google calendar!", Toast.LENGTH_SHORT).show()
        return
    }

    // Prepare event details
    val beginTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 2)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
    }

    val endTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 2)
        set(Calendar.HOUR_OF_DAY, 11)
        set(Calendar.MINUTE, 0)
    }

    val values = ContentValues().apply {
        put(CalendarContract.Events.DTSTART, beginTime.timeInMillis)
        put(CalendarContract.Events.DTEND, endTime.timeInMillis)
        put(CalendarContract.Events.TITLE, "My Google Calendar Event")
        put(CalendarContract.Events.DESCRIPTION, "Event inserted into Google Calendar: $calendarName")
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }

    // Insert event
    val eventUri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

    // 🚀 Trigger local ContentObserver
    context.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)

    // 🚀 Force Calendar sync → this will make Google Calendar app refresh
    try {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")

        // Match the correct Google account used by the calendar
        val matchingAccount = accounts.firstOrNull { it.name == accountName }

        if (matchingAccount != null) {
            val bundle = Bundle().apply {
                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            }

            ContentResolver.requestSync(matchingAccount, "com.android.calendar", bundle)

            Log.d("CalendarSync", "Requested sync for account: ${matchingAccount.name}")
        } else {
            Log.d("CalendarSync", "No matching Google account found for calendar: $accountName")
        }
    } catch (e: Exception) {
        Log.e("CalendarSync", "Failed to trigger sync: ${e.message}")
    }

    // Show result
    if (eventUri != null) {
        Toast.makeText(context, "Event added to Google calendar: $calendarName", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Failed to add event.", Toast.LENGTH_SHORT).show()
    }
}

fun requestCalendarPermissions(activity: Activity) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR
        ),
        100
    )
}

fun queryInsertedEvents(context: Context) {
    val contentResolver = context.contentResolver

    val beginTime = Calendar.getInstance().timeInMillis
    val endTime = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 7)
    }.timeInMillis

    val projection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND
    )

    val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND " +
            "(${CalendarContract.Events.DTEND} <= ?) AND " +
            "(${CalendarContract.Events.TITLE} = ?)"

    val selectionArgs = arrayOf(
        beginTime.toString(),
        endTime.toString(),
        "My Google Events Calendar Event"
    )

    val cursor = contentResolver.query(
        CalendarContract.Events.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        "${CalendarContract.Events.DTSTART} ASC"
    )

    if (cursor != null && cursor.moveToFirst()) {
        do {
            val eventId = cursor.getLong(0)
            val title = cursor.getString(1)
            val startMillis = cursor.getLong(2)
            val endMillis = cursor.getLong(3)

            val startDate = Date(startMillis)
            val endDate = Date(endMillis)

            Log.d("EventQuery", "Event found! ID=$eventId, Title=$title, Start=$startDate, End=$endDate")

        } while (cursor.moveToNext())

        cursor.close()
    } else {
        Log.d("EventQuery", "No matching event found.")
    }
}



fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Context is not an Activity.")
}

