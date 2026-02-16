package com.example.mobile_hw4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobile_hw4.ui.theme.Mobile_hw4Theme
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = CounterNotificationService(applicationContext)
        setContent {
            Mobile_hw4Theme() {
                ProximitySensor(service)
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(onClick = {
                        service.showNotification(Counter.value)
                    }) {
                        Text(text = "Show notification")
                    }
                }



                val factory = rememberPermissionsControllerFactory()
                val controller = remember(factory) {
                    factory.createPermissionsController()
                }

                BindEffect(controller)

                val viewModel = viewModel {
                    PermissionsViewModel(controller)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when(viewModel.state) {
                        PermissionState.Granted -> {
                            Text("Record audio permission granted!")
                        }
                        PermissionState.DeniedAlways -> {
                            Text("Permission was permanently declined.")
                            Button(onClick = {
                                controller.openAppSettings()
                            }) {
                                Text("Open app settings")
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    viewModel.provideOrRequestRecordAudioPermission()
                                }
                            ) {
                                Text("Request permission")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProximitySensor(service: CounterNotificationService) {
    val ctx = LocalContext.current
    val sensorManager: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    val sensorStatus = remember {
        mutableStateOf("")
    }
    val proximitySensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                if (event.values[0] == 0f) {
                    sensorStatus.value = "Near"
                    service.showNotification(50)
                } else {
                    sensorStatus.value = "Away"
                }
            }
        }
    }

    sensorManager.registerListener(
        proximitySensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = sensorStatus.value,
            fontSize = 40.sp, modifier = Modifier.padding(5.dp)
        )
    }
}

class PermissionsViewModel(
    private val controller: PermissionsController
): ViewModel() {

    var state by mutableStateOf(PermissionState.NotDetermined)
        private set

    init {
        viewModelScope.launch {
            state = controller.getPermissionState(Permission.RECORD_AUDIO)
        }
    }

    fun provideOrRequestRecordAudioPermission() {
        viewModelScope.launch {
            try {
                controller.providePermission(Permission.RECORD_AUDIO)
                state = PermissionState.Granted
            } catch(e: DeniedAlwaysException) {
                state = PermissionState.DeniedAlways
            } catch(e: DeniedException) {
                state = PermissionState.Denied
            } catch(e: RequestCanceledException) {
                e.printStackTrace()
            }
        }
    }
}