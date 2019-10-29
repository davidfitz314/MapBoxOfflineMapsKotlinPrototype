package com.example.mapboxofflinemapskotlinprototype

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.util.Log
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import org.json.JSONObject
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class MainActivity : AppCompatActivity() {
    val TAG = "MAPBOXERROR"
    private var isEndNotified: Boolean = false
    private var progressBar: ProgressBar? = null
    private var mapView: MapView? = null
    var offlineManager: OfflineManager? = null

    // JSON encoding/decoding
    val JSON_CHARSET = "UTF-8"
    val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(
            this,
            "pk.eyJ1IjoiZGF2aWRmaXR6MzE0IiwiYSI6ImNqeTdvZHlxYjAxa3YzbW9sZnlnZzJzMWsifQ.J1-VZUeqnR5oYH9514frYQ"
        )
        setContentView(R.layout.activity_main)
        offlineManager = OfflineManager.getInstance(this)
        mapView = findViewById(R.id.mapView);
        mapView?.onCreate(savedInstanceState);
        mapView?.getMapAsync { map ->
            map.setStyle(Style.OUTDOORS){ style ->
                // Create a bounding box for the offline region
                val latLngBounds = LatLngBounds.Builder()
                    .include(LatLng(37.787287, -112.340052)) // Northeast
                    .include(LatLng(37.002615, -114.050181)) // Southwest
                    .build()

                val definition = OfflineTilePyramidRegionDefinition(
                style.uri,
                latLngBounds,
        10.0,
        14.0,this@MainActivity.getResources().getDisplayMetrics().density)

                var metadata: ByteArray?
                try {
                    val jsonObject = JSONObject()
                    jsonObject.put(JSON_FIELD_REGION_NAME, "Zion National Park")
                    val json = jsonObject.toString()
                    metadata = json.toByteArray(charset(JSON_CHARSET))
                } catch (exception: Exception) {
                    Log.d("MAPBOXERROR", "Failed to encode metadata: " + exception.message)
                    metadata = null
                }

                metadata?.let {
                    offlineManager?.createOfflineRegion(definition, metadata,
                        object : OfflineManager.CreateOfflineRegionCallback {
                            override fun onCreate(offlineRegion: OfflineRegion) {
                                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)

                                progressBar = findViewById(R.id.progress_bar);
                                startProgress();

                                // Monitor the download progress using setObserver
                                offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                                    override fun onStatusChanged(status: OfflineRegionStatus) {

                                        // Calculate the download percentage
                                        val percentage = if (status.requiredResourceCount >= 0)
                                            100.0 * status.completedResourceCount /status.requiredResourceCount else 0.0

                                        if (status.isComplete) {
                                            // Download complete
                                            Log.d(TAG, "Region downloaded successfully.")
                                            endProgress("Download Complete");
                                        } else if (status.isRequiredResourceCountPrecise) {
                                            Log.d(TAG, percentage.toString())
                                            setPercentage(Math.round(percentage).toInt())
                                        }
                                    }

                                    override fun onError(error: OfflineRegionError) {
                                        // If an error occurs, print to logcat
                                        Log.d(TAG, "onError reason: " + error.reason)
                                        Log.d(TAG, "onError message: " + error.message)
                                    }

                                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                                        // Notify if offline region exceeds maximum tile count
                                        Log.d(TAG, "Mapbox tile count limit exceeded: $limit")
                                    }
                                })
                            }

                            override fun onError(error: String) {
                                Log.d(TAG, "Error: $error")
                            }
                        })
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }



    override fun onLowMemory() {
        super.onLowMemory();
        mapView?.onLowMemory();
    }

    override fun onDestroy() {
        super.onDestroy();
        mapView?.onDestroy();
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView?.onSaveInstanceState(outState);
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
        offlineManager?.let {
            it.listOfflineRegions(object: OfflineManager.ListOfflineRegionsCallback{
                override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                    offlineRegions?.let {
                        if (it.size > 0) {
                            it[offlineRegions.size - 1].delete(object: OfflineRegion.OfflineRegionDeleteCallback{
                                override fun onError(error: String?) {
                                    Log.d(TAG, "MAP DELETE ERROR")
                                }

                                override fun onDelete() {
                                    Toast.makeText(
                                        applicationContext,
                                        "region deleted",
                                        Toast.LENGTH_LONG
                                    ).show();
                                }
                            })

                        }
                    }
                }

                override fun onError(error: String?) {
                    Log.d(TAG, "Offline Regions Error")
                }

            })
        }
    }

// Progress bar methods
    private fun startProgress() {

        // Start and show the progress bar
        isEndNotified = false;
        progressBar?.setIndeterminate(true);
        progressBar?.setVisibility(View.VISIBLE);
    }

    private fun setPercentage(percentage: Int) {
        progressBar?.setIndeterminate(false);
        progressBar?.setProgress(percentage);
    }

    private fun endProgress(message: String ) {
// Don't notify more than once
        if (isEndNotified) {
            return;
        }

// Stop and hide the progress bar
        isEndNotified = true;
        progressBar?.setIndeterminate(false);
        progressBar?.setVisibility(View.GONE);

// Show a toast
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}


