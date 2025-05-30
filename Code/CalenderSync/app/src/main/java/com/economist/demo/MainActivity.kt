package com.economist.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.economist.demo.ui.theme.DemoTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CalendarEventScreen()
            }
        }
    }
}

@Composable
fun CalendarEventScreen() {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current

    // Track if Calendar Intent was launched
    val isReturningFromCalendar = rememberSaveable { mutableStateOf(false) }
    var launchedCalendar by rememberSaveable { mutableStateOf(false) }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Only show toast if returning after launching calendar
                if (launchedCalendar) {
                    isReturningFromCalendar.value = true
                    launchedCalendar = false // Reset after return
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show Toast after coming back
    LaunchedEffect(isReturningFromCalendar.value) {
        if (isReturningFromCalendar.value) {
            Toast.makeText(
                context,
                "Welcome back! If you saved the event, it should now appear in your calendar.",
                Toast.LENGTH_LONG
            ).show()
            isReturningFromCalendar.value = false // Reset after showing toast
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                launchedCalendar = true
                launchCalendarIntent(context)
            }
        ) {
            Text("Add Event to Calendar")
        }
    }
}



fun launchCalendarIntent(context: Context) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI

        putExtra(CalendarContract.Events.TITLE, "Sample Event Title")
        putExtra(CalendarContract.Events.DESCRIPTION, "This is a description of the event.")
        putExtra(CalendarContract.Events.EVENT_LOCATION, "Event Location")

        // Example: Event from 2 days from now, at 10 AM - 11 AM
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

        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.timeInMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No calendar app found!", Toast.LENGTH_SHORT).show()
    }
}

